package com.github.teocci.newsmartaudio.net;

import android.os.AsyncTask;

import com.github.teocci.newsmartaudio.interfaces.ControlListener;
import com.github.teocci.newsmartaudio.utils.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */
public class ControlClientThread extends AsyncTask<Void, Void, Void>
{
    private final String TAG = LogHelper.makeLogTag(ControlClientThread.class);

    private ControlListener controlListener;
    private Socket clientSocket;
    private BufferedReader input;
    private BufferedWriter output;

    public ControlClientThread(ControlListener controlListener, Socket socket)
    {
        this.controlListener = controlListener;
        this.clientSocket = socket;

        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        try {
            if (this.controlListener != null) {
                this.controlListener.connectionSetting(true);
            }

            while (!Thread.currentThread().isInterrupted()) {
                String str = input.readLine();
                LogHelper.e(TAG, "Control line : " + str);
                if (str == null) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (this.controlListener != null) {
                    controlListener.receiveCommand(str);
                }
            }
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void param)
    {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (this.controlListener != null) {
                controlListener.connectionSetting(false);
            }
        } catch (Exception e) {
            if (e.getMessage() != null)
                LogHelper.e(TAG, e.getMessage());
        }
    }

    public void write(String data)
    {
        try {
            LogHelper.e(TAG, "Control send : " + data);
            output.write(data);
            output.flush();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }
}
