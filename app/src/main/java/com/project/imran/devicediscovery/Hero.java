package com.project.imran.devicediscovery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Administrator on 20-Aug-16.
 */
public class Hero {

    private final static String TAG = "Hero";

    private int[] textures = new int[1];
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final FloatBuffer textureBuffer;
    private final int mProgram;

    static final int COORDS_PER_TEXTURE = 2;
    static final int COORDS_PER_VERTEX = 3;

//    private final int vertexStride = COORDS_PER_VERTEX * 4;
    public static int textureStride = COORDS_PER_TEXTURE * 4;

    static float squareCoords[] = {
            0.0f,  0.3f, 0.0f, // top
            -0.1f, 0f, 0.0f, // bottom left
            0.1f, 0f, 0.0f  // bottom right
    };

    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 };

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 TexCoordIn;" +
                    "varying vec2 TexCoordOut;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  TexCoordOut = TexCoordIn;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "uniform sampler2D TexCoordIn;" +
                    "uniform float posX;" +
                    "uniform float posY;" +
                    "varying vec2 TexCoordOut;" +
                    "void main() {" +
                        " gl_FragColor = texture2D(TexCoordIn," +
                        " vec2(TexCoordOut.x + posX ,TexCoordOut.y + posY ));" +
                    "}";

//    private final String vertexShaderCode =
//            "attribute vec4 vPosition;" +
//                    "void main() {" +
//                    "  gl_Position = vPosition;" +
//                    "}";
//
//    private final String fragmentShaderCode =
//            "precision mediump float;" +
//                    "uniform vec4 vColor;" +
//                    "void main() {" +
//                    "  gl_FragColor = vColor;" +
//                    "}";

    private float texture[] = {
            0f, 0.25f,
            0f, 0f,
            0.25f, 0f,
            0.25f, 0.25f,
    };

    public void loadTexture(int texture, Context context) {
        InputStream imagestream = context.getResources().
                openRawResource(texture);
        Bitmap bitmap = null;
        android.graphics.Matrix flip = new android.graphics.Matrix();
        flip.postScale(-1f, -1f);

        try {
            bitmap = BitmapFactory.decodeStream(imagestream);
        } catch (Exception e) {
        } finally {
            try {
                imagestream.close();
                imagestream = null;
            } catch (IOException io) {
            }
        }

        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public Hero() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                /*# of coordinate values * 4 bytes per float */
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        bb = ByteBuffer.allocateDirect(texture.length * 4);
        bb.order(ByteOrder.nativeOrder());
        textureBuffer = bb.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        // Initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = GameRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        Log.d(TAG, "Vertex shader: " + vertexShader);
        int fragmentShader = GameRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);
        Log.d(TAG, "Fragment shader: " + fragmentShader);

        // Create empty OpenGL  ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to the program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to the program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES Program executables
        GLES20.glLinkProgram(mProgram);

        validateProgram(mProgram);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    private final int vertexCount = squareCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    public void draw(float[] mvpMatrix, float posX, float posY){

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition number
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        int vsTextureCoord = GLES20.glGetAttribLocation(mProgram, "TexCoordIn");

        // prepare the shape coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // set color for drawing the shape
//        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glVertexAttribPointer(vsTextureCoord, COORDS_PER_TEXTURE,
                GLES20.GL_FLOAT, false,
                textureStride, textureBuffer);

        GLES20.glEnableVertexAttribArray(vsTextureCoord);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        int fsTexture = GLES20.glGetUniformLocation(mProgram, "TexCoordOut");
        int fsPosX = GLES20.glGetUniformLocation(mProgram, "posX");
        int fsPosY = GLES20.glGetUniformLocation(mProgram, "posY");
        GLES20.glUniform1i(fsTexture, 0);
        GLES20.glUniform1f(fsPosX, posX);
        GLES20.glUniform1f(fsPosY, posY);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GameRenderer.checkGLError("glGetUniformLocation");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GameRenderer.checkGLError("glUniformMatrix4fv");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        // draw the shape
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }


    public static boolean validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        Log.d(TAG, "Results of validating program: " + validateStatus[0]
                + "\nLog:" + GLES20.glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }
}
