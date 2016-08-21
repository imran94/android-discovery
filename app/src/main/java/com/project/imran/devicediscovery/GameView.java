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

        switch(evt.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (x < getWidth() / 2) {
                    gameRenderer.setHeroMove(gameRenderer.getHeroMove() + .1f);
                }
                if (x > getWidth() / 2) {
                    gameRenderer.setHeroMove(gameRenderer.getHeroMove() - .1f);
                }
        }

        return true;
    }
}
