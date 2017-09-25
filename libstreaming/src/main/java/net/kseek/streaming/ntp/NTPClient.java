package net.kseek.streaming.ntp;

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
    static final public String TAG = NTPClient.class.getSimpleName();

    private static NTPClient instance;

    private static NTPUDPClient client;

    private static InetAddress ntpServer;

    private NTPClient() {
        openNTPConnection();
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
        client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(5000);
        try {
            client.open();
            ntpServer = InetAddress.getByName(NTP_SERVER_HOST);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

        return client.isOpen();
    }

    public TimeStamp getNTPTime()
    {
        if (client != null && client.isOpen()) {
            try {
                TimeInfo info = client.getTime(ntpServer, NTP_SEVER_PORT);
                info.computeDetails(); // compute offset/delay if not already done
//                Long offsetValue = info.getOffset();
//                Long delayValue = info.getDelay();
//                String delay = (delayValue == null) ? "N/A" : delayValue.toString();
//                String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();
//
//                Log.e(TAG, " Roundtrip delay(ms) = " + delay
//                        + ", clock offset(ms) = " + offset); // offset in ms

                return TimeStamp.getNtpTime(TimeStamp.getCurrentTime().getTime() +
                        (info.getOffset() != null ? info.getOffset() : 0) +
                        (info.getDelay() != null ? info.getDelay() : 0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return TimeStamp.getCurrentTime();
    }

    public void closeNTPConnection()
    {
        if (client != null) {
            client.close();
        }
    }
}
