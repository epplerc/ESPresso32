# ESPresso32

The goal of this project is to create a espresso scale based on a ESP32 with
a coresponding Android App and BLE connection.

The ESP32 is programmed with the Andrino IDE.

## ESP32

### Software
You need the Arduino IDE to flash the software.
Simple flash the espresso32.ino file this is all.

Now you can install the Android app or use direct BLE
to communicate with the scale.

### Hardware setup
Beside of the software i have use a simple hardware setup which should only be a example without warranty.

#### List of components
Below you see a list of compnent i have used.
The advantage of the lifepo4 accumulator are that they deliver a voltage from 
max 3.6V which is the max voltage the ESP32 can be run direct on the 3.3V input.
So i was able to omitting one voltage converter.

- ESP32
- Load cell
- HX711
- Battery holder
- lifepo4 accumulator
- Step up (MT3608)
- Housing
- Other small parts like wires etc

This is only a  simple example there for sure other setups possible.

####Schematic
![Schematic](images/schematic.jpg)

## Android
To use the android application you need a phone with **Bluetooth Low Energy**.

You can install the app from the [Playstore](https://github.com/epplerc/ESPresso32) or build the app from the source.


If you want to build your application from the source you can do this.
The code is located in the folder  **[android](android)**.
