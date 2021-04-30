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

import android.Manifest;
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
import org.weblooker.espresso32.services.ConnectionService;
import org.weblooker.espresso32.utils.PreferencesUtil;
import org.weblooker.espresso32.utils.UiUtil;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ConnectionService mConnectionService;
    private boolean mConnectionServiceBound = false;
    private MyBroadcastReceiver receiver = null;
    private PreferencesUtil pref;

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String value;

            if (ConnectionService.WEIGHT_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.Weight);
                textView.setText(value + "g");
            }
            if (ConnectionService.CONNECTION_STATUS_INTEND_EXTRA_NAME.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.ConnectionStatus);
                textView.setText(value);

                Button btn = findViewById(R.id.reconnect);
                if (BleStatus.CONNECTED.toString().equals(value) ||
                        BleStatus.SCANNING.toString().equals(value)) {
                    btn.setEnabled(false);
                } else {
                    btn.setEnabled(true);
                }
            }
        }
    }

    public void tare(View view) {
        mConnectionService.tare();
    }

    public void startEspressoActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, EspressoActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void startCalibrateActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, CalibrateActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void startAboutActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, AboutActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void startSettingsActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void startTimelineActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, TimelineActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void startExportActivity(View view) {
        Intent myIntent = new Intent(MainActivity.this, ExportActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void storeWeight(View view) {
        TextView textView = findViewById(R.id.Weight);
        String weightString = textView.getText().toString();
        float weight;
        try {
            weight = Float.parseFloat(weightString.subSequence(0, weightString.indexOf("g") - 1).toString());
            pref.setWeightOfCoffee(weight);
        } catch (Exception e) {
            // Nothing to do can be happen if no weight is shown
            return;
        }
        UiUtil.makeToast(this, getString(R.string.remember_weight) +" "+ weightString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);

        pref = new PreferencesUtil(this.getApplicationContext());
        receiver = new MyBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ConnectionService.ACTION));
        this.requestPermissions(permissions.toArray(new String[0]), 1);
        setContentView(R.layout.activity_main);
    }

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
    protected void onResume() {
        super.onResume();
        if (mConnectionServiceBound) {
            String s = mConnectionService.connectionStatus();
            TextView textView = findViewById(R.id.ConnectionStatus);
            textView.setText(s);

            Button btn = findViewById(R.id.reconnect);
            btn.setEnabled(!BleStatus.CONNECTED.toString().equals(s));
        }
    }

    @Override
    protected void onStop() {
        if (mConnectionServiceBound) {
            unbindService(mServiceConnection);
            mConnectionServiceBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver();
        super.onDestroy();
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
            mConnectionService.scanLeDevice(true);
        }
    };

    private void unregisterReceiver() {
        try {
            this.unregisterReceiver(receiver);
            receiver = null;
        } catch (Exception e) {
            // Nothing to do
        }
    }

    public void reconnect(View view) {
        mConnectionService.reconnect();
    }
}