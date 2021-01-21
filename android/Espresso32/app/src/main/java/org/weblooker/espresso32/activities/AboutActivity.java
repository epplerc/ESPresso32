package org.weblooker.espresso32.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import org.weblooker.espresso32.R;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }
    public void showOSN(View view) {
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
    }
}