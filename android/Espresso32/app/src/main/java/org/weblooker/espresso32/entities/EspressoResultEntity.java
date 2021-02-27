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

package org.weblooker.espresso32.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EspressoResultEntity {

    @PrimaryKey
    @NonNull
    private String uuid;

    @ColumnInfo(name = "date")
    private long date;

    @ColumnInfo(name = "coffee_in")
    private Float coffeeIn;

    @ColumnInfo(name = "coffee_out")
    private Float coffeeOut;

    @ColumnInfo(name = "ratio")
    private Float ratio;

    @ColumnInfo(name = "ratio_set")
    private Float ratioSet;

    @ColumnInfo(name = "time")
    private Float time;

    @ColumnInfo(name = "perfect_time")
    private Float perfectTime;

    // Need to be stored into json string
    @ColumnInfo(name = "chart_data")
    private String chartData;

    @ColumnInfo(name = "min_time")
    private Float minTime;

    @ColumnInfo(name = "max_time")
    private Float maxTime;

    @ColumnInfo(name = "coffee")
    private String coffee;

    @ColumnInfo(name = "ratingbar")
    private float rating;

    @ColumnInfo(name = "notes")
    private String notes;


    @Override
    public String toString() {
        return "EspressoResultEntity{" +
                "uid=" + uuid +
                ", timestamp=" + date +
                ", coffeeIn=" + coffeeIn +
                ", coffee_out=" + coffeeOut +
                ", ratio=" + ratio +
                ", ratioSet=" + ratioSet +
                ", time=" + time +
                ", perfectTime=" + perfectTime +
                ", chartData='" + chartData + '\'' +
                ", minTime=" + minTime +
                ", maxTime=" + maxTime +
                ", coffee=" + coffee +
                ", ratingbar=" + rating +
                '}';
    }

    @NonNull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@NonNull String uuid) {
        this.uuid = uuid;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Float getCoffeeIn() {
        return coffeeIn;
    }

    public void setCoffeeIn(Float coffeeIn) {
        this.coffeeIn = coffeeIn;
    }

    public Float getCoffeeOut() {
        return coffeeOut;
    }

    public void setCoffeeOut(Float coffeeOut) {
        this.coffeeOut = coffeeOut;
    }

    public Float getRatio() {
        return ratio;
    }

    public void setRatio(Float ratio) {
        this.ratio = Math.abs(ratio);
    }

    public Float getRatioSet() {
        return ratioSet;
    }

    public void setRatioSet(Float ratioSet) {
        this.ratioSet = ratioSet;
    }

    public Float getTime() {
        return time;
    }

    public void setTime(Float time) {
        this.time = time;
    }

    public Float getPerfectTime() {
        return perfectTime;
    }

    public void setPerfectTime(Float perfectTime) {
        this.perfectTime = perfectTime;
    }

    public String getChartData() {
        return chartData;
    }

    public void setChartData(String chartData) {
        this.chartData = chartData;
    }

    public Float getMinTime() {
        return minTime;
    }

    public void setMinTime(Float minTime) {
        this.minTime = minTime;
    }

    public Float getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Float maxTime) {
        this.maxTime = maxTime;
    }

    public String getCoffee() {
        return coffee;
    }

    public void setCoffee(String coffee) {
        this.coffee = coffee;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
