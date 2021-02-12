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

import android.bluetooth.BluetoothGattCharacteristic;

import org.weblooker.espresso32.models.BleJob;

import java.util.LinkedList;
import java.util.Queue;

public class BleCommands {

    private final Queue<BleJob> writeQueue = new LinkedList<>();

    public void addTarCommandToQueue(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        BleJob bleJob = new BleJob();
        bleJob.setType("write");
        bleJob.setCharacteristic(bluetoothGattCharacteristic.getUuid().toString());
        bleJob.setValue("tare");
        writeQueue.add(bleJob);
    }

    public void addWriteValueToCommandToQueue(BluetoothGattCharacteristic bluetoothGattCharacteristic, String value) {
        BleJob bleJob = new BleJob();
        bleJob.setType("write");
        bleJob.setCharacteristic(bluetoothGattCharacteristic.getUuid().toString());
        bleJob.setValue(value);
        writeQueue.add(bleJob);
    }

    public void addReadValueToCommandToQueue(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        BleJob bleJob = new BleJob();
        bleJob.setType("read");
        bleJob.setCharacteristic(bluetoothGattCharacteristic.getUuid().toString());
        writeQueue.add(bleJob);
    }

    public BleJob getNextEntry() {
        return writeQueue.poll();
    }

    public boolean commandQueueHasEntries() {
        return writeQueue.size() > 0;
    }

    public void clearCommandQueue() {
        writeQueue.clear();
    }

}
