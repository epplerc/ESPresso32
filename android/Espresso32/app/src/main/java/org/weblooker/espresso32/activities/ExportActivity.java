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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.entities.CoffeeEntity;
import org.weblooker.espresso32.entities.EspressoResultEntity;
import org.weblooker.espresso32.models.Settings;
import org.weblooker.espresso32.utils.DbUtil;
import org.weblooker.espresso32.utils.PreferencesUtil;
import org.weblooker.espresso32.utils.UiUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ExportActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 4343;
    private static final int REQUEST_CODE_WRITE = 4444;
    private static final int REQUEST_CODE_READ = 4545;
    private static final String DEFAULT_FILE_NAME = "espresso32.zip";
    private static final String APPLICATION_ZIP = "application/zip";
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_OBJECT_WRITER = DEFAULT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    private PreferencesUtil pref;
    private DbUtil dbUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pref = new PreferencesUtil(this.getApplicationContext());
        dbUtil = new DbUtil(this);
        setContentView(R.layout.activity_export);

        checkForPermissions();
    }

    public void exportData(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imExportIntent(intent,REQUEST_CODE_WRITE);
    }

    public void importData(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        imExportIntent(intent,REQUEST_CODE_READ);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE && resultCode == Activity.RESULT_OK) {
            try {
                writeDataToFile(data.getData());
                UiUtil.makeToast(this, getString(R.string.export_message));
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                UiUtil.makeToast(this, getString(R.string.export_message_fail));
            }
        }
        if (requestCode == REQUEST_CODE_READ && resultCode == Activity.RESULT_OK) {
            try {
                writeDataIntoDB(data.getData());
                UiUtil.makeToast(this, getString(R.string.import_message));
            } catch (IOException | RuntimeException e) {
                UiUtil.makeToast(this, getString(R.string.import_message_fail));
            }
        }
    }

    private void checkForPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    private void imExportIntent(Intent intent,int requestCode )
    {
        intent.setType(APPLICATION_ZIP);
        intent.putExtra(Intent.EXTRA_TITLE, DEFAULT_FILE_NAME);
        startActivityForResult(intent, requestCode);
    }

    private void writeDataToFile(Uri uri) throws IOException {

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            dbUtil.getAllEspressoResults().whenComplete((el, ex) -> {
                ZipEntry timeline = new ZipEntry("timeline.json");
                try {
                    zipOutputStream.putNextEntry(timeline);
                    byte[] bytes = PRETTY_OBJECT_WRITER.writeValueAsBytes(el);
                    zipOutputStream.write(bytes);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e.getCause());
                }

            }).join();

            dbUtil.getAllCoffee().whenComplete((el, ex) -> {
                ZipEntry coffee = new ZipEntry("coffee.json");
                try {
                    zipOutputStream.putNextEntry(coffee);
                    byte[] bytes = PRETTY_OBJECT_WRITER.writeValueAsBytes(el);
                    zipOutputStream.write(bytes);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e.getCause());
                }
            }).join();

            Settings settings = pref.exportSettings();
            ZipEntry settingsZipEntry = new ZipEntry("settings.json");
            zipOutputStream.putNextEntry(settingsZipEntry);
            byte[] bytes = PRETTY_OBJECT_WRITER.writeValueAsBytes(settings);
            zipOutputStream.write(bytes);
            zipOutputStream.closeEntry();

            zipOutputStream.finish();
            zipOutputStream.close();
            zipOutputStream.flush();

            OutputStream output = getContentResolver().openOutputStream(uri);
            output.write(byteArrayOutputStream.toByteArray());
        }
    }

    private void writeDataIntoDB(Uri uri) throws IOException {

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ZipInputStream zipStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                String filename = entry.getName();
                StringBuilder input = readStreamToString(zipStream);

                if ("coffee.json".equals(filename)) {
                    ObjectReader objectReader = DEFAULT_OBJECT_MAPPER.reader().forType(new TypeReference<List<CoffeeEntity>>() {
                    });
                    List<CoffeeEntity> elements = objectReader.readValue(input.toString());
                    elements.forEach(el -> dbUtil.storeCoffee(el));
                } else if ("timeline.json".equals(filename)) {
                    ObjectReader objectReader = DEFAULT_OBJECT_MAPPER.reader().forType(new TypeReference<List<EspressoResultEntity>>() {
                    });
                    List<EspressoResultEntity> elements = objectReader.readValue(input.toString());
                    elements.forEach(el -> dbUtil.storeEspressoResult(el));
                } else if ("settings.json".equals(filename)) {
                    ObjectReader objectReader = DEFAULT_OBJECT_MAPPER.reader().forType(Settings.class);
                    Settings settings = objectReader.readValue(input.toString());
                    pref.importSettings(settings);
                }
            }
        }
    }

    private StringBuilder readStreamToString(ZipInputStream zipStream) throws IOException {
        StringBuilder input = new StringBuilder();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = zipStream.read(buffer, 0, bufferSize)) >= 0) {
            input.append(new String(buffer, 0, read));
        }
        return input;
    }
}
