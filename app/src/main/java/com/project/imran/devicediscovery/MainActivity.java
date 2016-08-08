package com.project.imran.devicediscovery;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {

    private boolean mIsHost = false;

    private static final String TAG = "sup";

    private static final long TIMEOUT_ADVERTISE = 1000L * 30L;
    private static final long TIMEOUT_DISCOVER = 1000L * 30L;

    private static final int STATE_IDLE = 1023; // Not yet connected
    private static final int STATE_READY = 1024;    // Connected, ready to use Nearby
    private static final int STATE_ADVERTISING = 1025;  // Advertising for peers
    private static final int STATE_DISCOVERING = 1026;  // Looking for advertising peer
    private static final int STATE_CONNECTED = 1027;    // Found a peer

    // Connecting to the Nearby Connections API
    public GoogleApiClient mGoogleApiClient;

    // Displaying found endpoints
    private ListView endpointList;
    private List<Endpoint> myEndpoints = new ArrayList<>();
    private ArrayAdapter<Endpoint> endpointAdapter;

    private EditText mMessageText;
    private TextView mDebugInfo;
    private AlertDialog mConnectionRequestDialog;

    private int mState = STATE_IDLE;

    // Endpoint ID of the connected peer, used for messaging
    public String mOtherEndpointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button Listeners
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_discover).setOnClickListener(this);
        findViewById(R.id.button_send).setOnClickListener(this);
        findViewById(R.id.button_new).setOnClickListener(this);

        mMessageText = (EditText) findViewById(R.id.edittext_message);

        // Debug text view
        mDebugInfo = (TextView) findViewById(R.id.debug_text);
        mDebugInfo.setMovementMethod(new ScrollingMovementMethod());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        // Device list
        endpointAdapter = new MyEndpointAdapter(MainActivity.this, R.layout.endpoint_view, myEndpoints);
        endpointList = (ListView) findViewById(R.id.device_list);
        endpointList.setAdapter(endpointAdapter);

        updateViewVisibility(STATE_READY);
    }

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

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectTo(currentEndpoint.getEndpointId(), currentEndpoint.getEndpointName());
                }
            });

            return convertView;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        debugLog("onConnected");
        updateViewVisibility(STATE_READY);
    }

    @Override
    public void onConnectionSuspended(int i) {
        debugLog("onConnectionSuspended: " + i);
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void startAdvertising() {
        if (!isConnectedToNetwork()) {
            debugLog("startAdvertising: Not connected to network");
            return;
        }

        mIsHost = true;

         // Prompt other network devices to install the application
        List<AppIdentifier> appIdentifierList= new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        long NO_TIMEOUT = 0L;

        String name = null;
        Nearby.Connections.startAdvertising(mGoogleApiClient, name, appMetadata, NO_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(Connections.StartAdvertisingResult result) {
                        if (result.getStatus().isSuccess()) {
                            debugLog("startAdvertising:onResult: SUCCESS");
                            updateViewVisibility(STATE_ADVERTISING);
                        } else {
                            debugLog("startAdvertising:onResult: FAILURE ");

                            int statusCode = result.getStatus().getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING){
                                debugLog("Already advertising");
                            } else {
                                updateViewVisibility(STATE_READY);
                            }
                        }
                    }
                });
    }

    private void startDiscovery() {
        if (!isConnectedToNetwork()) {
            debugLog("startAdvertising: Not connected to network");
            return;
        }

        String serviceId = getString(R.string.service_id);

        long DISCOVER_TIMEOUT = 1000L;

        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, DISCOVER_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            debugLog("startDiscovery:onResult: SUCCESS");
                            updateViewVisibility(STATE_DISCOVERING);
                        } else {
                            debugLog("startAdvertising:onResult: FAILURE ");

                            int statusCode = status.getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING){
                                debugLog("Already discovering");
                            } else {
                                updateViewVisibility(STATE_READY);
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String msg = mMessageText.getText().toString();
        Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, msg.getBytes());
    }

    private void startNewActivity() {
        Intent intent = new Intent(this, NewActivity.class);
        startActivity(intent);
    }

    public void connectTo(String endpointId, final String endpointName) {
        debugLog("connectTo: " + endpointId + ", " + endpointName);

        String myName = null;
        byte[] myPayload = null;
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, myName, endpointId, myPayload,
                new Connections.ConnectionResponseCallback(){
                    @Override
                    public void onConnectionResponse(String endpointId, Status status, byte[] bytes) {
                        debugLog("onConnectionResponse: " + endpointId + ", " + status);
                        if (status.isSuccess()) {
                            debugLog("onConnectionResponse: " + endpointId + " SUCCESS");
                            Toast.makeText(MainActivity.this, "Connected to " + endpointName,
                                    Toast.LENGTH_SHORT).show();

                            mOtherEndpointId = endpointId;
                            updateViewVisibility(STATE_CONNECTED);
                        } else {
                            debugLog("onConnectionResponse: " + endpointId + "FAILED");
                        }
                    }
                }, this);
    }

    @Override
    public void onConnectionRequest(final String endpointId, String deviceId,
                                    String endpointName, byte[] payload) {
        debugLog("onConnectionRequest: " + endpointId + ", " + endpointName);

        // Device is advertising and has received a connection request. Show dialog
        // asking for the user's response.
        mConnectionRequestDialog = new AlertDialog.Builder(this)
                .setTitle("Connection Request")
                .setMessage("Do you want to connect to " + endpointName + "?")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        byte[] payload = null;
                        Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, endpointId,
                                payload, MainActivity.this)
                                .setResultCallback(new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if (status.isSuccess()) {
                                            debugLog("acceptConnectionRequest: SUCCESS");

                                            mOtherEndpointId = endpointId;
                                            updateViewVisibility(STATE_CONNECTED);
                                        } else {
                                            debugLog("acceptConnectionRequest: FAILED");
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Nearby.Connections.rejectConnectionRequest(mGoogleApiClient, endpointId);
                    }
                }).create();

        mConnectionRequestDialog.show();
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_advertise:
                startAdvertising();
                break;
            case R.id.button_discover:
                 startDiscovery();
                break;
            case R.id.button_send:
                 sendMessage();
                break;
            case R.id.button_new:
                startNewActivity();
                break;
        }
    }

    private void updateViewVisibility(int newState) {
        mState = newState;

        switch(mState) {
            case STATE_IDLE:
                findViewById(R.id.layout_nearby_buttons).setVisibility(View.GONE);
                findViewById(R.id.layout_message).setVisibility(View.GONE);
                break;
            case STATE_READY:
                findViewById(R.id.layout_nearby_buttons).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_message).setVisibility(View.GONE);
                break;
            case STATE_DISCOVERING:
                break;
            case STATE_ADVERTISING:
                break;
            case STATE_CONNECTED:
                findViewById(R.id.layout_nearby_buttons).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_message).setVisibility(View.VISIBLE);
                break;
        }
    }

    private void debugLog(String msg) {
        Log.d(TAG, msg);
        mDebugInfo.append("\n" + msg);
    }

    private void updateEndpointList() {
        endpointAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        debugLog("Endpoint found:");
        debugLog("Endpoint ID: " + endpointId + ", Endpoint Name: " + endpointName);
        debugLog("Device ID: " + deviceId);
        debugLog("Service ID: : " + serviceId);

        myEndpoints.add(new Endpoint(endpointId, endpointName, deviceId, serviceId));
        updateEndpointList();
    }

    @Override
    public void onEndpointLost(String mOtherEndpointId) {
        // An endpoint previously available for connection is no longer there
        debugLog("Lost connection with endpoint: " + mOtherEndpointId);
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        debugLog("onMessageReceived: " + endpointId + ":" + new String(payload));
    }

    @Override
    public void onDisconnected(String endpointId) {
        debugLog("onDisconnected:" + endpointId);
        updateViewVisibility(STATE_READY);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        debugLog("onConnectionFailed: " + connectionResult);
    }
}
