package com.github.teocci.newsmartaudio.utils;

import net.kseek.streaming.SessionBuilder;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Config
{
    public static final String LOG_PREFIX = "[AMixer]";

    /**
     * By default AMR is the audio encoder.
     */
    public static int AUDIO_ENCODER = SessionBuilder.AUDIO_AAC;

    /**
     * If the notification is enabled in the status bar of the phone.
     */
    public static boolean NOTIFICATION_ENABLED = true;

    public static int DEFAULT_BT_PORT = 7777;

    public static final String KEY_OPERATION_MODE = "operation_mode";
    public static final String KEY_STATION_NAME = "station_name";
    public static final String KEY_USED_NAMES = "station_used_names";
    public static final String KEY_STATION_NAME_LIST = "station_name_list";

    public static final String SERVICE_TYPE = "_smartmixer._tcp"; // Smart Mixer
    public static final String SERVICE_CHANNEL_NAME = "Channel_00";
    public static final String SERVICE_NAME_SEPARATOR = ":";
}