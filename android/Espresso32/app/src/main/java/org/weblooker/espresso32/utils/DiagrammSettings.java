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

package org.weblooker.espresso32.utils;

import android.graphics.Color;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.entities.EspressoResultEntity;
import org.weblooker.espresso32.models.EspressoDiagram;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class DiagrammSettings {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static LineChart setConfiguration(LineChart lineChart, PreferencesUtil pref) {
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        Description desc = new Description();
        desc.setText("Coffee flow");
        desc.setYOffset(-7);
        lineChart.setDescription(desc);
        lineChart.setNoDataText("");
        lineChart.setNoDataTextColor(Color.BLACK);
        lineChart.setMaxVisibleValueCount(20);

        LimitLine limitLineH = new LimitLine(pref.getMaxTime(), "Max");
        limitLineH.setLineColor(Color.BLACK);
        LimitLine limitLineL = new LimitLine(pref.getMinTime(), "Min");
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
        return lineChart;
    }

    public static LineChart setConfiguration(LineChart lineChart, EspressoResultEntity espressoResultEntity) {
        EspressoDiagram espressoDiagram;
        try {
            espressoDiagram = OBJECT_MAPPER.readValue(espressoResultEntity.getChartData(), EspressoDiagram.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
        lineChart.setData(espressoDiagram.toLineData());

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        Description desc = new Description();
        desc.setText("Coffee flow");
        desc.setYOffset(-7);
        lineChart.setDescription(desc);
        lineChart.setNoDataText("");
        lineChart.setNoDataTextColor(Color.BLACK);
        lineChart.setMaxVisibleValueCount(20);

        LimitLine limitLineH = new LimitLine(espressoResultEntity.getMaxTime(), "Max");
        limitLineH.setLineColor(Color.BLACK);
        LimitLine limitLineL = new LimitLine(espressoResultEntity.getMinTime(), "Min");
        limitLineL.setLineColor(Color.BLACK);
        lineChart.getXAxis().addLimitLine(limitLineL);
        lineChart.getXAxis().addLimitLine(limitLineH);
        lineChart.getXAxis().setAxisMinimum(espressoDiagram.getMinDiagramX());
        lineChart.getXAxis().setAxisMaximum(espressoDiagram.getMaxDiagramX());
        lineChart.getXAxis().setLabelCount(8);
        lineChart.getAxisLeft().setAxisMinimum(espressoDiagram.getMinDiagramY());
        lineChart.getAxisLeft().setAxisMaximum(espressoDiagram.getMaxDiagramY());

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
        return lineChart;
    }

    public static LineDataSet getCoffeeFlowDataSet(ArrayList<Entry> values) {
        return new LineDataSet(values, "Coffee in g");
    }

    public static LineDataSet getReferenceDataSet(ArrayList<Entry> values) {
        LineDataSet reference = new LineDataSet(values, "Reference");
        reference.setDrawCircles(false);
        reference.setColor(R.color.darkgray);
        return reference;
    }

}
