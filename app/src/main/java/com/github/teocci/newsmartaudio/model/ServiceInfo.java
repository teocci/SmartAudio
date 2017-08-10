package com.github.teocci.newsmartaudio.model;

import android.net.nsd.NsdServiceInfo;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jul-17
 */

public class ServiceInfo
{
    public NsdServiceInfo nsdServiceInfo;
    public String stationName;
    public String serviceName;
    public String address;
    public int port;
    public boolean resolved;
    public int nsdUpdates;
    public int state;
    public long ping;
    
    public ServiceInfo() {
        resolved = false;
        address = "not resolved";
        nsdUpdates = 0;
        state = 0;
        ping = 0;
    }

    public ServiceInfo setNsdServiceInfo(NsdServiceInfo nsdServiceInfo)
    {
        this.nsdServiceInfo = nsdServiceInfo;
        return this;
    }

    public ServiceInfo setStationName(String stationName)
    {
        this.stationName = stationName;
        return this;
    }

    public ServiceInfo setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
        return this;
    }

    public ServiceInfo setAddress(String address)
    {
        this.address = address;
        return this;
    }

    public ServiceInfo setPort(int port)
    {
        this.port = port;
        return this;
    }

    public ServiceInfo setResolved(boolean resolved)
    {
        this.resolved = resolved;
        return this;
    }

    public ServiceInfo setNsdUpdates(int nsdUpdates)
    {
        this.nsdUpdates = nsdUpdates;
        return this;
    }

    public ServiceInfo setState(int state)
    {
        this.state = state;
        return this;
    }

    public ServiceInfo setPing(long ping)
    {
        this.ping = ping;
        return this;
    }
}