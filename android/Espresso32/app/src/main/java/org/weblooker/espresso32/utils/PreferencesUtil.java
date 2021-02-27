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

import android.content.Context;
import android.content.SharedPreferences;

import org.weblooker.espresso32.models.Preference;
import org.weblooker.espresso32.models.Settings;

public class PreferencesUtil {

    private final Context context;

    public PreferencesUtil(Context context) {
        this.context = context;
    }

    public String getCalibrationValue() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getString(Preference.CALIPRATION_VALUE.name(), "5000.0");
    }

    public void setCalibrationValue(String value) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Preference.CALIPRATION_VALUE.name(), value);
        editor.apply();
    }

    public float getWeightOfCoffee() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.COFFEE_WEIGHT.name(), 0.0f);
    }

    public void setWeightOfCoffee(float coffeeWeight) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.COFFEE_WEIGHT.name(), coffeeWeight);
        editor.apply();
    }

    public float getDefaultCoffeeIn() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.DEFAULT_COFFEE_WEIGHT.name(), -1.0f);
    }

    public void setDefaultCoffeeIn(float coffeeWeight) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.DEFAULT_COFFEE_WEIGHT.name(), coffeeWeight);
        editor.apply();
    }

    public float getRatio() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.RATIO.name(), 2.0f);
    }

    public void setRatio(float ratio) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.RATIO.name(), ratio);
        editor.apply();
    }

    public float getPerfectTime() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.PERFECT_TIME.name(), 25.0f);
    }

    public void setPerfectTime(float time) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.PERFECT_TIME.name(), time);
        editor.apply();
    }

    public float getMinTime() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.MIN_TIME.name(), 22.0f);
    }

    public void setMinTime(float time) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.MIN_TIME.name(), time);
        editor.apply();
    }

    public float getMaxTime() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getFloat(Preference.MAX_TIME.name(), 30.0f);
    }

    public void setMaxTime(float time) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Preference.MAX_TIME.name(), time);
        editor.apply();
    }

    public String getLastSelectedCoffee() {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        return sharedPref.getString(Preference.LAST_SELECTED_COFFEE.name(), "");
    }

    public void setLastSelectedCoffee(String name) {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Preference.LAST_SELECTED_COFFEE.name(), name);
        editor.apply();
    }

    public Settings exportSettings() {
        return new Settings()
                .setCalibrationValue(getCalibrationValue())
                .setDefaultCoffeeIn(getDefaultCoffeeIn())
                .setRatio(getRatio())
                .setPerfectTime(getPerfectTime())
                .setMaxTime(getMaxTime())
                .setMinTime(getMinTime());
    }

    public void importSettings(Settings settings) {
        setCalibrationValue(settings.getCalibrationValue());
        setDefaultCoffeeIn(settings.getDefaultCoffeeIn());
        setRatio(settings.getRatio());
        setPerfectTime(settings.getPerfectTime());
        setMaxTime(settings.getMaxTime());
        setMinTime(settings.getMinTime());
    }

}
