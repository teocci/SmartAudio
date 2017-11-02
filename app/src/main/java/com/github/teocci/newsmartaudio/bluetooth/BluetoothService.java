package com.github.teocci.newsmartaudio.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import com.github.teocci.newsmartaudio.ui.BluetoothListActivity;
import com.github.teocci.newsmartaudio.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */
public class BluetoothService
{
    private final String TAG = LogHelper.makeLogTag(BluetoothService.class);

    private Activity activity;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothConnectThread connectThread;
    private BluetoothClientThread clientThread;
    private int state;

    protected static final int STATE_NONE = 0;
    protected static final int STATE_LISTEN = 1;
    protected static final int STATE_CONNECTING = 2;
    protected static final int STATE_CONNECTED = 3;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_CONNECT_DEVICE = 2;

    public BluetoothService(Activity activity)
    {
        this.activity = activity;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void start()
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
    }

    public synchronized void connect(BluetoothDevice device)
    {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        connectThread = new BluetoothConnectThread(device, this);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket)
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
        clientThread = new BluetoothClientThread(socket, this);
        clientThread.start();
        setState(STATE_CONNECTED);
    }

    public synchronized void stop()
    {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out)
    {
        BluetoothClientThread clientConnected;
        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            clientConnected = clientThread;
            clientConnected.write(out);
        }
    }

    protected synchronized void setState(int state)
    {
        this.state = state;
    }

    protected synchronized int getState()
    {
        return this.state;
    }

    public void enableBluetooth()
    {
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    public void scanDevice()
    {
        Intent intent = new Intent(activity, BluetoothListActivity.class);
        activity.startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    public void getDeviceInfo(Intent data)
    {
        String address = data.getExtras().getString(BluetoothListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connect(device);
    }

    public boolean getDeviceState()
    {
        return bluetoothAdapter != null;
    }

    public Activity getCallerActivity()
    {
        return activity;
    }

    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;
    }

    public BluetoothConnectThread getBluetoothConnectThread()
    {
        return connectThread;
    }

    public boolean isServiceConnected()
    {
        synchronized (this) {
            return state == STATE_CONNECTED;
        }
    }

    public void setBluetoothConnectThread(BluetoothConnectThread connectThread)
    {
        this.connectThread = connectThread;
    }

    public void connectionFailed()
    {
        setState(STATE_LISTEN);
    }
}