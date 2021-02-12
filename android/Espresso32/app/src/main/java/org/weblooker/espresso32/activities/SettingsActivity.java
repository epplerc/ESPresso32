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

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.utils.PreferencesUtil;
import org.weblooker.espresso32.utils.UiUtil;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private PreferencesUtil preferencesUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        preferencesUtil = new PreferencesUtil(this.getApplicationContext());

        Float value = preferencesUtil.getRatio();
        TextView textView = findViewById(R.id.settingsRatioValue);
        textView.setText(value.toString());
        value = preferencesUtil.getPerfectTime();
        textView = findViewById(R.id.settingsPerfectTimeValue);
        textView.setText(value.toString());
        value = preferencesUtil.getMaxTime();
        textView = findViewById(R.id.settingsmaxTimeValue);
        textView.setText(value.toString());
        value = preferencesUtil.getMinTime();
        textView = findViewById(R.id.settingsminTimeValue);
        textView.setText(value.toString());
        value = preferencesUtil.getDefaultCoffeeIn();
        textView = findViewById(R.id.settingsDefaultCoffeeIn);
        if (value > 0.0)
            textView.setText(value.toString());
        else
            textView.setText("");
    }

    public void setEspressoSettings(View view) {
        TextView textView = findViewById(R.id.settingsRatioValue);
        String ratioString = textView.getText().toString();
        Float ratio;
        ratio = Float.valueOf(ratioString);
        preferencesUtil.setRatio(ratio);

        textView = findViewById(R.id.settingsPerfectTimeValue);
        String timeString = textView.getText().toString();
        Float time;
        time = Float.valueOf(timeString);
        preferencesUtil.setPerfectTime(time);

        textView = findViewById(R.id.settingsminTimeValue);
        timeString = textView.getText().toString();
        time = Float.valueOf(timeString);
        preferencesUtil.setMinTime(time);

        textView = findViewById(R.id.settingsmaxTimeValue);
        timeString = textView.getText().toString();
        time = Float.valueOf(timeString);
        preferencesUtil.setMaxTime(time);

        textView = findViewById(R.id.settingsDefaultCoffeeIn);
        String coffeeInString = textView.getText().toString();
        float coffeeIn = -1.0f;
        try {
            coffeeIn = Float.parseFloat(coffeeInString);
        } catch (Exception e) {
            // Nothing to do
        }
        preferencesUtil.setDefaultCoffeeIn(coffeeIn);
        UiUtil.makeToast(this, getString(R.string.settings_saved));
    }
}