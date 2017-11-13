package net.kseek.streaming.ntp;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static net.kseek.streaming.utils.Config.NTP_SERVER_HOST;
import static net.kseek.streaming.utils.Config.NTP_SEVER_PORT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Sep-07
 */

public class NTPClient
{
    public static final String TAG = NTPClient.class.getSimpleName();

    private final static int INTERVAL = 1000 * 61; //61 seconds

    private Long offsetValue;
    private Long delayValue;

    private String host = NTP_SERVER_HOST;
    private int port = NTP_SEVER_PORT;

    private static NTPClient instance;

    private static NTPUDPClient client;

    private static InetAddress ntpServer;

    private Handler handler;
    private HandlerThread handlerThread;

    private Thread handlerTask = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            updateNTPTime();
            handler.postDelayed(handlerTask, INTERVAL);
        }
    });

    private NTPClient(String host, int port)
    {
        this.host = host;
        this.port = port;

        openNTPConnection();
    }

    private NTPClient()
    {
        openNTPConnection();
    }

    private void startRepeatingTask()
    {
        handlerTask.start();
    }

    private void stopRepeatingTask()
    {
        handler.removeCallbacks(handlerTask);
    }

    public static NTPClient getInstance(String host, int port)
    {
        if (instance == null) {
            synchronized (NTPClient.class) {
                if (instance == null) {
                    instance = new NTPClient(host, port);
                }
            }
        }

        return instance;
    }

    public static NTPClient getInstance()
    {
        if (instance == null) {
            synchronized (NTPClient.class) {
                if (instance == null) {
                    instance = new NTPClient();
                }
            }
        }

        return instance;
    }

    public boolean openNTPConnection()
    {
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(5000);
        try {
            client.open();
            ntpServer = InetAddress.getByName(host);
            startRepeatingTask();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

        return client.isOpen();
    }

    private void updateNTPTime()
    {
        if (client != null && client.isOpen()) {
            try {
                TimeInfo info = client.getTime(ntpServer, port);
                info.computeDetails(); // compute offset/delay if not already done

                offsetValue = info.getOffset();
                delayValue = info.getDelay();
                String delay = (delayValue == null) ? "N/A" : delayValue.toString();
                String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

                Log.e(TAG, " Roundtrip delay(ms) = " + delay
                        + ", clock offset(ms) = " + offset); // offset in ms

//                return TimeStamp.getNtpTime(TimeStamp.getCurrentTime().getTime() +
//                        (info.getOffset() != null ? info.getOffset() : 0) +
//                        (info.getDelay() != null ? info.getDelay() : 0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TimeStamp getNTPTime()
    {
        if (client != null && client.isOpen()) {
            return TimeStamp.getNtpTime(TimeStamp.getCurrentTime().getTime() +
                    (offsetValue != null ? offsetValue : 0) +
                    (delayValue != null ? delayValue : 0));
        }

        return TimeStamp.getCurrentTime();
    }

    public void closeNTPConnection()
    {
        stopRepeatingTask();
        if (client != null) {
            client.close();
        }
    }
}
