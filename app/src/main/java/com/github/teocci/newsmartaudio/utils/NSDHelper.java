package com.github.teocci.newsmartaudio.utils;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Base64;

import com.github.teocci.newsmartaudio.model.ServiceInfo;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.teocci.newsmartaudio.utils.Config.CLIENT_MODE;
import static com.github.teocci.newsmartaudio.utils.Config.SERVICE_APP_NAME;
import static com.github.teocci.newsmartaudio.utils.Config.SERVICE_CHANNEL_NAME;
import static com.github.teocci.newsmartaudio.utils.Config.SERVICE_NAME_SEPARATOR;
import static com.github.teocci.newsmartaudio.utils.Config.SERVICE_TYPE;
import static com.github.teocci.newsmartaudio.utils.Utilities.getDeviceID;
import static net.kseek.streaming.rtsp.RtspServer.DEFAULT_RTSP_PORT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */

public class NSDHelper
{
    private static String TAG = LogHelper.makeLogTag(NSDHelper.class);

    private Context context;
    private final ReentrantLock reentrantLock;
    private Condition condition;

    private NsdManager nsdManager;
    private NsdManager.ResolveListener resolveListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private boolean isDiscoveryStarted;

    private NSDHelper nsdHelper;

    private String operationMode;
    private String stationName;
    private String serviceName;

    private NsdServiceInfo currentNsdServiceInfo;

    private final Map<String, ServiceInfo> serviceInfo; // Sorting required

    public NSDHelper(Context context)
    {
        this.context = context;
        reentrantLock = new ReentrantLock();
        serviceInfo = Collections.synchronizedMap(new TreeMap<String, ServiceInfo>());
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        nsdHelper = this;
    }

    public void initializeNsd()
    {
//        initializeResolveListener();
//        initializeDiscoveryListener();
//        initializeRegistrationListener();

        //nsdManager.init(context.getMainLooper(), this);
    }

    public void registerService()
    {
        LogHelper.e("registerService()");

        final String deviceID = getDeviceID(context.getContentResolver());
        reentrantLock.lock();

        try {
            // Android NSD implementation is very unstable when services
            // registers with the same name.
            // Therefore, we will use "SERVICE_CHANNEL_NAME:STATION_NAME:DEVICE_ID:".
            final NsdServiceInfo serviceInfo = new NsdServiceInfo();
            final String serviceName = Base64.encodeToString(SERVICE_APP_NAME.getBytes(), (Base64.NO_WRAP)) +
                    SERVICE_NAME_SEPARATOR +
                    Base64.encodeToString(SERVICE_CHANNEL_NAME.getBytes(), (Base64.NO_WRAP)) +
                    SERVICE_NAME_SEPARATOR +
                    deviceID +
                    SERVICE_NAME_SEPARATOR +
                    Base64.encodeToString(stationName.getBytes(), (Base64.NO_WRAP)) +
                    SERVICE_NAME_SEPARATOR;

            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setServiceName(serviceName);
            serviceInfo.setPort(DEFAULT_RTSP_PORT);

            LogHelper.e(TAG, "registerService(): " + serviceInfo);
            // registrationListener = new RegistrationListener();
            initializeRegistrationListener();
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            return;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void initializeDiscoveryListener()
    {
        discoveryListener = new NsdManager.DiscoveryListener()
        {
            @Override
            public void onDiscoveryStarted(String serviceType)
            {
                reentrantLock.lock();
                try {
                    if (condition == null) {
                        isDiscoveryStarted = true;
                        if (!operationMode.equals(CLIENT_MODE)) {
//                            while (!isSetStationListener) {} // waiting for the listener
                            updateServiceState();
                        }
                        LogHelper.e(TAG, "initDiscoveryListener() Discovery started");
                    } else {
                        nsdManager.stopServiceDiscovery(this);
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo)
            {
//                while (!isSetStationListener) {} // waiting for the listener
                try {
                    final String serviceName = nsdServiceInfo.getServiceName().split(" ")[0];
                    if (!serviceName.contains(SERVICE_NAME_SEPARATOR)) {
                        return;
                    }

                    final String[] ss = serviceName.split(SERVICE_NAME_SEPARATOR);

                    final String channelName = new String(Base64.decode(ss[1], 0));
                    final String stationName = new String(Base64.decode(ss[3], 0));

                    LogHelper.e(TAG, "initDiscoveryListener.onServiceFound-> " + channelName + ": " + nsdServiceInfo);
                    LogHelper.e(TAG, "initDiscoveryListener.onServiceFound-> serviceName: " + serviceName);
                    if (channelName.compareTo(SERVICE_CHANNEL_NAME) == 0) {
                        if (serviceName != null && serviceName.equals(serviceName))
                            return;

                        synchronized (serviceInfo) {
                            ServiceInfo serviceInfo = nsdHelper.serviceInfo.get(serviceName);
                            if (serviceInfo == null) {
                                serviceInfo = new ServiceInfo();
                                serviceInfo.nsdServiceInfo = nsdServiceInfo;
                                serviceInfo.stationName = stationName;
                                serviceInfo.serviceName = serviceName;

                                nsdHelper.serviceInfo.put(serviceName, serviceInfo);
//                                final StationListener stationListener = nsdHelper.stationListener;
//                                if (stationListener != null) {
//                                    stationListener.onStationListChanged(getStationList());
//                                }
                            }
                        }
                    }
                } catch (final IllegalArgumentException ex) {
                    // Base64.decode() can throw an exception, will be better to handle it.
                    LogHelper.w(TAG, ex.toString());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service)
            {
                LogHelper.e(TAG, "service lost" + service);
                if (currentNsdServiceInfo == service) {
                    currentNsdServiceInfo = null;
                    serviceName = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType)
            {
                LogHelper.e(TAG, "Discovery stopped: " + serviceType);
                reentrantLock.lock();
                try {
                    // Wakes up one waiting thread.
                    condition.signal();
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode)
            {
                LogHelper.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
                reentrantLock.lock();
                try {
                    if (condition != null)
                        // Wakes up one waiting thread.
                        condition.signal();
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode)
            {
                LogHelper.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener(final String serviceName)
    {
        resolveListener = new NsdManager.ResolveListener()
        {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode)
            {
                reentrantLock.lock();
                try {
                    LogHelper.e(TAG, "initResolveListener.onResolveFailed()-> errorCode=" + errorCode);
//                    final ServiceInfo serviceInfo = Channel.this.serviceInfo.get(serviceName);
//                    if (serviceInfo != null) {
//                        if (BuildConfig.DEBUG &&
//                                ((serviceInfo.connector != null) || (serviceInfo.session != null))) {
//                            throw new AssertionError();
//                        }
//
//                        if (latchSynchronizer != null) {
//                            Channel.this.serviceInfo.remove(serviceName);
//                            registrationListener = null;
//                            latchSynchronizer.countDown();
//                        } else if (serviceInfo.nsdServiceInfo == null) {
//                            // Service lost while being resolved, let's remove record.
//                            Channel.this.serviceInfo.remove(serviceName);
//                        } else {
//                            resolveListener = null;
//                            resolveNextLocked(serviceName);
//                        }
//                    } else
//                        LogHelper.e(TAG, channelName +
//                                ": channel.initResolveListener.onResolvedFailed()-> internal error: service info not found [" + serviceName + "]");
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo)
            {
                LogHelper.e(TAG, "initResolveListener.onServiceResolved()-> " + nsdServiceInfo);
                reentrantLock.lock();
                try {
                    ServiceInfo serviceInfo = nsdHelper.serviceInfo.get(serviceName);
                    if (serviceInfo != null) {
                        resolveListener = null;
                        if (!serviceInfo.resolved) {
                            LogHelper.e(TAG, "initResolveListener.onServiceResolved()-> inetSocketAddress creation");
//                            service.serviceInfo.get(serviceName).nsdServiceInfo = nsdServiceInfo;
//                            service.serviceInfo.get(serviceName).resolved = true;
//                            service.serviceInfo.get(serviceName).address = nsdServiceInfo.getHost().getHostAddress();
//                            service.serviceInfo.get(serviceName).port = nsdServiceInfo.getPort();
//                            service.serviceInfo.get(serviceName).state = 1;
                            nsdHelper.serviceInfo.get(serviceName)
                                    .setNsdServiceInfo(nsdServiceInfo)
                                    .setResolved(true)
                                    .setAddress(nsdServiceInfo.getHost().getHostAddress())
                                    .setPort(nsdServiceInfo.getPort())
                                    .setState(1);

//                            final StationListener stationListener = service.stationListener;
//                            if (stationListener != null)
//                                stationListener.onStationListChanged(getStationList());

//                            initRtspClient(nsdServiceInfo);
//                            initRemoteController();
                        }
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }
        };
    }

    public void initializeRegistrationListener()
    {
        registrationListener = new NsdManager.RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo)
            {
                // Service registered now, we use service name to distinguish client from server,
                // so now we can try to connect to the services already discovered.
                LogHelper.e(TAG, "onServiceRegistered()-> " + nsdServiceInfo);
                reentrantLock.lock();
                try {
                    if (serviceName != null) {
                        LogHelper.e(TAG, "initRegistrationListener()-> Duplicate registration: " + nsdServiceInfo);
                        return;
                    }

                    serviceName = nsdServiceInfo.getServiceName();
                    updateServiceState();

                    for (Map.Entry<String, ServiceInfo> entry : serviceInfo.entrySet()) {
                        if (serviceName.compareTo(entry.getKey()) > 0) {
                            final ServiceInfo serviceInfo = entry.getValue();
                            LogHelper.e(TAG, "onServiceRegistered()-> resolve service: " +
                                    serviceInfo.nsdServiceInfo);
                            serviceInfo.nsdUpdates = 0;

                            initializeResolveListener(entry.getKey());
                            nsdManager.resolveService(serviceInfo.nsdServiceInfo, resolveListener);
                            break;
                        }
                    }

//                    final StationListener stationListener = service.stationListener;
//                    if (stationListener != null)
//                        stationListener.onStationListChanged(getStationList());

                    LogHelper.e(TAG, "initRegistrationListener()-> getting the ServiceName: " + serviceName);

                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                LogHelper.e(TAG, "onRegistrationFailed()-> " + serviceInfo + " (" + errorCode + ")");
                reentrantLock.lock();
                try {
                    registrationListener = null;
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo)
            {
                LogHelper.e(TAG, "onServiceUnregistered()-> " + nsdServiceInfo);
                reentrantLock.lock();
                try {
                    registrationListener = null;
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Not expected...
                LogHelper.e(TAG, "onUnregistrationFailed()-> " + serviceInfo + " (" + errorCode + ")");
            }
        };
    }

    private void updateServiceState()
    {
        LogHelper.e(TAG, "updateServiceState()");

//        final StationListener stationListener = this.stationListener;
//        if (stationListener != null) {
//            boolean registered;
//            String str = (!operationMode.equals(CLIENT_MODE)) ? "Server Mode" : "Client Mode";
//            if (serviceName != null) {
//                str += "(Active)";
//                registered = true;
//            } else {
//                registered = !operationMode.equals(CLIENT_MODE);
//            }
//            stationListener.onStateChanged(str, registered);
//        }
    }

    public void discoverServices()
    {
        nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void setStationName(String stationName)
    {
        this.stationName = stationName;
    }

    public void setOperationMode(String operationMode)
    {
        this.operationMode = operationMode;
    }

    public boolean stopDiscovery()
    {
        boolean interrupted = false;
        reentrantLock.lock();
        try {
            if (discoveryListener != null) {
                final Condition cond = reentrantLock.newCondition();
                condition = cond;

                if (isDiscoveryStarted)
                    nsdManager.stopServiceDiscovery(discoveryListener);

                cond.await();
            }
        } catch (final InterruptedException ex) {
            LogHelper.w(TAG, ex.toString());
            interrupted = true;
        } finally {
            reentrantLock.unlock();
        }
        return interrupted;
    }

//    public NsdServiceInfo getChosenServiceInfo()
//    {
//        return mService;
//    }

    public void tearDown()
    {
        reentrantLock.lock();
        try {
            if (registrationListener == null) {
                // Acceptor is not started yet
                LogHelper.e(TAG, "wait channelAcceptor");
            } else {
                LogHelper.e(TAG, "unregister service");
                nsdManager.unregisterService(registrationListener);
            }

            // Discovery is stopped now, onServiceFound()/onServiceLost() will not be called any more.
            synchronized (serviceInfo) {
                for (Map.Entry<String, ServiceInfo> entry : serviceInfo.entrySet()) {
                    if (entry.getValue().nsdServiceInfo != null) {
                        final String serviceName = entry.getKey();
                        final ServiceInfo serviceInfo = entry.getValue();
                        if ((resolveListener != null) && this.serviceName.equals(serviceName)) {
                            serviceInfo.nsdServiceInfo = null;
                        }
                    }
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }
}
