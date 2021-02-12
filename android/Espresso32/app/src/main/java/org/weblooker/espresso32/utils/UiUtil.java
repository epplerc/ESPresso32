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
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import org.weblooker.espresso32.entities.CoffeeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UiUtil {

    public static void setupAutoCompleteTextViewFoTimeline(final AutoCompleteTextView autoCompleteTextView, View view, DbUtil dbUtil) {
        List<String> coffeeList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>
                (view.getContext(), android.R.layout.select_dialog_item, coffeeList);

        autoCompleteTextView.clearFocus();
        autoCompleteTextView.setThreshold(0);
        autoCompleteTextView.setTextColor(Color.BLACK);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener((parent, view1, position, id) -> {
            InputMethodManager inputMethodManager = (InputMethodManager) view1.getContext().getSystemService(view1.getContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view1.getApplicationWindowToken(), 0);
        });
        autoCompleteTextView.setOnEditorActionListener((v, action, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                    (action == EditorInfo.IME_ACTION_DONE)) {
                // Nothing to do
            }
            return false;
        });

        dbUtil.getAllCoffee().whenComplete((el, ex) -> {
            List<String> collect = el.stream().map(CoffeeEntity::getName).collect(Collectors.toList());
            coffeeList.addAll(collect);
        });
    }

    public static void setupAutoCompleteTextView(final AutoCompleteTextView autoCompleteTextView, DbUtil dbUtil, PreferencesUtil pref) {
        List<String> coffeeList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>
                (autoCompleteTextView.getContext(), android.R.layout.select_dialog_item, coffeeList);

        autoCompleteTextView.clearFocus();
        autoCompleteTextView.setThreshold(0);
        autoCompleteTextView.setTextColor(Color.BLACK);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setText(pref.getLastSelectedCoffee(), false);
        autoCompleteTextView.setSelection(pref.getLastSelectedCoffee().length());

        autoCompleteTextView.setOnClickListener((view2) -> autoCompleteTextView.setCursorVisible(true));
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            storeCoffeeIfNotExists(autoCompleteTextView, dbUtil);
            pref.setLastSelectedCoffee(autoCompleteTextView.getText().toString());
            autoCompleteTextView.setCursorVisible(false);

            InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(view.getContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        });
        autoCompleteTextView.setOnEditorActionListener((v, action, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                    (action == EditorInfo.IME_ACTION_DONE)) {
                autoCompleteTextView.setCursorVisible(false);
            }
            return false;
        });

        dbUtil.getAllCoffee().whenComplete((el, ex) -> {
            List<String> collect = el.stream().map(CoffeeEntity::getName).collect(Collectors.toList());
            coffeeList.addAll(collect);
        });
    }

    public static void storeCoffeeIfNotExists(AutoCompleteTextView view, DbUtil dbUtil) {
        dbUtil.getAllCoffee().whenComplete((el, ex) -> {
            String coffeeName = view.getText().toString();
            List<CoffeeEntity> collect = el.stream()
                    .filter(el2 -> el2.getName().equals(coffeeName))
                    .collect(Collectors.toList());
            if (collect.size() == 0) {
                CoffeeEntity coffeeEntity = new CoffeeEntity();
                coffeeEntity.setName(coffeeName);
                dbUtil.storeCoffee(coffeeEntity);
            }
        });
    }

    public static void makeToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
        toast.show();
    }
}
