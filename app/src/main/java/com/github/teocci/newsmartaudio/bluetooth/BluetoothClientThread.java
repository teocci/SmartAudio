package com.github.teocci.newsmartaudio.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import com.github.teocci.newsmartaudio.R;
import com.github.teocci.newsmartaudio.ui.SmartAudioActivity;
import com.github.teocci.newsmartaudio.utils.LogHelper;

import java.io.InputStream;
import java.io.OutputStream;

import static com.github.teocci.newsmartaudio.bluetooth.BluetoothService.STATE_LISTEN;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */

public class BluetoothClientThread extends Thread
{
    private final String TAG = LogHelper.makeLogTag(BluetoothClientThread.class);

    private BluetoothSocket bluetoothSocket;
    private BluetoothService bluetoothService;
    private final Activity callerActivity;
    private InputStream inputStream;
    private OutputStream outputStream;

    public BluetoothClientThread(BluetoothSocket socket, BluetoothService bluetoothService)
    {
        this.bluetoothSocket = socket;
        this.bluetoothService = bluetoothService;
        this.callerActivity = bluetoothService.getCallerActivity();

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }

        callerActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(callerActivity, R.string.connect_success, Toast.LENGTH_SHORT).show();
            }
        });
        ((SmartAudioActivity) callerActivity).successBTConnection();
    }

    public void run()
    {
        byte[] buf = new byte[1024];
        while (true) {
            LogHelper.d(TAG, "socket read");
            try {
                inputStream.read(buf);
            } catch (Exception e) {
                LogHelper.e(TAG, e.getMessage());
                connectionLost();
                break;
            }
        }
    }

    public void write(byte[] buf)
    {
        try {
            LogHelper.d(TAG, "socket send");
            outputStream.write(buf);
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    public void cancel()
    {
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    public void connectionLost()
    {
        callerActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(callerActivity, R.string.connection_lost, Toast.LENGTH_SHORT).show();
            }
        });

        ((SmartAudioActivity) callerActivity).stopBTConnection();
        bluetoothService.setState(STATE_LISTEN);
    }
}
