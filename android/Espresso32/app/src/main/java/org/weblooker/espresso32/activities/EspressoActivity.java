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
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.entities.EspressoResultEntity;
import org.weblooker.espresso32.models.BleStatus;
import org.weblooker.espresso32.models.EspressoDiagram;
import org.weblooker.espresso32.models.EspressoResult;
import org.weblooker.espresso32.models.EspressoResults;
import org.weblooker.espresso32.models.ScaleModus;
import org.weblooker.espresso32.models.ScaleStatus;
import org.weblooker.espresso32.services.ConnectionService;
import org.weblooker.espresso32.utils.DbUtil;
import org.weblooker.espresso32.utils.DiagrammSettings;
import org.weblooker.espresso32.utils.PreferencesUtil;
import org.weblooker.espresso32.utils.UiUtil;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;

public class EspressoActivity extends AppCompatActivity {

    private ConnectionService mConnectionService;
    private boolean mConnectionServiceBound = false;
    private MyBroadcastReceiver receiver = null;
    private static ArrayList<Entry> values = new ArrayList<>();
    private LineDataSet coffee_flow;
    private LineDataSet reference;
    private LineData data;
    private LineChart lineChart;
    private PreferencesUtil pref;
    private DbUtil dbUtil;
    private float perfectTime;
    private float ratio;
    private float coffeeIn;
    private float coffeeOut;
    private float defaultCoffeeIn;
    private float coffeeInSelected;
    private float time;
    private String selectedCoffee = "unknown";
    private EspressoDiagram espressoDiagram;

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String value;
            if (ConnectionService.ESPRESSO_WEIGHT_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                Log.i("debug", "Nothing todo");
            }
            if (ConnectionService.SETTINGS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.espressoActivityModus);
                textView.setText(value);

                Button btn = findViewById(R.id.espressoModus);
                btn.setEnabled(!ScaleModus.ESPRESSO_MODUS.toString().equals(value));
                if (ScaleModus.WEIGHT_MODUS.toString().equals(value)) {
                    Button storeButton = findViewById(R.id.store);
                    storeButton.setVisibility(View.VISIBLE);
                    RatingBar ratingView = findViewById(R.id.rating);
                    ratingView.setIsIndicator(false);
                }
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

                TextView textViewTime = findViewById(R.id.espressoActivityTime);
                TextView textViewWeight = findViewById(R.id.espressoActivityWeight);
                TextView textViewRatio = findViewById(R.id.espressoActivityRatio);

                for (EspressoResult el : espressoResults.getResults()) {
                    BigDecimal timeInSeconds = new BigDecimal(el.getT()).divide(new BigDecimal("1000.0"), MathContext.DECIMAL128).setScale(2, BigDecimal.ROUND_HALF_UP);
                    Float tmpTime = timeInSeconds.floatValue();
                    Float weight = el.getW();
                    Float ratio = weight / coffeeInSelected;

                    setTimerColor(timeInSeconds, textViewTime);
                    textViewTime.setText(timeInSeconds + "s");
                    textViewWeight.setText(weight + "g");
                    textViewRatio.setText(String.format(Locale.forLanguageTag("EN"), "%.2f", ratio));

                    updateLineChart(weight, tmpTime);
                    coffeeOut = weight;
                    time = tmpTime;
                }
            }
            if (ConnectionService.STATUS_CHARACTERISTIC_UUID.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                TextView textView = findViewById(R.id.espressoActivityStatus);
                textView.setText(value);

                TextView textViewNote = findViewById(R.id.espressoActivityNote);
                if (ScaleStatus.TARE.toString().equals(value)) {
                    textViewNote.setText(R.string.note_tar);
                }
                if (ScaleStatus.WAITING.toString().equals(value)) {
                    textViewNote.setText(R.string.note_waiting);
                }
                if (ScaleStatus.MEASSURE.toString().equals(value)) {
                    textViewNote.setText(R.string.note_espresso_progress);
                }
                if (ScaleStatus.READY.toString().equals(value)) {
                    textViewNote.setText(R.string.note_done);
                }
            }
            if (ConnectionService.CONNECTION_STATUS_INTEND_EXTRA_NAME.equals(intent.getStringExtra("type"))) {
                value = intent.getStringExtra("value");
                Button btn = findViewById(R.id.espressoModus);
                if (BleStatus.CONNECTED.equals(value)) {
                    btn.setEnabled(true);
                } else {
                    btn.setEnabled(false);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        espressoDiagram = new EspressoDiagram();
        pref = new PreferencesUtil(this.getApplicationContext());
        ratio = pref.getRatio();
        perfectTime = pref.getPerfectTime();
        coffeeIn = pref.getWeightOfCoffee();
        defaultCoffeeIn = pref.getDefaultCoffeeIn();
        receiver = new MyBroadcastReceiver();
        dbUtil = new DbUtil(this);
        UiUtil.setupAutoCompleteTextView(findViewById(R.id.coffee), dbUtil, pref);

        this.registerReceiver(receiver, new IntentFilter(ConnectionService.ACTION));
        initChat();
    }

    private void initChat() {
        if (lineChart != null)
            lineChart.clear();
        if (coffee_flow != null)
            coffee_flow.clear();

        lineChart = DiagrammSettings.setConfiguration(findViewById(R.id.flowChart), pref);
        coffee_flow = DiagrammSettings.getCoffeeFlowDataSet(values);

        createCoffeeFlowReference();
        if (reference != null)
            data = new LineData(Arrays.asList(coffee_flow, reference));
        else
            data = new LineData(coffee_flow);
    }

    private void createCoffeeFlowReference() {
        coffeeInSelected = coffeeIn;

        if (coffeeIn <= 0.1 && defaultCoffeeIn <= 0.0)
            return;

        if (coffeeIn <= 0.1)
            coffeeInSelected = defaultCoffeeIn;


        ArrayList<Entry> valuesRegression = new ArrayList<>();
        float perTimeUnit = coffeeInSelected * ratio / perfectTime;
        for (float i = 0.0f; i <= perfectTime; i++) {
            valuesRegression.add(new Entry(i, i * perTimeUnit));
            espressoDiagram.addReferenceValue(i, i * perTimeUnit);
        }
        reference = DiagrammSettings.getReferenceDataSet(valuesRegression);

        // Set back to default value
        pref.setWeightOfCoffee(0.0f);
    }

    private void updateLineChart(Float weight, Float time) {
        data.getDataSetByIndex(0).addEntry(new Entry(time, weight));
        espressoDiagram.addCoffeeFlowValue(time, weight);

        float maxValue = lineChart.getXAxis().getAxisMaximum();
        if (time > maxValue)
            lineChart.getXAxis().setAxisMaximum(time + 1);

        maxValue = lineChart.getAxisLeft().getAxisMaximum();
        if (weight > maxValue)
            lineChart.getAxisLeft().setAxisMaximum(weight + 1);

        if (lineChart.getData() == null || lineChart.getData().getDataSets().size() == 0)
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
            if (mConnectionService.connectionStatus().equals(BleStatus.CONNECTED.toString())) {
                activateButton();
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver();
        super.onDestroy();
    }

    public void espressoModus(View view) {
        cleanupScreen();
        if (coffeeIn >= 0.5) { // If the value is less it make no sense to show
            ((TextView) findViewById(R.id.espressoActivityCoffeeIn)).setText(String.format("%.2fg", coffeeIn));
        } else if (defaultCoffeeIn >= 0.0) {
            ((TextView) findViewById(R.id.espressoActivityCoffeeIn)).setText(String.format("(~%.2fg)", defaultCoffeeIn));
        } else {
            ((TextView) findViewById(R.id.espressoActivityCoffeeIn)).setText("-");
        }
        mConnectionService.setEspressoModus();
    }

    private void unregisterReceiver() {
        try {
            this.unregisterReceiver(receiver);
            receiver = null;
        } catch (Exception e) {
            // Nothing to do
        }
    }

    private void cleanupScreen() {
        ((TextView) findViewById(R.id.espressoActivityWeight)).setText("");
        ((TextView) findViewById(R.id.espressoActivityTime)).setText("");
        values = new ArrayList<>();
        initChat();
    }

    public void storeResult(View view) {
        AutoCompleteTextView autoCompleteView = findViewById(R.id.coffee);
        UiUtil.storeCoffeeIfNotExists(autoCompleteView, dbUtil);
        selectedCoffee = autoCompleteView.getText().toString();
        pref.setLastSelectedCoffee(selectedCoffee);

        RatingBar ratingView = findViewById(R.id.rating);
        Button storeButton = findViewById(R.id.store);

        ObjectMapper mapper = new ObjectMapper();
        EspressoResultEntity result = new EspressoResultEntity();
        result.setCoffeeOut(this.coffeeOut);
        result.setCoffeeIn(this.coffeeInSelected);
        result.setRatioSet(this.ratio);
        result.setRatio(this.coffeeOut / this.coffeeInSelected);
        result.setDate(new Date().getTime());
        result.setTime(this.time);
        result.setUuid(UUID.randomUUID().toString());
        result.setRating(ratingView.getRating());
        result.setMaxTime(pref.getMaxTime());
        result.setMinTime(pref.getMinTime());
        result.setCoffee(selectedCoffee);
        espressoDiagram.setMaxDiagramX(lineChart.getXAxis().getAxisMaximum());
        espressoDiagram.setMinDiagramX(lineChart.getXAxis().getAxisMinimum());
        espressoDiagram.setMaxDiagramY(lineChart.getAxisLeft().getAxisMaximum());
        espressoDiagram.setMinDiagramY(lineChart.getAxisLeft().getAxisMinimum());

        try {
            result.setChartData(mapper.writeValueAsString(espressoDiagram));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        dbUtil.storeEspressoResult(result);
        storeButton.setEnabled(false);
    }

    public void activateButton() {
        Button btn = findViewById(R.id.espressoModus);
        btn.setEnabled(true);
    }
}