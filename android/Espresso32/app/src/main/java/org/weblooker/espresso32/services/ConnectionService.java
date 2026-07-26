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
import android.bluetooth.BluetoothGattDescriptor;
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
import android.content.pm.ServiceInfo;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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
    public static final String CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String ACTION = "org.weblooker.espresso32.changes";
    private static final String DEVICE_NAME = "ESPresso32";

    private static final long SCAN_PERIOD_TIMEOUT = 10000;
    public static final String CONNECTION_STATUS_INTEND_EXTRA_NAME = "CONNECTION_STATUS";
    public static final String SERVICE_NOTICE = "Bluetooth service is running";
    public static final String STOP_APP = "stopApp";

    private final IBinder mBinder = new ConnectionServiceBinder();
    private final Map<String, BluetoothGattCharacteristic> characteristics = new HashMap<>();

    private BluetoothGatt mBluetoothGatt = null;
    private final AtomicBoolean isWriting = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isKilled = new AtomicBoolean(false);

    private Handler mainHandler;
    private BluetoothAdapter mBluetoothAdapter;

    private ConnectionService.MyBroadcastReceiver receiver = null;
    private PreferencesUtil preferencesUtil;
    private BleCommands bleCommands;

    private final Runnable writeTimeoutRunnable = () -> {
        if (isWriting.get()) {
            Log.w(TAG, "GATT operation timeout. Resetting lock.");
            isWriting.set(false);
            executeBleCommand();
        }
    };

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        closeGatt();
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
        mainHandler = new Handler(Looper.getMainLooper());
        
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

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } else {
                startForeground(1, notification);
            }
        }
        
        preferencesUtil = new PreferencesUtil(this.getApplicationContext());
        bleCommands = new BleCommands();

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        
        receiver = new ConnectionService.MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    public boolean tare() {
        BluetoothGattCharacteristic charac = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addTarCommandToQueue(charac);
        executeBleCommand();
        return true;
    }

    public boolean setEspressoModus() {
        BluetoothGattCharacteristic charac = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addWriteValueToCommandToQueue(charac, ScaleModus.ESPRESSO_MODUS.toString());
        executeBleCommand();
        return true;
    }

    public boolean setWeightModus() {
        BluetoothGattCharacteristic charac = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addWriteValueToCommandToQueue(charac, ScaleModus.WEIGHT_MODUS.toString());
        executeBleCommand();
        return true;
    }

    public boolean setCalibrationWeight(String g) {
        BluetoothGattCharacteristic charac = characteristics.get(CALIBRATION_WEIGHT_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addWriteValueToCommandToQueue(charac, g);
        executeBleCommand();
        return true;
    }

    public boolean setCalibrationModus() {
        BluetoothGattCharacteristic charac = characteristics.get(SETTINGS_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addWriteValueToCommandToQueue(charac, ScaleModus.CALIBRATION_MODUS.toString());
        executeBleCommand();
        return true;
    }

    public boolean getCalibrationValue() {
        BluetoothGattCharacteristic charac = characteristics.get(CALIBRATION_VALUE_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addReadValueToCommandToQueue(charac);
        executeBleCommand();
        return true;
    }

    public boolean setCalibrationValue(String value) {
        BluetoothGattCharacteristic charac = characteristics.get(CALIBRATION_VALUE_CHARACTERISTIC_UUID);
        if (charac == null) return false;
        bleCommands.addWriteValueToCommandToQueue(charac, value);
        executeBleCommand();
        preferencesUtil.setCalibrationValue(value);
        return true;
    }

    public void executeBleCommand() {
        if (!isConnected.get() || mBluetoothGatt == null) return;
        if (isWriting.get()) return;
        if (!bleCommands.commandQueueHasEntries()) return;

        isWriting.set(true);
        BleJob job = bleCommands.getNextEntry();
        if (job == null) {
            isWriting.set(false);
            return;
        }

        BluetoothGattCharacteristic characteristic = characteristics.get(job.getCharacteristic());
        if (characteristic == null) {
            Log.w(TAG, "Characteristic not found for UUID: " + job.getCharacteristic());
            isWriting.set(false);
            executeBleCommand();
            return;
        }

        boolean result = false;
        String type = job.getType();
        Log.d(TAG, "Executing BLE job: " + type + " for " + characteristic.getUuid());

        mainHandler.removeCallbacks(writeTimeoutRunnable);
        mainHandler.postDelayed(writeTimeoutRunnable, 2500);

        if ("write".equals(type)) {
            characteristic.setValue(job.getValue().getBytes());
            result = mBluetoothGatt.writeCharacteristic(characteristic);
        } else if ("read".equals(type)) {
            result = mBluetoothGatt.readCharacteristic(characteristic);
        } else if ("write_descriptor".equals(type)) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(job.getDescriptor()));
            if (descriptor != null) {
                descriptor.setValue(job.getData());
                result = mBluetoothGatt.writeDescriptor(descriptor);
            } else {
                Log.e(TAG, "Descriptor " + job.getDescriptor() + " not found for " + characteristic.getUuid());
                result = false;
            }
        }

        if (!result) {
            Log.e(TAG, "Failed to initiate GATT operation: " + type);
            mainHandler.removeCallbacks(writeTimeoutRunnable);
            isWriting.set(false);
            executeBleCommand();
        }
    }

    public void reconnect() {
        Log.i(TAG, "Reconnect requested");
        closeGatt();
        scanLeDevice(true);
    }

    private void closeGatt() {
        isConnected.set(false);
        isWriting.set(false);
        mainHandler.removeCallbacks(writeTimeoutRunnable);
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        characteristics.clear();
        bleCommands.clearCommandQueue();
    }

    public String connectionStatus() {
        return isConnected.get() ? BleStatus.CONNECTED.toString() : BleStatus.DISCONNECTED.toString();
    }

    public void scanLeDevice(final boolean enable) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) return;

        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) return;

        if (enable) {
            if (!isConnected.get()) sendConnectionStatusIntent(BleStatus.SCANNING.toString());

            mainHandler.postDelayed(() -> {
                Log.i(TAG, "Stop scan timeout");
                scanner.stopScan(mLeScanCallback);
                if (!isConnected.get()) sendConnectionStatusIntent(BleStatus.DISCONNECTED.toString());
            }, SCAN_PERIOD_TIMEOUT);

            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName(DEVICE_NAME)
                    .setServiceUuid(ParcelUuid.fromString(WEIGHT_SERVICE_UUID))
                    .build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanner.startScan(asList(filter), settings, mLeScanCallback);
        } else {
            scanner.stopScan(mLeScanCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        private boolean initCalibrationValue = true;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT status error: " + status);
                closeGatt();
                sendConnectionStatusIntent(BleStatus.DISCONNECTED.toString());
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected. Delaying MTU and service discovery...");
                isConnected.set(true);
                initCalibrationValue = true;
                mainHandler.postDelayed(() -> {
                    if (mBluetoothGatt != null) {
                        Log.i(TAG, "Requesting MTU...");
                        mBluetoothGatt.requestMtu(247);
                    }
                }, 600);
                sendConnectionStatusIntent(BleStatus.CONNECTED.toString());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected.");
                closeGatt();
                sendConnectionStatusIntent(BleStatus.DISCONNECTED.toString());
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.i(TAG, "onMtuChanged: " + mtu + " status=" + status);
            mainHandler.postDelayed(() -> {
                if (mBluetoothGatt != null) {
                    Log.i(TAG, "Discovering services...");
                    mBluetoothGatt.discoverServices();
                }
            }, 500);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.");
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                setupNotifications(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic charac, int status) {
            mainHandler.removeCallbacks(writeTimeoutRunnable);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(charac);
                if (initCalibrationValue && CALIBRATION_VALUE_CHARACTERISTIC_UUID.equals(charac.getUuid().toString())) {
                    String val = charac.getStringValue(0);
                    if (val != null) preferencesUtil.setCalibrationValue(val);
                    initCalibrationValue = false;
                }
            }
            isWriting.set(false);
            executeBleCommand();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic charac, int status) {
            mainHandler.removeCallbacks(writeTimeoutRunnable);
            isWriting.set(false);
            executeBleCommand();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic charac) {
            String uuid = charac.getUuid().toString();
            Log.d(TAG, "onCharacteristicChanged: " + uuid);
            
            // Weight is small and frequent, broadcast immediately
            if (uuid.equals(WEIGHT_CHARACTERISTIC_UUID)) {
                broadcastUpdate(charac);
            } else if (isWatchedCharacteristic(uuid)) {
                // For Status, Modus, and JSON values, restore original behavior of queuing a read
                // This ensures we get the full and correct value even if it's large.
                Log.d(TAG, "Queuing read for watched characteristic change: " + uuid);
                bleCommands.addReadValueToCommandToQueue(charac);
                executeBleCommand();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mainHandler.removeCallbacks(writeTimeoutRunnable);
            isWriting.set(false);
            executeBleCommand();
        }
    };

    private void setupNotifications(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics()) {
                String uuid = charac.getUuid().toString();
                characteristics.put(uuid, charac);
                
                if (isWatchedCharacteristic(uuid)) {
                    Log.i(TAG, "Processing watched characteristic: " + uuid);
                    
                    int props = charac.getProperties();
                    // Enable notifications locally
                    if ((props & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
                        gatt.setCharacteristicNotification(charac, true);
                        
                        // Remote Enable via CCCD
                        byte[] cccdVal = null;
                        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            cccdVal = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                        } else if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                            cccdVal = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                        }
                        
                        if (cccdVal != null) {
                            Log.d(TAG, "Queuing CCCD write for: " + uuid);
                            bleCommands.addDescriptorWriteToQueue(charac, CCCD_UUID, cccdVal);
                        }
                    }
                    
                    // Initial read to populate UI
                    if ((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        Log.d(TAG, "Queuing initial read for: " + uuid);
                        bleCommands.addReadValueToCommandToQueue(charac);
                    }
                }
            }
        }
        executeBleCommand();
    }

    private boolean isWatchedCharacteristic(String uuid) {
        return uuid.equals(WEIGHT_CHARACTERISTIC_UUID) || 
               uuid.equals(ESPRESSO_WEIGHT_CHARACTERISTIC_UUID) || 
               uuid.equals(ESPRESSO_TIME_CHARACTERISTIC_UUID) ||
               uuid.equals(STATUS_CHARACTERISTIC_UUID) ||
               uuid.equals(SETTINGS_CHARACTERISTIC_UUID) ||
               uuid.equals(CALIBRATION_VALUE_CHARACTERISTIC_UUID);
    }

    private void broadcastUpdate(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        String value = characteristic.getStringValue(0);
        if (value == null) value = "";
        
        Log.d(TAG, "Broadcasting update for " + uuid + " value: " + value);
        
        Intent intent = new Intent(ACTION);
        intent.putExtra("type", uuid);
        intent.putExtra("value", value);
        getApplicationContext().sendBroadcast(intent);
    }

    private void sendConnectionStatusIntent(String status) {
        Intent intent = new Intent(ACTION);
        intent.putExtra("type", CONNECTION_STATUS_INTEND_EXTRA_NAME);
        intent.putExtra("value", status);
        getApplicationContext().sendBroadcast(intent);
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && DEVICE_NAME.equals(device.getName()) && mBluetoothGatt == null && !isKilled.get()) {
                Log.i(TAG, "Found target device. Connecting...");
                mainHandler.post(() -> {
                    if (mBluetoothGatt == null && !isKilled.get()) {
                        mBluetoothGatt = device.connectGatt(ConnectionService.this, false, gattCallback);
                    }
                });
            }
        }
    };

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
        isKilled.set(true);
        closeGatt();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }
}
