package com.project.imran.devicediscovery;

/**
 * Created by Administrator on 15-Jul-16.
 */
public class Endpoint {
    private String endpointId;
    private String endpointName;
    private String deviceId;
    private String serviceId;

    public Endpoint(String endpointId, String endpointName, String deviceId, String serviceId) {
        this.endpointId = endpointId;
        this.endpointName = endpointName;
        this.deviceId = deviceId;
        this.serviceId = serviceId;
    }

    public String getEndpointId() {return endpointId;}
    public String getEndpointName() {return endpointName;}
    public String getDeviceId() {return deviceId;}
    public String getServiceId() {return serviceId;}
}
