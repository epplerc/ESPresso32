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
import android.view.MenuItem;
import android.view.WindowManager;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.adapter.TimelineRowAdapter;
import org.weblooker.espresso32.models.UiMenuEntries;
import org.weblooker.espresso32.utils.DbUtil;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TimelineActivity extends AppCompatActivity {

    private TimelineRowAdapter adapter;
    private DbUtil dbUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dbUtil = new DbUtil(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimelineRowAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        dbUtil.getAllEspressoResults().whenComplete((list, err) ->
                adapter.setEspressoResultEntities(list));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == UiMenuEntries.Delete.toString()) {
            int itemId = item.getItemId();
            adapter.deleteElement(itemId);
        } else if (item.getTitle() == UiMenuEntries.Edit.toString()) {
            int itemId = item.getItemId();
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            adapter.editElement(itemId, (TimelineRowAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(itemId));
        } else {
            return false;
        }
        return true;
    }
}