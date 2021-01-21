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
