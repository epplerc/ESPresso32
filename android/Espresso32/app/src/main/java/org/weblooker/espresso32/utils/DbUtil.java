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

import org.weblooker.espresso32.daos.CoffeeDao;
import org.weblooker.espresso32.daos.EspressoResultDao;
import org.weblooker.espresso32.entities.CoffeeEntity;
import org.weblooker.espresso32.entities.EspressoResultEntity;
import org.weblooker.espresso32.services.EspressoDatabase;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import androidx.room.Room;

public class DbUtil {

    private static final String DB = "espresso";
    private static EspressoDatabase espressoDatabase = null;

    private final EspressoResultDao espressoResultDao;
    private final CoffeeDao coffeeDao;


    public DbUtil(Context context) {

        if (espressoDatabase == null) {
            espressoDatabase = Room.databaseBuilder(context, EspressoDatabase.class, DB).build();
        }

        this.espressoResultDao = espressoDatabase.espressoResultDao();
        this.coffeeDao = espressoDatabase.CoffeeDao();
    }


    public CompletableFuture<List<EspressoResultEntity>> getAllEspressoResults() {
        return CompletableFuture.supplyAsync(espressoResultDao::getAll);
    }

    public CompletableFuture<Void> storeEspressoResult(EspressoResultEntity espressoResultEntity) {
        return CompletableFuture.runAsync(() -> espressoResultDao.insertEspressoResult(espressoResultEntity));

    }

    public CompletableFuture<Void> deleteEspressoResult(EspressoResultEntity espressoResultEntity) {
        return CompletableFuture.runAsync(() -> espressoResultDao.delete(espressoResultEntity));

    }

    public CompletableFuture<List<CoffeeEntity>> getAllCoffee() {
        return CompletableFuture.supplyAsync(coffeeDao::getAll);
    }

    public CompletableFuture<Void> storeCoffee(CoffeeEntity coffeeEntity) {
        return CompletableFuture.runAsync(() -> coffeeDao.insertCoffee(coffeeEntity));

    }

}
