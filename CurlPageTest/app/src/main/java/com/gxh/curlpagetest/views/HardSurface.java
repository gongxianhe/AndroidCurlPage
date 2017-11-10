package com.gxh.curlpagetest.views;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by fft123 on 2016/11/30.
 * //硬板首页,终于快出炉了
 */
public class HardSurface {

    private static final int VERTEX_COUNT = 2;

    private float mRotate = 0.0f; // 旋转角度
    private float mtransaction = 0.0f; // 移动距离

    private Bitmap[] coverBitmap; // 两面图片

    private FloatBuffer verticeBuffer;
    private FloatBuffer textureFrontBuffer;
    private FloatBuffer textureBackBuffer;

    private int[] textures;

    public HardSurface() {
        verticeBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticeBuffer.position(0);

        textureFrontBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureFrontBuffer.position(0);

        textureBackBuffer = ByteBuffer.allocateDirect(VERTEX_COUNT * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBackBuffer.position(0);
    }

    public boolean isNotHaveCover() {
        return coverBitmap == null;
    }

    /**
     * 设置旋转度数
     *
     * @param rotate
     */
    public float setRotate(float rotate) {
        if (Float.isNaN(rotate)) {
            return mRotate;
        }
        if (rotate < -180) {
            this.mRotate = -180;
        } else if (rotate > 0) {
            this.mRotate = 0;
        }
        this.mRotate = rotate;
        return mRotate;
    }

    /**
     * 设置旋转移动距离  解决旋转时候移动的问题
     *
     * @param transaction
     */
    public void setTransaction(float transaction) {
        this.mtransaction = transaction;
    }

    public void draw(GL10 gl) {
        gl.glPushMatrix();
        gl.glEnable(gl.GL_TEXTURE_2D);
        gl.glEnable(gl.GL_BLEND);
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
        gl.glVertexPointer(VERTEX_COUNT, gl.GL_FLOAT, 0, verticeBuffer);
        gl.glTranslatef(mtransaction, 0, 0);
        gl.glRotatef(mRotate, 0, 1, 0);
        loadTexture(gl);

        gl.glCullFace(GL10.GL_BACK);
        drawFrontFace(gl);
        gl.glCullFace(GL10.GL_FRONT);
        drawBackFace(gl);

        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_CULL_FACE);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);


        gl.glPopMatrix();

    }

    /**
     * 画后面
     *
     * @param gl
     */
    private void drawBackFace(GL10 gl) {
        if (textures != null) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);
        }
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBackBuffer);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    }

    /**
     * 画前面
     *
     * @param gl
     */
    private void drawFrontFace(GL10 gl) {
        if (textures != null) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        }
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureFrontBuffer);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    }

    /**
     * 加载纹理
     *
     * @param gl
     */
    private void loadTexture(GL10 gl) {
        if (textures == null) {
            textures = new int[2];
            gl.glGenTextures(2, textures, 0);
        }

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        for (int i = 0; i < 2; i++) {

            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[i]);

            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, coverBitmap[i], 0);
        }

    }


    /**
     * 设置矩阵
     *
     * @param coverRect
     */
    public void setRect(RectF coverRect) {
        verticeBuffer.put(coverRect.left);
        verticeBuffer.put(coverRect.bottom);
        verticeBuffer.put(coverRect.right);
        verticeBuffer.put(coverRect.bottom);
        verticeBuffer.put(coverRect.left);
        verticeBuffer.put(coverRect.top);
        verticeBuffer.put(coverRect.right);
        verticeBuffer.put(coverRect.top);
        verticeBuffer.position(0);
    }

    /**
     * 设置前页面纹理矩阵
     *
     * @param vertices
     */
    public void setFrontTextureRect(float[] vertices) {
        float left = vertices[0];
        float bottom = vertices[1];
        float right = vertices[2];
        float top = vertices[3];
        textureFrontBuffer.put(left);
        textureFrontBuffer.put(top);
        textureFrontBuffer.put(right);
        textureFrontBuffer.put(top);
        textureFrontBuffer.put(left);
        textureFrontBuffer.put(bottom);
        textureFrontBuffer.put(right);
        textureFrontBuffer.put(bottom);
        textureFrontBuffer.position(0);
    }

    /**
     * 设置前页面纹理矩阵
     *
     * @param vertices
     */
    public void setFrontTextureRect(RectF vertices) {
        float left = vertices.left;
        float bottom = vertices.top;
        float right = vertices.right;
        float top = vertices.bottom;
        textureFrontBuffer.put(left);
        textureFrontBuffer.put(top);
        textureFrontBuffer.put(right);
        textureFrontBuffer.put(top);
        textureFrontBuffer.put(left);
        textureFrontBuffer.put(bottom);
        textureFrontBuffer.put(right);
        textureFrontBuffer.put(bottom);
        textureFrontBuffer.position(0);
    }

    /**
     * 设置后页面纹理矩阵
     *
     * @param vertices
     */
    public void setBackTextureRect(float[] vertices) {
        float left = vertices[0];
        float bottom = vertices[1];
        float right = vertices[2];
        float top = vertices[3];
        textureBackBuffer.put(right);
        textureBackBuffer.put(top);
        textureBackBuffer.put(left);
        textureBackBuffer.put(top);
        textureBackBuffer.put(right);
        textureBackBuffer.put(bottom);
        textureBackBuffer.put(left);
        textureBackBuffer.put(bottom);
        textureBackBuffer.position(0);
    }

    /**
     * 设置后页面纹理矩阵
     *
     * @param vertices
     */
    public void setBackTextureRect(RectF vertices) {
        float left = vertices.left;
        float bottom = vertices.top;
        float right = vertices.right;
        float top = vertices.bottom;
        textureBackBuffer.put(right);
        textureBackBuffer.put(top);
        textureBackBuffer.put(left);
        textureBackBuffer.put(top);
        textureBackBuffer.put(right);
        textureBackBuffer.put(bottom);
        textureBackBuffer.put(left);
        textureBackBuffer.put(bottom);
        textureBackBuffer.position(0);
    }

    /**
     * 设置图片
     *
     * @param bitmap
     */
    public void setCoverBitmap(Bitmap[] bitmap) {
        if (bitmap != null) {
            coverBitmap = new Bitmap[2];
            this.coverBitmap = bitmap;
        }
    }


}
