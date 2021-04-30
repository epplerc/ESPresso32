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

package org.weblooker.espresso32.adapter;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;

import org.weblooker.espresso32.R;
import org.weblooker.espresso32.entities.EspressoResultEntity;
import org.weblooker.espresso32.models.EspressoDiagram;
import org.weblooker.espresso32.models.UiMenuEntries;
import org.weblooker.espresso32.utils.DbUtil;
import org.weblooker.espresso32.utils.DiagrammSettings;
import org.weblooker.espresso32.utils.UiUtil;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;

public class TimelineRowAdapter extends RecyclerView.Adapter<TimelineRowAdapter.ViewHolder> {

    private List<EspressoResultEntity> espressoResultEntities;
    private final LayoutInflater layoutInflater;
    private final ObjectMapper objectMapper;
    private final DbUtil dbUtil;

    public TimelineRowAdapter(Context context, List<EspressoResultEntity> results) {
        this.layoutInflater = LayoutInflater.from(context);
        this.espressoResultEntities = results;
        this.objectMapper = new ObjectMapper();
        this.dbUtil = new DbUtil(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.fragment_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        EspressoResultEntity result = espressoResultEntities.get(position);
        holder.coffeeIn.setText(String.format("%.2f g", result.getCoffeeIn()));
        holder.coffeeInEdit.setText(result.getCoffeeIn().toString());
        holder.coffeeOut.setText(result.getCoffeeOut().toString() + "g");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        holder.date.setText(simpleDateFormat.format(new Date(result.getDate())));
        holder.ratio.setText(String.format(Locale.forLanguageTag("EN"), "%.2f", result.getRatio()));
        holder.time.setText(result.getTime().toString() + "s");
        holder.uuid.setText(result.getUuid());
        holder.ratingBar.setRating(result.getRating());
        holder.position.setText(String.valueOf(position));
        holder.coffee.setText(result.getCoffee());
        holder.coffeeEdit.setText(result.getCoffee(), false);
        holder.coffeeEdit.setSelection(result.getCoffee().length());


        DiagrammSettings.setConfiguration(holder.lineChart, result);
        try {
            LineData lineData = objectMapper.readValue(result.getChartData(), EspressoDiagram.class).toLineData();
            holder.lineChart.setData(lineData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        holder.lineChart.notifyDataSetChanged();
        holder.lineChart.invalidate();
    }

    @Override
    public int getItemCount() {
        if (espressoResultEntities == null)
            return 0;
        return espressoResultEntities.size();
    }

    public void setEspressoResultEntities(List<EspressoResultEntity> results) {
        this.espressoResultEntities = results;
    }

    public void deleteElement(int index) {
        EspressoResultEntity espressoResultEntity = espressoResultEntities.get(index);
        dbUtil.deleteEspressoResult(espressoResultEntity);
        espressoResultEntities.remove(index);
        notifyItemRemoved(index);
        notifyItemRangeChanged(index, espressoResultEntities.size());
    }

    public void editElement(int index, ViewHolder viewHolder) {
        viewHolder.store.setVisibility(View.VISIBLE);
        viewHolder.coffee.setVisibility(View.GONE);
        viewHolder.coffeeEdit.setVisibility(View.VISIBLE);
        viewHolder.coffeeIn.setVisibility(View.GONE);
        viewHolder.coffeeInEdit.setVisibility(View.VISIBLE);

        viewHolder.ratingBar.setIsIndicator(false);
        viewHolder.store.setOnClickListener((view) -> {
            viewHolder.store.setVisibility(View.GONE);
            viewHolder.ratingBar.setIsIndicator(true);
            viewHolder.coffeeEdit.setVisibility(View.GONE);
            viewHolder.coffeeEdit.clearFocus();
            viewHolder.coffee.setVisibility(View.VISIBLE);
            viewHolder.coffeeInEdit.setVisibility(View.GONE);
            viewHolder.coffeeIn.setVisibility(View.VISIBLE);
            UiUtil.storeCoffeeIfNotExists(viewHolder.coffeeEdit, dbUtil);

            EspressoResultEntity espressoResultEntity = espressoResultEntities.get(index);
            espressoResultEntity.setRating(viewHolder.ratingBar.getRating());
            espressoResultEntity.setCoffee(viewHolder.coffeeEdit.getText().toString());
            espressoResultEntity.setCoffeeIn(Float.valueOf(viewHolder.coffeeInEdit.getText().toString()));
            dbUtil.storeEspressoResult(espressoResultEntity);
            espressoResultEntities.set(index, espressoResultEntity);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, espressoResultEntities.size());
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        protected final TextView date;
        protected final TextView coffeeIn;
        protected final EditText coffeeInEdit;
        protected final TextView coffeeOut;
        protected final TextView time;
        protected final TextView ratio;
        protected final LineChart lineChart;
        protected final TextView uuid;
        protected final TextView position;
        protected final RatingBar ratingBar;
        protected final AutoCompleteTextView coffeeEdit;
        protected final TextView coffee;
        protected final Button store;

        ViewHolder(View view) {
            super(view);
            view.setOnCreateContextMenuListener(this);
            date = view.findViewById(R.id.espressoResultRowDate);
            coffeeIn = view.findViewById(R.id.espressoResultCoffeeIn);
            coffeeInEdit = view.findViewById(R.id.espressoResultCoffeeInEdit);
            coffeeOut = view.findViewById(R.id.espressoResultCoffeeOut);
            time = view.findViewById(R.id.espressoResultTime);
            ratio = view.findViewById(R.id.espressoResultRatio);
            lineChart = view.findViewById(R.id.flowChart);
            uuid = view.findViewById(R.id.uuid);
            ratingBar = view.findViewById(R.id.rating);
            position = view.findViewById(R.id.position);
            coffee = view.findViewById(R.id.coffee);
            coffeeEdit = view.findViewById(R.id.coffeeEdit);
            UiUtil.setupAutoCompleteTextViewFoTimeline(coffeeEdit, view, dbUtil);
            store = view.findViewById(R.id.store);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Action");
            TextView pos = v.findViewById(R.id.position);
            menu.add(0, Integer.parseInt(pos.getText().toString()), 0, UiMenuEntries.Delete.toString());
            menu.add(0, Integer.parseInt(pos.getText().toString()), 1, UiMenuEntries.Edit.toString());
        }

    }

    public void loadAllResultsAsynchronously()
    {
        dbUtil.getAllEspressoResults().whenCompleteAsync((list, err) -> {
            this.setEspressoResultEntities(list);
            this.notifyDataSetChanged();
        });
    }
}
