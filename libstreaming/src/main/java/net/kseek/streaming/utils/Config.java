package net.kseek.streaming.utils;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-May-24
 */

public class Config
{
//    public final static String NTP_SERVER_HOST = "time.bora.net";
//    public final static int NTP_SEVER_PORT = 123;

    public final static String NTP_SERVER_HOST = "192.168.1.125";
    public final static int NTP_SEVER_PORT = 50780;

    public final static String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    public final static String KEY_DEVICE_NAME = "device_name";

    public final static String KEY_STREAM_VIDEO = "stream_video";
    public final static String KEY_STREAM_AUDIO = "stream_audio";

    public final static String KEY_AUDIO_ENCODER = "audio_encoder";
    public final static String KEY_AUDIO_BITRATE = "audio_bitrate";
    public final static String KEY_AUDIO_CHANNEL = "audio_channel";


    public final static String KEY_VIDEO_ENCODER = "video_encoder";
    public final static String KEY_VIDEO_BITRATE = "video_bitrate";
    public final static String KEY_VIDEO_FRAMERATE = "video_framerate";
    public final static String KEY_VIDEO_RESOLUTION = "video_resolution";

    public final static String KEY_VIDEO_WIDTH = "video_resX";
    public final static String KEY_VIDEO_HEIGHT = "video_resY";

    public final static String KEY_HTTP_ENABLED = "http_server_enabled";
    public final static String KEY_HTTPS_ENABLED = "use_https";
    /**
     * Key used in the SharedPreferences for the port used by the HTTP server.
     */
    public final static String KEY_HTTP_PORT = "http_port";

    /**
     * Key used in the SharedPreferences for the port used by the HTTPS server.
     */
    public final static String KEY_HTTPS_PORT = "https_port";

    /**
     * Key used in the SharedPreferences to store whether the RTSP server is enabled or not.
     */
    public final static String KEY_RTSP_ENABLED = "rtsp_enabled";

    /**
     * Key used in the SharedPreferences for the port used by the RTSP server.
     */
    public final static String KEY_RTSP_PORT = "rtsp_port";

    public final static String KEY_OPEN_SOURCE_LICENSE = "open_source_license";
}
