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
import android.widget.TextView;

import org.weblooker.espresso32.R;
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
                TextView textView = (TextView) findViewById(R.id.calibrationActivityWeight);
                textView.setText(value);
            }
            if (ConnectionService.SETTINGS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
            }
            if (ConnectionService.CALIBRATION_VALUE_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = (TextView) findViewById(R.id.calibrationActivityCalVal);
                textView.setText(value);
            }
            if (ConnectionService.STATUS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = (TextView) findViewById(R.id.calibrateActivityModus);
                textView.setText(value);

                if(value != null && value.equals("READY"))
                {
                    textView = (TextView) findViewById(R.id.calibrationActivityCalVal);
                    if(textView.getText().toString().length() > 0) {
                        textView = (TextView) findViewById(R.id.setCalibrationButton);
                        textView.setEnabled(true);
                    }
                    textView = (TextView) findViewById(R.id.setManualCalibrationButton);
                    textView.setEnabled(true);
                    textView = (TextView) findViewById(R.id.calibrateModus);
                    textView.setEnabled(true);
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

        receiver = new CalibrateActivity.MyBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ConnectionService.ACTION));
        PreferencesUtil preferencesUtil = new PreferencesUtil(this.getApplicationContext());

        String value =  preferencesUtil.getCalibrationValue();
        TextView textView = (TextView) findViewById(R.id.calibrateActivityCalValueManual);
        textView.setText(value);
        textView = (TextView) findViewById(R.id.calibrateActivityCalStartValue);
        textView.setText(value);
    }

    public void calibrateModus(View view) {
        TextView textView = (TextView) findViewById(R.id.calibrateActivityCalStartValue);
        String startCalValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(startCalValue);

        textView = (TextView) findViewById(R.id.calibrateActivityCalWeight);
        String weight = textView.getText().toString();
        mConnectionService.setCalibrationWeight(weight);
        mConnectionService.setCalibrationModus();
        boolean executed = mConnectionService.getCalibrationValue();

        if(executed) {
            textView = (TextView) findViewById(R.id.setManualCalibrationButton);
            textView.setEnabled(false);
            textView = (TextView) findViewById(R.id.calibrateModus);
            textView.setEnabled(false);
        }
    }

    public void setCalibrationValue(View view)
    {
        TextView textView = (TextView) findViewById(R.id.calibrationActivityCalVal);
        String calValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(calValue);
    }

    public void setCalibrationValueManual(View view)
    {
        TextView textView = (TextView) findViewById(R.id.calibrateActivityCalValueManual);
        String calValue = textView.getText().toString();
        mConnectionService.setCalibrationValue(calValue);
    }
}