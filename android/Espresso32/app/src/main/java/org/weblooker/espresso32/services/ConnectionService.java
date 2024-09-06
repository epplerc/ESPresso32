/*
 * Copyright 2021 Christian Eppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.weblooker.espresso32.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.activities.EnableDependenciesActivity;
import org.weblooker.espresso32.activities.MainActivity;
import org.weblooker.espresso32.models.BleJob;
import org.weblooker.espresso32.models.BleStatus;
import org.weblooker.espresso32.models.ScaleModus;
import org.weblooker.espresso32.utils.BleCommands;
import org.weblooker.espresso32.utils.PreferencesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.core.app.NotificationCompat;

import static java.util.Arrays.asList;

public class ConnectionService extends Service {

    private final static String TAG = ConnectionService.class.getSimpleName();

    public static final String WEIGHT_SERVICE_UUID = "dff971a9-142a-4021-a8d2-f5298ab2bdbb";
    public static final String SETTINGS_CHARACTERISTIC_UUID = "76053035-3aa1-4148-a70d-a73e35332418";
    public static final String STATUS_CHARACTERISTIC_UUID = "c5c78e8f-5963-4642-bd24-bbb8507e22ca";
    public static final String CALIBRATION_WEIGHT_CHARACTERISTIC_UUID = "18d456b3-3c7b-43fa-9d3c-db867d2a93b2";
    public static final String CALIBRATION_VALUE_CHARACTERISTIC_UUID = "e5c96eed-c523-4e81-9d8b-1f92f58603dc";
    public static final String WEIGHT_CHARACTERISTIC_UUID = "00002a98-0000-1000-8000-00805f9b34fb";
    public static final String ESPRESSO_WEIGHT_CHARACTERISTIC_UUID = "d0dac8e6-cf56-4e0c-9823-0aed58dc9bfe";
    public static final String ESPRESSO_TIME_CHARACTERISTIC_UUID = "6e980e27-b771-485a-8396-42f1dab56506";

    public static final String ACTION = "org.weblooker.espresso32.changes";
    private static final String DEVICE_NAME = "ESPresso32";

    private static final long SCAN_PERIOD_TIMEOUT = 10000;
    public static final String CONNECTION_STATUS_INTEND_EXTRA_NAME = "CONNECTION_STATUS";
    public static final String SERVICE_NOTICE = "Bluetooth service is running";
    public static final String STOP_APP = "stopApp";

    private final IBinder mBinder = new ConnectionServiceBinder();
    private final List<BluetoothGattService> services = new ArrayList<>();
    private final List<BluetoothGatt> bluetoothGatt = new ArrayList<>();
    private final Map<String, BluetoothGattCharacteristic> characteristics = new HashMap<>();

    private final AtomicBoolean isWriting = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isKilled = new AtomicBoolean(false);

    private Handler bleTimeoutHandler;
    private BluetoothAdapter mBluetoothAdapter;

    private ConnectionService.MyBroadcastReceiver receiver = null;
    private PreferencesUtil preferencesUtil;
    private BleCommands bleCommands;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Intent myIntent = new Intent(context, EnableDependenciesActivity.class);
                        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(myIntent);
                        stopSelf();
                }
            }
            if (action != null && action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Intent myIntent = new Intent(context, EnableDependenciesActivity.class);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(myIntent);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = DEVICE_NAME;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    DEVICE_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Intent notificationIntent = new Intent(this.getApplicationContext(), MainActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openAppPendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Intent stopAppIntent = new Intent(STOP_APP);
            stopAppIntent.putExtra(STOP_APP, true);
            PendingIntent stopAppPendingIntent = PendingIntent.getBroadcast(this,
                    new Random().nextInt(), stopAppIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                    .setContentTitle(DEVICE_NAME)
                    .setContentIntent(openAppPendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.Stop), stopAppPendingIntent)
                    .setContentText(SERVICE_NOTICE).build();

            startForeground(1, notification);
        }
        bleTimeoutHandler = new Handler();
        preferencesUtil = new PreferencesUtil(this.getApplicationContext());
        bleCommands = new BleCommands();

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent myIntent = new Intent(this, EnableDependenciesActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(myIntent);
        }
        LocationManager locationManager = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent myIntent = new Intent(this, EnableDependenciesActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(myIntent);
        }

        receiver = new ConnectionService.MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(receiver, filter,RECEIVER_EXPORTED);

    }

    public boolean tare() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addTarCommandToQueue(bluetoothGattCharacteristic);
        executeBleCommand();

        return true;
    }

    public boolean setEspressoModus() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addWriteValueToCommandToQueue(bluetoothGattCharacteristic, ScaleModus.ESPRESSO_MODUS.toString());
        executeBleCommand();

        return true;
    }

    public boolean setWeightModus() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addWriteValueToCommandToQueue(bluetoothGattCharacteristic, ScaleModus.WEIGHT_MODUS.toString());
        executeBleCommand();

        return true;
    }

    public boolean setCalibrationWeight(String g) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(CALIBRATION_WEIGHT_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addWriteValueToCommandToQueue(bluetoothGattCharacteristic, g);
        executeBleCommand();

        return true;
    }

    public boolean setCalibrationModus() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addWriteValueToCommandToQueue(bluetoothGattCharacteristic, ScaleModus.CALIBRATION_MODUS.toString());
        executeBleCommand();

        return true;
    }

    public boolean getCalibrationValue() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(CALIBRATION_VALUE_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addReadValueToCommandToQueue(bluetoothGattCharacteristic);
        executeBleCommand();

        return true;
    }

    public boolean setCalibrationValue(String value) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(CALIBRATION_VALUE_CHARACTERISTIC_UUID);

        if (checkIfCharacteristicExists(bluetoothGattCharacteristic))
            return false;

        bleCommands.addWriteValueToCommandToQueue(bluetoothGattCharacteristic, value);
        executeBleCommand();

        preferencesUtil.setCalibrationValue(value);
        return true;
    }

    private boolean checkIfCharacteristicExists(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (bluetoothGattCharacteristic == null) {
            Log.i(TAG, "Can't execute BLE command because not found device");
            return true;
        }
        return false;
    }

    public void executeBleCommand() {
        if (!isConnected.get()) {
            return;
        }
        if (isWriting.get()) {
            return;
        }
        if (!bleCommands.commandQueueHasEntries()) {
            return;
        }

        isWriting.set(true);
        BleJob job = bleCommands.getNextEntry();
        if (job == null) {
            Log.i(TAG, "No job found. Nothing todo");
            isWriting.set(false);
            return;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(job.getCharacteristic());
        if (bluetoothGattCharacteristic == null) {
            Log.i(TAG, "No Characteristic found. Nothing todo");
            isWriting.set(false);
            return;
        }

        Log.i(TAG, "Execute BLE command: " + bluetoothGattCharacteristic.getUuid());
        if ("write".equals(job.getType())) {
            bluetoothGattCharacteristic.setValue(job.getValue().getBytes());
            bluetoothGatt.get(bluetoothGatt.size() - 1).writeCharacteristic(bluetoothGattCharacteristic);
        } else if ("disconnect".equals(job.getType())) {
            bluetoothGatt.get(bluetoothGatt.size() - 1).setCharacteristicNotification(bluetoothGattCharacteristic, false);
            bluetoothGatt.get(bluetoothGatt.size() - 1).writeCharacteristic(bluetoothGattCharacteristic);
        } else if ("connect".equals(job.getType())) {
            bluetoothGatt.get(bluetoothGatt.size() - 1).setCharacteristicNotification(bluetoothGattCharacteristic, true);
            bluetoothGatt.get(bluetoothGatt.size() - 1).writeCharacteristic(bluetoothGattCharacteristic);
        } else {
            boolean b = bluetoothGatt.get(bluetoothGatt.size() - 1).readCharacteristic(bluetoothGattCharacteristic);
            Log.i(TAG, "Execute read BLE command: " + bluetoothGattCharacteristic.getUuid() + " " + b);
        }
    }

    public void reconnect() {
        if (isConnected.compareAndSet(false, false)) {
            isConnected.set(false);
            while (!isWriting.compareAndSet(false, true)) {
                Log.i(TAG, "Waiting...");
            }
            bleCommands.clearCommandQueue();
            isWriting.set(false);
            scanLeDevice(true);
        }
    }

    public String connectionStatus() {
        if (isConnected.get()) {
            return BleStatus.CONNECTED.toString();
        } else {
            return BleStatus.DISCONNECTED.toString();
        }
    }

    public void scanLeDevice(final boolean enable) {
        if (!mBluetoothAdapter.isEnabled())
            return;

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        Log.i(TAG, "Scan");
        if (enable) {
            if (!isConnected.get())
                sendConnectionStatusIntent(BleStatus.SCANNING.toString());

            // Stops scanning after some time
            bleTimeoutHandler.postDelayed(() -> {
                try {
                    Log.i(TAG, "Stop scan");
                    bluetoothLeScanner.stopScan(mLeScanCallback);
                } catch (Exception e) {
                    Log.i(TAG, "Looks like bluetooth is not enabled");
                } finally {
                    if (!isConnected.get())
                        sendConnectionStatusIntent(BleStatus.DISCONNECTED.toString());
                }
            }, SCAN_PERIOD_TIMEOUT);

            ScanFilter filter = new ScanFilter.Builder().setDeviceName(DEVICE_NAME).setServiceUuid(ParcelUuid.fromString(WEIGHT_SERVICE_UUID)).build();
            ScanSettings.Builder builder = new ScanSettings.Builder();
            bluetoothLeScanner.startScan(asList(filter), builder.build(), mLeScanCallback);
        } else {
            bluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                private boolean initCalibrationValue = true;

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        initCalibrationValue = true;
                        Log.i(TAG, "Connected to GATT server");
                        isConnected.set(true);
                        gatt.discoverServices();

                        sendConnectionStatusIntent(BleStatus.CONNECTED.toString());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        isConnected.set(false);
                        sendConnectionStatusIntent(BleStatus.DISCONNECTED.toString());
                        Log.i(TAG, "Disconnected from GATT server");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    gatt.connect();
                    Log.i(TAG, "Discovered service: " + gatt.getServices().toString());
                    setNotification(gatt);

                    // Place to get on scale stored values on connect
                    getCalibrationValue();
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Intent intent = new Intent();
                    intent.setAction(ACTION);
                    intent.putExtra("type", characteristic.getUuid().toString());
                    intent.putExtra("value", characteristic.getStringValue(0));
                    getApplicationContext().sendBroadcast(intent);
                    Log.i(TAG, characteristic.getUuid() + " Value: " + characteristic.getStringValue(0));
                    isWriting.set(false);
                    executeBleCommand();

                    getAndStoreCalibrationValueOfScale(characteristic);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);

                    // For small value the notification has enough space
                    if (WEIGHT_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION);
                        intent.putExtra("type", characteristic.getUuid().toString());
                        intent.putExtra("value", characteristic.getStringValue(0));
                        getApplicationContext().sendBroadcast(intent);
                        Log.i(TAG, "Value has changed/read for characteristic: " + characteristic.getUuid());
                    } else { // For big values we need to get the value
                        bleCommands.addReadValueToCommandToQueue(characteristic);
                        Log.i(TAG, "Value has changed for characteristic: " + characteristic.getUuid());
                    }
                    executeBleCommand();
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    isWriting.set(false);
                    executeBleCommand();
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                    Log.d(TAG, "Mtu is changed to: " + mtu);
                }

                private void getAndStoreCalibrationValueOfScale(BluetoothGattCharacteristic characteristic) {
                    if (initCalibrationValue && CALIBRATION_VALUE_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                        preferencesUtil.setCalibrationValue(characteristic.getStringValue(0));
                        initCalibrationValue = false;
                    }
                }
            };

    private void sendConnectionStatusIntent(String status) {
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.putExtra("type", CONNECTION_STATUS_INTEND_EXTRA_NAME);
        intent.putExtra("value", status);
        getApplicationContext().sendBroadcast(intent);
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!mBluetoothAdapter.isEnabled())
                return;

            if (result.getDevice() != null && result.getDevice().getName() != null &&
                    result.getDevice().getName().equals(DEVICE_NAME) && bluetoothGatt.isEmpty()) {
                Log.i(TAG, "Found device: " + result.getDevice().getName());
                storeGatt(result.getDevice());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            String devices = "";
            for (ScanResult result : results)
                for (ParcelUuid uuid : result.getDevice().getUuids())
                    devices += " " + uuid.toString();
            Log.i(TAG, "Found devices: " + devices);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "Error on device search: " + errorCode);
        }
    };

    protected void storeGatt(BluetoothDevice de) {
        if (!isConnected.get() && !isKilled.get()) {
            BluetoothGatt bluetoothGatt = de.connectGatt(this, true, gattCallback);
            //bluetoothGatt.requestMtu(128);
            this.bluetoothGatt.add(bluetoothGatt);
        }
    }

    protected void setNotification(BluetoothGatt gatt) {

        List<BluetoothGattService> servicesTmp = gatt.getServices();
        List<BluetoothGattService> servicesNew = new ArrayList<>();

        for (BluetoothGattService bluetoothGattService : servicesTmp) {
            if (!services.contains(bluetoothGattService)) {
                servicesNew.add(bluetoothGattService);
            }
        }

        services.addAll(servicesNew);
        for (BluetoothGattService service : servicesNew) {
            Log.i(TAG, "Go though characteristics of service: " + service.getUuid());
            List<BluetoothGattCharacteristic> characteristicsLocal = service.getCharacteristics();
            List<BluetoothGattCharacteristic> characteristicsNew = new ArrayList<>();

            for (BluetoothGattCharacteristic x : characteristicsLocal) {
                if (!characteristics.containsKey(x.getUuid().toString())) {
                    characteristicsNew.add(x);
                    characteristics.put(x.getUuid().toString(), x);
                }
            }
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristicsNew) {
                boolean successfully = gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                gatt.readCharacteristic(bluetoothGattCharacteristic);
                Log.i(TAG, "Enable notification for Characteristic: " + bluetoothGattCharacteristic.getUuid() + " " + successfully);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ConnectionServiceBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Cleanup on app exit");
        isKilled.set(true);
        isConnected.set(false);
        bleCommands.clearCommandQueue();
        isWriting.set(false);

        for (BluetoothGatt gatt : bluetoothGatt) {
            gatt.disconnect();
            gatt.close();
        }
        bluetoothGatt.clear();
        services.clear();
        characteristics.clear();
        unregisterReceiver(receiver);
    }
}