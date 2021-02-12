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

package org.weblooker.espresso32.daos;

import org.weblooker.espresso32.entities.EspressoResultEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface EspressoResultDao {

    @Query("SELECT * FROM EspressoResultEntity ORDER BY  date DESC")
    List<EspressoResultEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEspressoResult(EspressoResultEntity espressoResultEntity);

    @Delete
    void delete(EspressoResultEntity espressoResultEntity);
}
