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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.weblooker.espresso32.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EnableDependenciesActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 4242;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_dependencies);
        check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        check();
    }

    public void enableBLE(View view) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_CODE);
    }

    public void enableGPS(View view) {
        Intent enableBtIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(enableBtIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent myIntent = new Intent(EnableDependenciesActivity.this, MainActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            EnableDependenciesActivity.this.startActivity(myIntent);
        }
    }

    private void check() {
        Button btnBle = findViewById(R.id.enableBLE);
        Button btnGps = findViewById(R.id.enableGPS);

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            btnBle.setEnabled(true);
        } else {
            btnBle.setEnabled(false);
        }
        LocationManager locationManager = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            btnGps.setEnabled(true);
        } else {
            btnGps.setEnabled(false);
        }

        if (mBluetoothAdapter.isEnabled() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent myIntent = new Intent(this, MainActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(myIntent);
        }
    }
}