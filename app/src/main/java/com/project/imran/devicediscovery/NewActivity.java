package com.project.imran.devicediscovery;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class NewActivity extends AppCompatActivity {

    private static final String TAG = "sup";
    private GameView myGameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myGameView = new GameView(this);
        setContentView(myGameView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // De-allocate significant memory-consuming objects here

        myGameView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-allocate objects de-allocated in onPause() here

        myGameView.onResume();
    }


}
