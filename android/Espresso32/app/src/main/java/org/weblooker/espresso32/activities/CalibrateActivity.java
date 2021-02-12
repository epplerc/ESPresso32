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

package org.weblooker.espresso32.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.models.BleStatus;
import org.weblooker.espresso32.models.ScaleStatus;
import org.weblooker.espresso32.services.ConnectionService;
import org.weblooker.espresso32.utils.PreferencesUtil;

import androidx.appcompat.app.AppCompatActivity;

public class CalibrateActivity extends AppCompatActivity {

    private ConnectionService mConnectionService;
    private boolean mConnectionServiceBound = false;
    private CalibrateActivity.MyBroadcastReceiver receiver = null;


    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String value;
            if (ConnectionService.WEIGHT_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.calibrationActivityWeight);
                textView.setText(value);
            }
            if (ConnectionService.SETTINGS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
            }
            if (ConnectionService.CALIBRATION_VALUE_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.calibrationActivityCalVal);
                textView.setText(value);
            }
            if (ConnectionService.STATUS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.calibrateActivityModus);
                textView.setText(value);

                if (value != null && value.equals(ScaleStatus.READY.toString())) {
                    textView = findViewById(R.id.calibrationActivityCalVal);
                    if (textView.getText().toString().length() > 0) {
                        textView = findViewById(R.id.setCalibrationButton);
                        textView.setEnabled(true);
                    }
                    textView = findViewById(R.id.setManualCalibrationButton);
                    textView.setEnabled(true);
                    textView = findViewById(R.id.calibrateModus);
                    textView.setEnabled(true);
                }


            }
            if (ConnectionService.CONNECTION_STATUS_INTEND_EXTRA_NAME.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                Button btn = findViewById(R.id.setManualCalibrationButton);
                Button btn2 = findViewById(R.id.calibrateModus);
                if (BleStatus.CONNECTED.toString().equals(value)) {
                    mConnectionService.getCalibrationValue();
                    btn.setEnabled(true);
                    btn2.setEnabled(true);
                } else {
                    btn.setEnabled(false);
                    btn2.setEnabled(false);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnectionServiceBound) {
            unbindService(mServiceConnection);
            mConnectionServiceBound = false;
        }
        if (receiver.isOrderedBroadcast()) {
            this.unregisterReceiver(receiver);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnectionServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectionService.ConnectionServiceBinder myBinder = (ConnectionService.ConnectionServiceBinder) service;
            mConnectionService = myBinder.getService();
            mConnectionServiceBound = true;
            if (mConnectionService.connectionStatus().equals(BleStatus.CONNECTED.toString())) {
                mConnectionService.getCalibrationValue();
                activateButton();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ConnectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        receiver = new CalibrateActivity.MyBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ConnectionService.ACTION));
        PreferencesUtil preferencesUtil = new PreferencesUtil(this.getApplicationContext());

        String value = preferencesUtil.getCalibrationValue();
        TextView textView = findViewById(R.id.calibrateActivityCalValueManual);
        textView.setText(value);
        textView = findViewById(R.id.calibrateActivityCalStartValue);
        textView.setText(value);
    }

    public void calibrateModus(View view) {
        TextView textView = findViewById(R.id.calibrateActivityCalStartValue);
        String startCalValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(startCalValue);

        textView = findViewById(R.id.calibrateActivityCalWeight);
        String weight = textView.getText().toString();
        mConnectionService.setCalibrationWeight(weight);
        mConnectionService.setCalibrationModus();
        boolean executed = mConnectionService.getCalibrationValue();

        if (executed) {
            textView = findViewById(R.id.setManualCalibrationButton);
            textView.setEnabled(false);
            textView = findViewById(R.id.calibrateModus);
            textView.setEnabled(false);
        }
    }

    public void setCalibrationValue(View view) {
        TextView textView = findViewById(R.id.calibrationActivityCalVal);
        String calValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(calValue);
    }

    public void setCalibrationValueManual(View view) {
        TextView textView = findViewById(R.id.calibrateActivityCalValueManual);
        String calValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(calValue);
    }

    public void activateButton() {
        Button btn = findViewById(R.id.setManualCalibrationButton);
        Button btn2 = findViewById(R.id.calibrateModus);
        btn.setEnabled(true);
        btn2.setEnabled(true);
    }
}