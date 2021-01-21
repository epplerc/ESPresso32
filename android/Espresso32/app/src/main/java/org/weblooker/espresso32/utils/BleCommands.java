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
        return (BleJob) writeQueue.poll();
    }

    public boolean commandQueueHasEntries() {
        return writeQueue.size() > 0;
    }

    public void clearCommandQueue() {
        writeQueue.clear();
    }

}
