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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.models.EspressoResult;
import org.weblooker.espresso32.models.EspressoResults;
import org.weblooker.espresso32.services.ConnectionService;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class EspressoActivity extends AppCompatActivity {

    private ConnectionService mConnectionService;
    private boolean mConnectionServiceBound = false;
    private MyBroadcastReceiver receiver = null;
    private static ArrayList<Entry> values = new ArrayList<>();
    private LineDataSet coffee_flow;
    private LineData data;
    private LineChart lineChart;

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String value;
            if (ConnectionService.ESPRESSO_WEIGHT_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
               Log.i("debg","Nothing todo");
            }
            if (ConnectionService.SETTINGS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = (TextView) findViewById(R.id.espressoActivityModus);
                textView.setText(value);

                Button btn = (Button) findViewById(R.id.espressoModus);
                btn.setEnabled(!"ESPRESSO_MODUS".equals(value));
            }
            if (ConnectionService.ESPRESSO_TIME_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                ObjectMapper objectMapper = new ObjectMapper();
                EspressoResults espressoResults;
                try {
                   espressoResults = objectMapper.readValue(value, EspressoResults.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return;
                }

                TextView textViewTime = (TextView) findViewById(R.id.espressoActivityTime);
                TextView textViewWeight = (TextView) findViewById(R.id.espressoActivityWeight);

                for(EspressoResult el :espressoResults.getResults())
                {
                    BigDecimal timeInSeconds = new BigDecimal(el.getT()).divide(new BigDecimal("1000.0"), MathContext.DECIMAL128).setScale(2,BigDecimal.ROUND_HALF_UP);
                    Float time = timeInSeconds.floatValue();
                    Float weight = el.getW();

                    setTimerColor(timeInSeconds, textViewTime);
                    textViewTime.setText(timeInSeconds + "s");
                    textViewWeight.setText(weight + "g");

                    updateLineChart(weight,time);
                }
            }
            if (ConnectionService.STATUS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = (TextView) findViewById(R.id.espressoActivityStatus);
                textView.setText(value);

                TextView textViewNote = (TextView) findViewById(R.id.espressoActivityNote);
                if ("TARE".equals(value)) {
                    textViewNote.setText("Note: Tare weight of cup.");
                }
                if ("WAITING".equals(value)) {
                    textViewNote.setText("Note: Waiting for first drop of espresso.");
                }
                if ("MEASSURE".equals(value)) {
                    textViewNote.setText("Note: Espresso is in progress.");
                }
                if ("READY".equals(value)) {
                    textViewNote.setText("Note: Done.");
                }
            }
        }

        private void setTimerColor(BigDecimal timeInSeconds, TextView textView) {
            if (timeInSeconds.compareTo(new BigDecimal(22)) < 0) {
                textView.setTextColor(Color.RED);
            }
            if (timeInSeconds.compareTo(new BigDecimal(22)) >= 0 && timeInSeconds.compareTo(new BigDecimal(30)) < 0) {
                textView.setTextColor(Color.BLACK);
            }
            if (timeInSeconds.compareTo(new BigDecimal(30)) >= 0) {
                textView.setTextColor(Color.RED);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_espresso);

        receiver = new MyBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ConnectionService.ACTION));
        initChat();
    }

    private void initChat() {
        if(lineChart != null)
            lineChart.clear();
        if(coffee_flow != null)
            coffee_flow.clear();

        lineChart = findViewById(R.id.flowChart);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        Description desc = new Description();
        desc.setText("Coffee flow");
        desc.setYOffset(-7);
        lineChart.setDescription(desc);
        lineChart.setNoDataText("");
        lineChart.setNoDataTextColor(Color.BLACK);
        lineChart.setMaxVisibleValueCount(20);

        LimitLine limitLineH = new LimitLine(30, "Max");
        limitLineH.setLineColor(Color.BLACK);
        LimitLine limitLineL = new LimitLine(22, "Min");
        limitLineL.setLineColor(Color.BLACK);
        lineChart.getXAxis().addLimitLine(limitLineL);
        lineChart.getXAxis().addLimitLine(limitLineH);
        lineChart.getXAxis().setAxisMinimum(0);
        lineChart.getXAxis().setLabelCount(8);
        lineChart.getXAxis().setAxisMaximum(35);
        lineChart.getAxisLeft().setAxisMinimum(0);
        lineChart.getAxisLeft().setAxisMaximum(40);


        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toString() + "s";
            }
        });

        lineChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toString() + "g";
            }
        });

        lineChart.getAxisRight().setEnabled(false);

        coffee_flow = new LineDataSet(values, "Coffee in g");
        /*coffee_flow.setColor(R.color.coffee);
        coffee_flow.setCircleColor(R.color.coffee);
        coffee_flow.setCircleHoleColor(R.color.coffee);
        coffee_flow.setFillColor(R.color.coffee);*/
        data = new LineData(coffee_flow);
    }

    private void updateLineChart(Float weight,Float time) {
        data.addEntry(new Entry(time, weight), data.getIndexOfDataSet(coffee_flow));

        float maxValue = lineChart.getXAxis().getAxisMaximum();
        if (time > maxValue)
            lineChart.getXAxis().setAxisMaximum(time + 1);

        maxValue = lineChart.getAxisLeft().getAxisMaximum();
        if (weight > maxValue)
            lineChart.getAxisLeft().setAxisMaximum(weight + 1);

        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
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
    protected void onDestroy() {
        unregisterReceiver();
        super.onDestroy();
    }

    public void espressoModus(View view) {
        cleanupScreen();
        mConnectionService.setEspressoModus();
    }

    private void unregisterReceiver() {
        try {
            this.unregisterReceiver(receiver);
            receiver = null;
        } catch (Exception e) {
            // Nothing todo
        }
    }

    private void cleanupScreen() {
        ((TextView) findViewById(R.id.espressoActivityWeight)).setText("");
        ((TextView) findViewById(R.id.espressoActivityTime)).setText("");
        values = new ArrayList<>();
        initChat();
    }
}