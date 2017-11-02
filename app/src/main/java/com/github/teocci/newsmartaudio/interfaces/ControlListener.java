package com.github.teocci.newsmartaudio.interfaces;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-26
 */
public interface ControlListener
{
    void receiveCommand(String str);

    void connectionSetting(boolean isConnected);
}
