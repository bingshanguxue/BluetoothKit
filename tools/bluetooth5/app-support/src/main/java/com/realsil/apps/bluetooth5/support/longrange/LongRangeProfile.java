package com.realsil.apps.bluetooth5.support.longrange;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class LongRangeProfile {
    /* Current Time Service UUID */
    public static UUID DATA_TRANSMIT_SERVICE = UUID.fromString("000002fd-3C17-D293-8E48-14FE2E4DA212");/*0000ABCD-0000-1000-8000-00805f9b34fb*/
    /* Mandatory Current Time Information Characteristic */
    public static UUID TX_CHARACTERISTIC = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    /* Optional Local Time Information Characteristic */
    public static UUID RX_CHARACTERISTIC = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");
    /* Mandatory Client Characteristic Config Descriptor */
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Current Time Service.
     */
    public static BluetoothGattService createTimeService() {
        BluetoothGattService service = new BluetoothGattService(DATA_TRANSMIT_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic currentTime = new BluetoothGattCharacteristic(TX_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        currentTime.addDescriptor(configDescriptor);

        // Local Time Information characteristic
        BluetoothGattCharacteristic localTime = new BluetoothGattCharacteristic(RX_CHARACTERISTIC,
                //Read-only characteristic
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(currentTime);
        service.addCharacteristic(localTime);

        return service;
    }

}
