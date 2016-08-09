package com.project.imran.devicediscovery;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Administrator on 09-Aug-16.
 */
public class GameView extends GLSurfaceView {
    private final String TAG = "GameView";
    private final GameRenderer gameRenderer;

    public GameView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        gameRenderer = new GameRenderer(context);
        setRenderer(gameRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        float x = evt.getX();
        float y = evt.getY();

        Log.d(TAG, x + ", " + y);

        return super.onTouchEvent(evt);
    }
}
