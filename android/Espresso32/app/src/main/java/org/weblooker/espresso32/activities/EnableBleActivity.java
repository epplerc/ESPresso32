package org.weblooker.espresso32.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.weblooker.espresso32.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EnableBleActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 4242;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_ble);
    }

    public void enableBLE(View view)
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            Intent myIntent = new Intent(EnableBleActivity.this, MainActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            EnableBleActivity.this.startActivity(myIntent);
        }
    }
}