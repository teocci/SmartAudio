package com.github.teocci.newsmartaudio.utils;

import net.kseek.streaming.SessionBuilder;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Config
{
    public static final String LOG_PREFIX = "[SmartAudio]";

    /**
     * By default AAC is the audio encoder.
     */
    public static int AUDIO_ENCODER = SessionBuilder.AUDIO_AAC;

    /**
     * By default NONE is the video encoder.
     */
    public static int VIDEO_ENCODER = SessionBuilder.VIDEO_NONE;

    public static int DEFAULT_BT_PORT = 7777;

    public static final String KEY_OPERATION_MODE = "operation_mode";
    public static final String KEY_STATION_NAME = "station_name";
    public static final String KEY_USED_NAMES = "station_used_names";
    public static final String KEY_STATION_NAME_LIST = "station_name_list";
    public static final String KEY_FEATURE_GUIDE = "feature_guide";
    public static final String KEY_MAIN_ACTIVITY = "main_activity";

    public static final String SERVICE_TYPE = "_smartmixer._tcp"; // Smart Mixer
    public static final String SERVICE_CHANNEL_NAME = "Channel_00";
    public static final String SERVICE_APP_NAME = "SmartAudio";
    public static final String SERVICE_NAME_SEPARATOR = ":";

    public static final String CLIENT_MODE = "client_mode";

    public static final String COMMAND_SEPARATOR = ";";
    public static final String PARAMETER_SEPARATOR = ":";
    public static final String VALUE_SEPARATOR = ",";

    public static final String TAG_WAKELOCK = "net.kseek.av.wakelock";
}
