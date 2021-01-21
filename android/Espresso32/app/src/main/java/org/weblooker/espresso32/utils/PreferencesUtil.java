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

    public void setCalibrationValue(String value)
    {
        SharedPreferences sharedPref = context
                .getSharedPreferences("org.weblooker.espresso32.PREFERENCE_MAIN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Preference.CALIPRATION_VALUE.name(), value);
        editor.apply();
    }
}
