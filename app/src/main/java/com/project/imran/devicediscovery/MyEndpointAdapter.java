package com.project.imran.devicediscovery;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 16-Jul-16.
 */
public class MyEndpointAdapter extends ArrayAdapter<Endpoint> {
    private List<Endpoint> myEndpoints;
    private int endpoint_view;

    public MyEndpointAdapter(Context context, @LayoutRes int resource, List<Endpoint> objects) {
        super(context, resource, objects);

        endpoint_view = resource;
        myEndpoints = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;

        if (itemView == null) {
            return itemView;
        }

        Endpoint currentEndpoint = myEndpoints.get(position);

        //Fill the view
        TextView endpointId = (TextView) itemView.findViewById(R.id.endpoint_id);
        endpointId.setText(currentEndpoint.getEndpointId());

        TextView endpointName = (TextView) itemView.findViewById(R.id.endpoint_name);
        endpointId.setText(currentEndpoint.getEndpointName());

        TextView deviceId = (TextView) itemView.findViewById(R.id.device_id);
        endpointId.setText(currentEndpoint.getDeviceId());

        TextView serviceId = (TextView) itemView.findViewById(R.id.service_id);
        endpointId.setText(currentEndpoint.getServiceId());

        return itemView;
    }
}
