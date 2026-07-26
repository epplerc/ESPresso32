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
import android.bluetooth.BluetoothGattDescriptor;
import org.weblooker.espresso32.models.BleJob;
import java.util.LinkedList;
import java.util.Queue;

public class BleCommands {

    private final Queue<BleJob> commandQueue = new LinkedList<>();

    public void addTarCommandToQueue(BluetoothGattCharacteristic characteristic) {
        BleJob job = new BleJob();
        job.setType("write");
        job.setCharacteristic(characteristic.getUuid().toString());
        job.setValue("tare");
        commandQueue.add(job);
    }

    public void addWriteValueToCommandToQueue(BluetoothGattCharacteristic characteristic, String value) {
        BleJob job = new BleJob();
        job.setType("write");
        job.setCharacteristic(characteristic.getUuid().toString());
        job.setValue(value);
        commandQueue.add(job);
    }

    public void addReadValueToCommandToQueue(BluetoothGattCharacteristic characteristic) {
        BleJob job = new BleJob();
        job.setType("read");
        job.setCharacteristic(characteristic.getUuid().toString());
        commandQueue.add(job);
    }

    public void addDescriptorWriteToQueue(BluetoothGattCharacteristic characteristic, String descriptorUuid, byte[] data) {
        BleJob job = new BleJob();
        job.setType("write_descriptor");
        job.setCharacteristic(characteristic.getUuid().toString());
        job.setDescriptor(descriptorUuid);
        job.setData(data);
        commandQueue.add(job);
    }

    public BleJob getNextEntry() {
        return commandQueue.poll();
    }

    public boolean commandQueueHasEntries() {
        return !commandQueue.isEmpty();
    }

    public void clearCommandQueue() {
        commandQueue.clear();
    }
}
