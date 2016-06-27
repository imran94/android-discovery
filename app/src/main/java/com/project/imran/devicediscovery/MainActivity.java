package com.project.imran.devicediscovery;

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


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {

    private static final String TAG = MainActivity.class.getSimpleName();

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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
    }

    @Override
    public void onConnectionSuspended(int i) {
        debugLog("onConnectionSuspended: " + i);
    }

    private void debugLog(String msg) {
        Log.d(TAG, msg);
        mDebugInfo.append("\n" + msg);
    }
}
