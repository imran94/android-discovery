package com.project.imran.devicediscovery;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewActivity extends AppCompatActivity implements
        View.OnClickListener,
        Connections.MessageListener {

    public static boolean active = false;

    private static final String TAG = "sup";
    private GameView myGameView;
    private TextView mDebugInfo;
    private ImageView mImageView;

    public static NewActivity instance;
    private GoogleApiClient mGoogleApiClient;
    private String mOtherEndpointId;

    Map<Integer, Integer> imageMap = new HashMap<>();
    private int imageNumber;

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

        instance = new NewActivity();
        mGoogleApiClient = MainActivity.mGoogleApiClient;
        mOtherEndpointId = MainActivity.mOtherEndpointId;
    }

    private void nextImage() {
        imageNumber++;

        if (imageNumber > 10)
            imageNumber = 0;

        mImageView.setImageResource(imageMap.get(imageNumber));
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        debugLog("isConnected: " + mGoogleApiClient.isConnected());
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

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

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_send:
                nextImage();
                debugLog("Button pressed");
                break;
        }
    }

    public boolean isActive() { return active; }

    public void debugLog(String message) {
        Log.d(TAG, message);
        mDebugInfo.append(message + "\n");
    }

    public void sendMessage() {
        String msg = "Button pressed by " + mGoogleApiClient.toString();
        Nearby.Connections.sendReliableMessage(mGoogleApiClient, mOtherEndpointId, msg.getBytes());
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        debugLog("Message from " + endpointId + ": " + new String(payload));
    }

    @Override
    public void onDisconnected(String s) {
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
}
