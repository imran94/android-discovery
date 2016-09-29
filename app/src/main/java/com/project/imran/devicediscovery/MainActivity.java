package com.project.imran.devicediscovery;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Vibrator;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {

    public static MainActivity instance;

    public static boolean mIsHost = false;
    String mName = null;

    private static final String TAG = "MainActivity";

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
    private AlertDialog mConnectionRequestDialog;

    private int mState = STATE_IDLE;

    // Endpoint ID of the connected peer, used for messaging
    public static String mOtherEndpointId;
    public static String mOtherEndpointName;

    private TextView mDebugInfo;
    private TextView mGameText;

    Map<Integer, Integer> imageMap = new HashMap<>();
    ImageView mImageView;
    TextView wrongCharView;
    TextView keyWordView;
    TextView turnIndicator;
    EditText guessText;
    HangmanData gameData;
    boolean hasTurn = false;

    String[] words = {"malaysia","america","russia","france","britain"};

    MediaPlayer notification, success, failure;
    Vibrator vibrator;

    final long DISCOVER_TIMEOUT = 10000L;
    CountDownTimer discoveryTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Button Listeners
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_discover).setOnClickListener(this);
        findViewById(R.id.button_send).setOnClickListener(this);
        findViewById(R.id.button_guess).setOnClickListener(this);
        findViewById(R.id.button_restart).setOnClickListener(this);

        mMessageText = (EditText) findViewById(R.id.edittext_message);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .addApi(AppIndex.API).build();

        // Device list
        endpointAdapter = new MyEndpointAdapter(MainActivity.this, R.layout.endpoint_view, myEndpoints);
        endpointList = (ListView) findViewById(R.id.device_list);
        endpointList.setAdapter(endpointAdapter);

        mDebugInfo = (TextView) findViewById(R.id.debug_text);
        mDebugInfo.setMovementMethod(new ScrollingMovementMethod());
        mGameText = (TextView) findViewById(R.id.game_text);
        setImages();
        guessText = (EditText) findViewById(R.id.guess_text);
        mImageView = (ImageView) findViewById(R.id.image_hangman);
        wrongCharView = (TextView) findViewById(R.id.wrong_text);
        wrongCharView.setMovementMethod(new ScrollingMovementMethod());
        keyWordView = (TextView) findViewById(R.id.key_text);
        turnIndicator = (TextView) findViewById(R.id.turn_indicator);

        discoveryTimer = new CountDownTimer(DISCOVER_TIMEOUT, 1000) {
            @Override
            public void onTick(long untilFinished) {}

            @Override
            public void onFinish() {
                if (mState == STATE_DISCOVERING) {
                    updateViewVisibility(STATE_READY);
                    displayText("Discovery timed out");
                }
            }
        };
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
            TextView endpointName = (TextView) convertView.findViewById(R.id.endpoint_name);
            endpointName.setText(currentEndpoint.getEndpointName());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectTo(currentEndpoint.getEndpointId(), currentEndpoint.getEndpointName());
                }
            });

            return convertView;
        }
    }

    void setImages() {
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
    public void onStart() {
        super.onStart();

        AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);
        nameDialog.setTitle("Display Name");
        nameDialog.setCancelable(false);
        nameDialog.setMessage("Set a custom display name for others to see?");
                nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        mGoogleApiClient.connect();
                    }
                });

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        nameDialog.setView(input);

        nameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                if (text != "")
                    mName = text;

                mGoogleApiClient.connect();
            }
        });

        nameDialog.show();

        gameData = new HangmanData();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        notification = MediaPlayer.create(MainActivity.this, R.raw.next_turn);
        notification.setAudioStreamType(AudioManager.STREAM_MUSIC);

        success = MediaPlayer.create(MainActivity.this, R.raw.success);
        success.setAudioStreamType(AudioManager.STREAM_MUSIC);

        failure = MediaPlayer.create(MainActivity.this, R.raw.success);
        failure.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Nearby.Connections.stopAdvertising(mGoogleApiClient);
            mGoogleApiClient.disconnect();
//            mGoogleApiClient = null;
            displayText("Client disconnected");
        }

        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayText("onConnected: " + Nearby.Connections.getLocalEndpointId(mGoogleApiClient));
        updateViewVisibility(STATE_READY);
        startAdvertising();
        startDiscovery();
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

    boolean advertising = false;
    private void startAdvertising() {
        if (!isConnectedToNetwork()) {
            debugLog("startAdvertising: Not connected to network");
            displayText("Not connected to a network");
            return;
        }

         // Prompt other network devices to install the application
        List<AppIdentifier> appIdentifierList= new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        long NO_TIMEOUT = 0L;

        Nearby.Connections.startAdvertising(mGoogleApiClient, mName, appMetadata, NO_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(Connections.StartAdvertisingResult result) {
                        if (result.getStatus().isSuccess()) {
                            advertising = true;
                            debugLog("startAdvertising:onResult: SUCCESS");
//                            displayText("Started advertising");
                        } else {
                            debugLog("startAdvertising:onResult: FAILURE ");

                            int statusCode = result.getStatus().getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING){
                                debugLog("Already advertising");
                            } else {
                                advertising = false;
                                updateViewVisibility(STATE_READY);
                            }
                        }
                    }
                });
    }

    private void startDiscovery() {
        if (!isConnectedToNetwork()) {
            displayText("Not connected to a network");
            return;
        }

        String serviceId = getString(R.string.service_id);
        myEndpoints.clear();
        updateEndpointList();

        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, DISCOVER_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            debugLog("startDiscovery:onResult: SUCCESS");
                            updateViewVisibility(STATE_DISCOVERING);

                            discoveryTimer.start();
//                            if (!advertising)
                                startAdvertising();
                        } else {
                            debugLog("startDiscovery:onResult: FAILURE ");

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

        if (!msg.isEmpty()) {
            Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, msg.getBytes());
            displayText("Message sent: " + msg);
        }
    }

    private void sendMessage(byte[] payload) {
        debugLog("Sending message: " + new String(payload));
        Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, payload);
    }

    private void sendMessage(String message) {
        debugLog("Sending message: " + message);
        displayText("Sending message: " + message);
        Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, message.getBytes());
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        vibrator.vibrate(400L);
        notification.start();

        String s = new String(payload);
        debugLog("Message Received: " + s);

        if (s.charAt(0) == '0') {
            gameData.deserialize(s);
            if (gameData.mostRecent != ' ') {
                String message = mOtherEndpointName + " picked the letter " + gameData.mostRecent;
                if (gameData.wrongChars.contains(gameData.mostRecent))
                    message += " incorrectly";
                else
                    message += " correctly";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            hasTurn = true;
            checkWinOrLose();
            updateGameView();
        } else if (s.equals("restart")) {
            displayText("Game restarted");
        } else {
            mGameText.append("\nMessage from " + mOtherEndpointName + ": " + s);
        }
    }

    public void connectTo(String endpointId, final String endpointName) {
        displayText("Sending connection request to " + endpointId);

        byte[] myPayload = null;

        mIsHost = false;
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, mName, endpointId, myPayload,
                new Connections.ConnectionResponseCallback(){
                    @Override
                    public void onConnectionResponse(String endpointId, Status status, byte[] bytes) {
                        debugLog("onConnectionResponse: " + endpointId + ", " + status);
                        if (status.isSuccess()) {
                            debugLog("onConnectionResponse: " + endpointId + " SUCCESS");
                            displayText("Successfully connected to " + endpointName);

                            mOtherEndpointId = endpointId;
                            mOtherEndpointName = endpointName;
                            mIsHost = true;
                            startGame();
                            updateViewVisibility(STATE_CONNECTED);
                        } else {
                            int statusCode = status.getStatusCode();
                            debugLog("Connection request failed, error: " + statusCode);

                            if (statusCode == ConnectionsStatusCodes.STATUS_NOT_CONNECTED_TO_ENDPOINT) {
                                displayText("Connection Request failed. Remote endpoint not connected to network");
                            } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                                displayText("Connection request to " + endpointName + " rejected");
                            }
                            mIsHost = false;
                        }
                    }
                }, this);
    }

    @Override
    public void onConnectionRequest(final String endpointId, String deviceId,
                                    final String endpointName, byte[] payload) {
        debugLog("onConnectionRequest: " + endpointId + ", " + endpointName);
        vibrator.vibrate(400L);
        notification.start();

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
                                            displayText("Successfully connected to " + endpointName);
                                            mIsHost = false;
                                            mOtherEndpointId = endpointId;
                                            mOtherEndpointName = endpointName;
                                            startGame();
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
            case R.id.button_guess:
                 checkGuess();
                break;
            case R.id.button_restart:
                 restartGame();
                break;
        }
    }

    private void startGame() {
        findViewById(R.id.button_restart).setVisibility(View.GONE);
        gameData.restart();
        if (mIsHost) {
            sendMessage(gameData.serialize());
        }
        updateGameView();
    }

    private void restartGame() {
        findViewById(R.id.button_restart).setVisibility(View.GONE);
        gameData.restart();
        sendMessage(gameData.serialize());
        sendMessage("restart");
        updateGameView();
    }

    private void checkGuess() {
        if (guessText.getText().length() == 0) return;

        Character guess = Character.toLowerCase(guessText.getText().charAt(0));
        boolean alreadyGuessed = false;

        Iterator<Character> iterator = gameData.wrongChars.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(guess)) {
                alreadyGuessed = true;
            }
        }

        iterator = gameData.rightChars.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(guess)) {
                alreadyGuessed = true;
            }
        }

        if (alreadyGuessed) {
            Toast.makeText(this, guess + " has already been guessed.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            String feedback = "You picked " + guess;
            gameData.mostRecent = guess;
            if (isCorrect(guess)) {
                Toast.makeText(MainActivity.this, feedback + " correctly", Toast.LENGTH_SHORT).show();
                gameData.rightChars.add(guess);
            } else {
                Toast.makeText(MainActivity.this, feedback + " incorrectly", Toast.LENGTH_SHORT).show();
                gameData.wrongChars.add(guess);
                if (gameData.imageIndex < 10)
                    gameData.imageIndex++;
            }

            nextTurn();
        }
    }

    private boolean isCorrect(char guess) {
        String keyword = words[gameData.wordIndex];
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);

            if (c == guess)
                return true;
        }

        return false;
    }

    private void nextTurn() {
        guessText.setText("");
        hasTurn = false;
        checkWinOrLose();
        updateGameView();
        sendMessage(gameData.serialize());
    }

    void checkWinOrLose() {
        boolean lost = false;
        if (gameData.imageIndex == 10) {
            lost = true;
        }

        boolean won = true;
        String s = keyWordView.getText().toString();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '_') {
                won = false;
            }
        }

        if (won || lost) {
            findViewById(R.id.guess_text).setEnabled(false);
            findViewById(R.id.button_guess).setEnabled(false);

            String endText;
            if (won) {
                success.start();
                endText = "you won";
            }
            else {
                failure.start();
                endText = "you lost";
            }

            if (mIsHost) {
                findViewById(R.id.button_restart).setVisibility(View.VISIBLE);
            } else {
                endText += ". Waiting for other player to restart";
            }

            turnIndicator.setText(endText);
        }
    }

    private void updateViewVisibility(int newState) {
        mState = newState;

        switch(mState) {
            case STATE_IDLE:
                findViewById(R.id.game_view).setVisibility(View.GONE);
                findViewById(R.id.lobby_view).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_nearby_buttons).setVisibility(View.GONE);
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                break;
            case STATE_READY:
                findViewById(R.id.game_view).setVisibility(View.GONE);
                findViewById(R.id.lobby_view).setVisibility(View.VISIBLE);
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                findViewById(R.id.layout_nearby_buttons).setVisibility(View.VISIBLE);
//                findViewById(R.id.device_list).setVisibility(View.VISIBLE);
//                findViewById(R.id.layout_message).setVisibility(View.GONE);
                break;
            case STATE_DISCOVERING:
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                break;
            case STATE_ADVERTISING:
                break;
            case STATE_CONNECTED:
//                findViewById(R.id.layout_nearby_buttons).setVisibility(View.GONE);
//                findViewById(R.id.device_list).setVisibility(View.GONE);
//                findViewById(R.id.layout_message).setVisibility(View.VISIBLE);
                findViewById(R.id.game_view).setVisibility(View.VISIBLE);
                findViewById(R.id.lobby_view).setVisibility(View.GONE);
                break;
        }
    }

    public void debugLog(String msg) {
        mDebugInfo.append("\n" + msg);
        Log.d(TAG, msg);
    }

    public void displayText(String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void updateEndpointList() {
        endpointAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        debugLog("Endpoint found: " + endpointId +  ", " + endpointName);

        notification.start();
        myEndpoints.add(new Endpoint(endpointId, endpointName, deviceId, serviceId));
        updateEndpointList();
    }

    @Override
    public void onEndpointLost(String mOtherEndpointId) {
        // An endpoint previously available for connection is no longer there
        for (int i = 0; i < myEndpoints.size(); i++) {
            if (myEndpoints.get(i).getEndpointId() == mOtherEndpointId)
                myEndpoints.remove(i);
        }
        updateEndpointList();
        debugLog("Lost connection with endpoint: " + mOtherEndpointId);
    }

    @Override
    public void onDisconnected(String endpointId) {
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpointId);
        debugLog("onDisconnected:" + endpointId);
        displayText("Lost connection with " + mOtherEndpointName);
        updateViewVisibility(STATE_READY);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        debugLog("onConnectionFailed: " + connectionResult);
    }

    private void updateGameView() {
        mImageView.setImageResource(imageMap.get(gameData.imageIndex));

        Iterator<Character> iterator = gameData.wrongChars.iterator();
        String s = "";
        while (iterator.hasNext()) {
            Character c = iterator.next();
            s += c + ", ";
        }
        wrongCharView.setText(s);

        keyWordView.setText("");
        String keyword = words[gameData.wordIndex];
        debugLog("keyword: " + keyword);
        s = "";
        for (int i = 0; i < keyword.length(); i++) {
            Character placeholder = '_';
            Character keywordChar = keyword.charAt(i);

            iterator = gameData.rightChars.iterator();
            while (iterator.hasNext()) {
                Character c = iterator.next();
                if (c.equals(keywordChar)) {
                    placeholder = c;
                }
            }
            s += placeholder;
        }
        keyWordView.setText(s);

        if (hasTurn) {
            turnIndicator.setText("your turn to guess");
            findViewById(R.id.guess_text).setEnabled(true);
            findViewById(R.id.button_guess).setEnabled(true);
        } else {
            turnIndicator.setText("waiting for other player's turn");
            findViewById(R.id.guess_text).setEnabled(false);
            findViewById(R.id.button_guess).setEnabled(false);
        }
    }
}
