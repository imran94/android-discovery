package com.project.imran.devicediscovery;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 16-Jul-16.
 */
public class EndpointAdapter extends ArrayAdapter<Endpoint> {
    private List<Endpoint> myEndpoints;
    private int endpoint_view;

    public EndpointAdapter(Context context, @LayoutRes int resource, List<Endpoint> objects) {
        super(context, resource, objects);

        endpoint_view = resource;
        myEndpoints = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Endpoint currentEndpoint = myEndpoints.get(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.endpoint_view, null);
        }

        //Fill the view
        TextView endpointId = (TextView) convertView.findViewById(R.id.endpoint_id);
        endpointId.setText(currentEndpoint.getEndpointId());

        TextView endpointName = (TextView) convertView.findViewById(R.id.endpoint_name);
        endpointName.setText(currentEndpoint.getEndpointName());

        TextView deviceId = (TextView) convertView.findViewById(R.id.device_id);
        deviceId.setText(currentEndpoint.getDeviceId());

        TextView serviceId = (TextView) convertView.findViewById(R.id.service_id);
        serviceId.setText(currentEndpoint.getServiceId());

        return convertView;
    }
}
