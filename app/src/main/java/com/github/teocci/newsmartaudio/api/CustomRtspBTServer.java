package com.github.teocci.newsmartaudio.api;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.github.teocci.newsmartaudio.ui.SmartAudioActivity;
import com.github.teocci.newsmartaudio.utils.LogHelper;
import com.github.teocci.newsmartaudio.utils.NSDHelper;

import net.kseek.streaming.rtsp.RtspServer;


import static com.github.teocci.newsmartaudio.utils.Config.CLIENT_MODE;
import static com.github.teocci.newsmartaudio.utils.Config.KEY_OPERATION_MODE;
import static com.github.teocci.newsmartaudio.utils.Config.KEY_STATION_NAME;

public class CustomRtspBTServer extends RtspServer
{
    private CustomRtspBTServer service;

    private final RemoteBinder serviceBinder;
    private final NSDHelper nsdHelper;

//    private StationListener stationListener;
//    private StatInfoListener statInfoListener;
    private boolean isSetStationListener;
    private boolean isSetStatInfoListener;

    private String operationMode;
    private String stationName;
    private String serviceName;

    private SmartAudioActivity currentActivity;

    public CustomRtspBTServer()
    {
        super();
        serviceBinder = new RemoteBinder();
        nsdHelper = new NSDHelper(this);
        service = this;
        isSetStationListener = false;

        // RTSP server enable by default
        enabled = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        LogHelper.d(TAG, "onStartCommand: flags=" + flags + " startId=" + startId);

        final String operationMode = intent.getStringExtra(KEY_OPERATION_MODE);
        final String stationName = intent.getStringExtra(KEY_STATION_NAME);

        this.operationMode = operationMode;
        this.stationName = stationName;

        final Boolean isClient = operationMode.equals(CLIENT_MODE);
        if (isClient) {
//            initRtspService();
//            initAudioRecorder();
            nsdHelper.registerService();
        }

        nsdHelper.discoverServices();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        LogHelper.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        LogHelper.d(TAG, "onBind");
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        LogHelper.d(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onDestroy()
    {
        LogHelper.d(TAG, "onDestroy");

        boolean interrupted = false;

        if (nsdHelper != null) {
            interrupted = nsdHelper.stopDiscovery();
            nsdHelper.tearDown();
        }

        if (interrupted)
            Thread.currentThread().interrupt();

        LogHelper.d(TAG, "onDestroy: done");
        super.onDestroy();
    }


    /**
     * RemoteBinder Class should be used for the client Binder.  Because we know this service always
     * runs in the same process as its clients.
     */
    public class RemoteBinder extends Binder
    {
//        public CustomRtspServer getService()
//        {
//            // Return this instance of LocalService so clients can call public methods
//            return CustomRtspServer.this;
//        }
//
//        public void setStationListener(RTSPStateListener stateListener, StationListener stationListener)
//        {
//            stateListener.onInit(new AudioRecorder());
//            service.setStationListener(stationListener);
//        }
//
//        public void setStationListener(StationListener stationListener)
//        {
//            LogHelper.e(TAG, "setStationListener");
//            service.setStationListener(stationListener);
//        }
//
//        public void setStatInfoListener(StatInfoListener statInfoListener)
//        {
//            LogHelper.e(TAG, "setStatInfoListener");
//            service.setStatInfoListener(statInfoListener);
//        }
//
//        public void setStationName(String stationName)
//        {
//            service.setStationName(stationName);
//        }
//
//        public void resolveStation(String serviceName)
//        {
//            ServiceInfo serviceInfo = service.serviceInfo.get(serviceName);
//            if (serviceInfo == null) {
//                // Should not happen, but probably would be better to handle a case.
//                LogHelper.e(TAG, "resolveStation()-> internal error: service not found: " + serviceName);
//            } else {
//                initResolveListener(serviceInfo.serviceName);
//                nsdManager.resolveService(serviceInfo.nsdServiceInfo, resolveListener);
//            }
//        }
//
//        public void setCurrentActivity(BaseModeActivity currentActivity)
//        {
//            service.currentActivity = currentActivity;
//        }
    }
}

