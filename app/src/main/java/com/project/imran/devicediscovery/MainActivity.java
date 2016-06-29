package com.project.imran.devicediscovery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
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
    private GoogleApiClient mGoogleApiClient;

    private TextView mDebugInfo;

    private int mState = STATE_IDLE;

    // Endpoint ID of the connected peer, used for messaging
    private String mOtherEndpointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button Listeners
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_discover).setOnClickListener(this);
        findViewById(R.id.button_send).setOnClickListener(this);

        // Debug text view
        mDebugInfo = (TextView) findViewById(R.id.debug_text);
//        mDebugInfo.setMovementMethod(new ScrollingMovementMethod());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        updateViewVisiblity(STATE_READY);
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
                            updateViewVisiblity(STATE_ADVERTISING);
                        } else {
                            debugLog("startAdvertising:onResult: FAILURE ");

                            int statusCode = result.getStatus().getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING){
                                debugLog("Already advertising");
                            } else {
                                updateViewVisiblity(STATE_READY);
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
                            updateViewVisiblity(STATE_DISCOVERING);
                        } else {
                            debugLog("startAdvertising:onResult: FAILURE ");

                            int statusCode = status.getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING){
                                debugLog("Already discovering");
                            } else {
                                updateViewVisiblity(STATE_READY);
                            }
                        }
                    }
                });
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
                // sendMessage();
                break;
        }
    }

    private void updateViewVisiblity(int newState) {
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

    @Override
    public void onConnectionRequest(String s, String s1, String s2, byte[] bytes) {

    }

    @Override
    public void onEndpointFound(String s, String s1, String s2, String s3) {

    }

    @Override
    public void onEndpointLost(String s) {

    }

    @Override
    public void onMessageReceived(String s, byte[] bytes, boolean b) {

    }

    @Override
    public void onDisconnected(String s) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
