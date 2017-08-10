package com.github.teocci.newsmartaudio.api;

import net.kseek.streaming.rtsp.RtspServer;

public class CustomRtspServer extends RtspServer
{
    public CustomRtspServer()
    {
        super();
        // RTSP server enable by default
        enabled = true;
    }
}
