package com.project.imran.devicediscovery;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {

    public static boolean active = false;

    private static final String TAG = "sup";
    private GameView myGameView;
    private TextView mDebugInfo;
    private ImageView mImageView;

    public static NewActivity instance;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIsHost;
    private String mOtherEndpointId;

    Map<Integer, Integer> imageMap = new HashMap<>();
    private int imageNumber;

    HangmanData gameData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setImages();

        myGameView = new GameView(this);
        setContentView(R.layout.game_view);

        findViewById(R.id.button_send).setOnClickListener(this);

        mDebugInfo = (TextView) findViewById(R.id.debug_text);
        mDebugInfo.setMovementMethod(new ScrollingMovementMethod());

        mImageView = (ImageView) findViewById(R.id.image_hangman);
        imageNumber = 0;
        mImageView.setImageResource(imageMap.get(imageNumber));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        mOtherEndpointId = MainActivity.mOtherEndpointId;
        mIsHost = MainActivity.mIsHost;

        gameData = new HangmanData();
        gameData.imageIndex = 0;
        gameData.wordIndex = 3;
        gameData.wrongChars.add('a'); gameData.wrongChars.add('b'); gameData.wrongChars.add('c');
        gameData.rightChars.add('x'); gameData.rightChars.add('y'); gameData.rightChars.add('z');
    }

    private void nextImage() {
        imageNumber++;
        gameData.imageIndex++;

        if (gameData.imageIndex > 10)
            gameData.imageIndex = 0;

        mImageView.setImageResource(imageMap.get(gameData.imageIndex));
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private String[] words = {
            "Malaysia",
            "America",
            "Russia",
            "France",
            "Britain"
    };

    private void setImages() {
        imageMap.put(0, R.drawable.hangman0);
        imageMap.put(1, R.drawable.hangman1);
        imageMap.put(2, R.drawable.hangman2);
        imageMap.put(3, R.drawable.hangman3);
        imageMap.put(4, R.drawable.hangman4);
        imageMap.put(5, R.drawable.hangman5);
        imageMap.put(6, R.drawable.hangman6);
        imageMap.put(7, R.drawable.hangman7);
        imageMap.put(8, R.drawable.hangman8);
        imageMap.put(9, R.drawable.hangman9);
        imageMap.put(10, R.drawable.hangman10);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_send:
                sendMessage();
//                debugLog("isConnected: " + mGoogleApiClient.isConnected());
//                nextImage();
//                debugLog("Button pressed");
                break;
        }
    }

    private void serial() {
        byte[] b = serialize();

        String s = new String(b);
        debugLog(b.toString());
        deserialize(s);
    }

    private byte[] serialize() {
        String s = "0";
        s+= Integer.toString(gameData.imageIndex);
        s+= Integer.toString(gameData.wordIndex);
        for (char c : gameData.wrongChars) {
            s += c;
        }
        s+= " ";
        for (char c : gameData.rightChars) {
            s += c;
        }
        s += "/";

        return s.getBytes();
    }

    private void deserialize(String s) {
        int i = 0;
        debugLog(""+i);
        gameData.imageIndex = (Character.getNumericValue(s.charAt(++i)));
        debugLog(""+i);

        gameData.wordIndex = (Character.getNumericValue(s.charAt(++i)));
        debugLog(""+i);

        gameData.wrongChars.clear();
        while (s.charAt(++i) != ' ') {
            gameData.wrongChars.add(s.charAt(i));
        }

        gameData.rightChars.clear();
        while(s.charAt(++i) != '/') {
            gameData.rightChars.add(s.charAt(i));
        }
    }

    public boolean isActive() { return active; }

    public void debugLog(String message) {
        Log.d(TAG, message);
        mDebugInfo.append(message + "\n");
    }

    public void sendMessage() {
        Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, serialize());
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        debugLog("Message from " + endpointId + ": " + new String(payload));
    }

    @Override
    public void onDisconnected(String s) {
        Toast.makeText(NewActivity.this, "Lost connection with other endpoint",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        AlertDialog exitDialog = new AlertDialog.Builder(this)
                .setTitle("Connection Request")
                .setMessage("Are you sure you want to quit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }

                }).create();

        exitDialog.show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        debugLog("Client Connected");
        startAdvertising();
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(int i) {
        debugLog("onConnectionSuspended");
    }

    @Override
    public void onConnectionRequest(final String endpointId, String deviceId,
                                    String endpointName, byte[] payload) {
        debugLog("onConnectionRequest: " + endpointId + ", " + endpointName);

        Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, endpointId,
                payload, NewActivity.this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            debugLog("acceptConnectionRequest: SUCCESS");

                            mIsHost = false;
                            mOtherEndpointId = endpointId;
                        } else {
                            debugLog("acceptConnectionRequest: FAILED");
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(NewActivity.this, "Failed to connect with endpoint",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public void connectTo(String endpointId) {
        String myName = null;
        byte[] myPayload = null;

        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, myName, endpointId, myPayload,
                new Connections.ConnectionResponseCallback(){
                    @Override
                    public void onConnectionResponse(String endpointId, Status status, byte[] bytes) {
                        debugLog("onConnectionResponse: " + endpointId + ", " + status);
                        if (status.isSuccess()) {
                            debugLog("onConnectionResponse: " + endpointId + " SUCCESS");
                            Toast.makeText(NewActivity.this, "Connected",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            debugLog("onConnectionResponse: " + endpointId + "FAILED");
                        }
                    }
                }, this);
    }

    private void startAdvertising() {
        if (!isConnectedToNetwork()) {
            debugLog("startAdvertising: Not connected to network");
            return;
        }

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

        long NO_TIMEOUT = 1000L;

        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, NO_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            debugLog("startDiscovery:onResult: SUCCESS");
                        }
                    }
                });
    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        if (mIsHost)
            connectTo(endpointId);
    }

    @Override
    public void onEndpointLost(String mOtherEndpointId) {
        // An endpoint previously available for connection is no longer there
        debugLog("Lost connection with endpoint: " + mOtherEndpointId);
    }
}
