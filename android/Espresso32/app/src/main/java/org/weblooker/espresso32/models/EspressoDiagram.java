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

package org.weblooker.espresso32.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.weblooker.espresso32.utils.DiagrammSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EspressoDiagram {

    private List<DiagramLine> coffeeFlow = new ArrayList<>();
    private List<DiagramLine> reference = new ArrayList<>();
    private float maxDiagramX;
    private float minDiagramX;
    private float maxDiagramY;
    private float minDiagramY;


    public List<DiagramLine> getCoffeeFlow() {
        return coffeeFlow;
    }

    public void setCoffeeFlow(List<DiagramLine> coffeeFlow) {
        this.coffeeFlow = coffeeFlow;
    }

    public List<DiagramLine> getReference() {
        return reference;
    }

    public void setReference(List<DiagramLine> reference) {
        this.reference = reference;
    }

    public void addReferenceValue(float x, float y) {
        reference.add(new DiagramLine(x, y));
    }

    public void addCoffeeFlowValue(float x, float y) {
        coffeeFlow.add(new DiagramLine(x, y));
    }

    public float getMaxDiagramX() {
        return maxDiagramX;
    }

    public void setMaxDiagramX(float maxDiagramX) {
        this.maxDiagramX = maxDiagramX;
    }

    public float getMinDiagramX() {
        return minDiagramX;
    }

    public void setMinDiagramX(float minDiagramX) {
        this.minDiagramX = minDiagramX;
    }

    public float getMaxDiagramY() {
        return maxDiagramY;
    }

    public void setMaxDiagramY(float maxDiagramY) {
        this.maxDiagramY = maxDiagramY;
    }

    public float getMinDiagramY() {
        return minDiagramY;
    }

    public void setMinDiagramY(float minDiagramY) {
        this.minDiagramY = minDiagramY;
    }

    @JsonIgnore
    public LineData toLineData() {
        ArrayList<Entry> referenceEntries = reference
                .stream()
                .map(el -> new Entry(el.getX(), el.getY()))
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Entry> coffeeFlowEntries = coffeeFlow
                .stream()
                .map(el -> new Entry(el.getX(), el.getY()))
                .collect(Collectors.toCollection(ArrayList::new));

        LineDataSet referenceDataSet = DiagrammSettings.getReferenceDataSet(referenceEntries);
        LineDataSet coffeeFlowDataSet = DiagrammSettings.getCoffeeFlowDataSet(coffeeFlowEntries);

        return new LineData(Arrays.asList(coffeeFlowDataSet, referenceDataSet));
    }
}

