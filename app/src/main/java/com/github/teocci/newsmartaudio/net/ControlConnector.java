package com.github.teocci.newsmartaudio.net;

import android.os.AsyncTask;

import com.github.teocci.newsmartaudio.interfaces.ControlListener;
import com.github.teocci.newsmartaudio.utils.LogHelper;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.github.teocci.newsmartaudio.utils.Config.DEFAULT_BT_PORT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */
public class ControlConnector extends AsyncTask<Void, Void, Void>
{
    private final String TAG = LogHelper.makeLogTag(ControlConnector.class);

    private ControlListener bluetoothListener;
    private ServerSocket serverSocket;
    private ControlClientThread clientThread;

    public ControlConnector(ControlListener bluetoothListener)
    {
        this.bluetoothListener = bluetoothListener;
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        connect();
        return null;
    }

    private void connect()
    {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(DEFAULT_BT_PORT));

            while (!Thread.currentThread().isInterrupted()) {
                LogHelper.d(TAG, "connect");
                Socket client = serverSocket.accept();
                LogHelper.d(TAG, "S: Receiving...");

                clientThread = new ControlClientThread(bluetoothListener, client);
                clientThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (Exception e) {
            if ( e.getMessage() != null )
                LogHelper.e(TAG, e.getMessage());
        }
    }

    public int getPort()
    {
        if (serverSocket != null)
            return serverSocket.getLocalPort();
        return -1;
    }

    public void write(String data)
    {
        clientThread.write(data);
    }

    public void close()
    {
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }
}
