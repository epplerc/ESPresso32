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

package org.weblooker.espresso32.services;

import org.weblooker.espresso32.daos.CoffeeDao;
import org.weblooker.espresso32.daos.EspressoResultDao;
import org.weblooker.espresso32.entities.CoffeeEntity;
import org.weblooker.espresso32.entities.EspressoResultEntity;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EspressoResultEntity.class, CoffeeEntity.class}, version = 1)
public abstract class EspressoDatabase extends RoomDatabase {
    public abstract EspressoResultDao espressoResultDao();

    public abstract CoffeeDao CoffeeDao();
}
