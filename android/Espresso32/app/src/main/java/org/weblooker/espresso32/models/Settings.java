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

public class Settings {
    private String calibrationValue;
    private float defaultCoffeeIn;
    private float ratio;
    private float perfectTime;
    private float maxTime;
    private float minTime;

    public Settings() {
    }

    public String getCalibrationValue() {
        return calibrationValue;
    }

    public Settings setCalibrationValue(String calibrationValue) {
        this.calibrationValue = calibrationValue;
        return this;
    }

    public float getDefaultCoffeeIn() {
        return defaultCoffeeIn;
    }

    public Settings setDefaultCoffeeIn(float defaultCoffeeIn) {
        this.defaultCoffeeIn = defaultCoffeeIn;
        return this;
    }

    public float getRatio() {
        return ratio;
    }

    public Settings setRatio(float ratio) {
        this.ratio = ratio;
        return this;
    }

    public float getPerfectTime() {
        return perfectTime;
    }

    public Settings setPerfectTime(float perfectTime) {
        this.perfectTime = perfectTime;
        return this;
    }

    public float getMaxTime() {
        return maxTime;
    }

    public Settings setMaxTime(float maxTime) {
        this.maxTime = maxTime;
        return this;
    }

    public float getMinTime() {
        return minTime;
    }

    public Settings setMinTime(float minTime) {
        this.minTime = minTime;
        return this;
    }
}
