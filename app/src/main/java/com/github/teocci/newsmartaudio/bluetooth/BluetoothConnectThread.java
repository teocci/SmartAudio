package com.github.teocci.newsmartaudio.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.github.teocci.newsmartaudio.utils.LogHelper;

import java.util.UUID;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */

public class BluetoothConnectThread extends Thread
{
    private final String TAG = LogHelper.makeLogTag(BluetoothConnectThread.class);

    // Serial connection
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket bluetoothSocket;
    private final BluetoothService bluetoothService;

    public BluetoothConnectThread(BluetoothDevice device, BluetoothService bluetoothService)
    {
        this.bluetoothService = bluetoothService;
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    public void run()
    {
        if (bluetoothService == null) return;

        bluetoothService.getBluetoothAdapter().cancelDiscovery();
        try {
            bluetoothSocket.connect();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
            bluetoothService.connectionFailed();
            cancel();
            bluetoothService.start();
            return;
        }

        synchronized (bluetoothService) {
            bluetoothService.setBluetoothConnectThread(null);
        }
        bluetoothService.connected(bluetoothSocket);
    }

    public void cancel()
    {
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }
}

