/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.gxh.curlpagetest.views;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Actual renderer class.
 *
 * @author harism
 */
public class CurlRenderer implements GLSurfaceView.Renderer {

    // Constant for requesting left page rect.
    public static final int PAGE_LEFT = 1;
    // Constant for requesting right page rect.
    public static final int PAGE_RIGHT = 2;
    // Constants for changing view mode.
    public static final int SHOW_TWO_PAGES = 2;
    // Set to true for checking quickly how perspective projection looks.
    private static final boolean USE_PERSPECTIVE_PROJECTION = true;
    // Background fill color.
    private int mBackgroundColor;
    // Curl meshes used for static and dynamic rendering.
    private Vector<CurlMesh> mCurlMeshes;
    private RectF mMargins = new RectF();
    private Observer mObserver;
    // Page rectangles.
    private RectF mPageRectLeft;
    private RectF mPageRectRight;
    // Screen size.
    private int mViewportWidth, mViewportHeight;
    // Rect for render area.
    private RectF mViewRect = new RectF();

    private AlbumView albumView;

    private HardSurface first;
    private HardSurface last;

    private RectF coverRect;

    private Vector<HardSurface> page;

    public CurlRenderer(Observer observer, AlbumView albumView) {
        mObserver = observer;
        mCurlMeshes = new Vector<CurlMesh>();
        page = new Vector<HardSurface>();
        mPageRectLeft = new RectF();
        mPageRectRight = new RectF();
        coverRect = new RectF();
        this.albumView = albumView;

        first = new HardSurface();
        last = new HardSurface();
    }


    /**
     * Adds CurlMesh to this renderer.
     */
    public synchronized void addCurlMesh(CurlMesh mesh) {
        removeCurlMesh(mesh);
        mCurlMeshes.add(mesh);
    }

    public synchronized void addHardMesh(HardSurface mesh) {
        //removeHardMesh(mesh);
        page.add(mesh);
    }

    /**
     * Returns rect reserved for left or right page. Value page should be
     * PAGE_LEFT or PAGE_RIGHT.
     */
    public RectF getPageRect(int page) {
        if (page == PAGE_LEFT) {
            return mPageRectLeft;
        } else if (page == PAGE_RIGHT) {
            return mPageRectRight;
        }
        return null;
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {

        mObserver.onDrawFrame();

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();

        if (USE_PERSPECTIVE_PROJECTION) {
            gl.glTranslatef(0, 0, -6f);
        }

        if (first.isNotHaveCover() || last.isNotHaveCover()) {
            first.setCoverBitmap(albumView.getFrontCoverBitmap());
            first.setBackTextureRect(albumView.getFirstBackRect());
            first.setFrontTextureRect(albumView.getFirstFrontRect());

            last.setCoverBitmap(albumView.getBackCoverBitmap());
            last.setFrontTextureRect(albumView.getLastFrontRect());
            last.setBackTextureRect(albumView.getLastBackRect());
        } else {
            if ((this.albumView.isFirstFlipping)
                    || (!this.albumView.isFirstFlipped)) {
                //Log.i("gxh","第一页正在翻,或者第一页没翻呢");
                drawSecond(gl);
                drawPage(gl);
                drawFirst(gl);
            } else {
                if ((this.albumView.isSecondFlipped)
                        || (this.albumView.isSecondFlipping)) {
                    //Log.i("gxh","最后一页正在翻,或者最后一页翻完了");
                    drawFirst(gl);
                    drawPage(gl);
                    drawSecond(gl);
                } else {
                    drawFirst(gl);
                    drawSecond(gl);
                    drawPage(gl);
                    //Log.i("gxh","第一页翻完了,最后一页没翻");
                }
            }
        }
    }

    private void drawFirst(GL10 gl) {
        this.first.draw(gl);
    }

    private void drawSecond(GL10 gl) {
        this.last.draw(gl);
    }

    private void drawPage(GL10 gl) {
        gl.glPushMatrix();
        if (albumView.isSoft()) {
            for (int i = 0; i < mCurlMeshes.size(); ++i) {
                mCurlMeshes.get(i).onDrawFrame(gl);
            }
        } else {
            for (int i = 0; i < page.size(); ++i) {
                page.get(i).draw(gl);
            }
        }
        gl.glPopMatrix();
    }

    public void moveFirstCover(PointF pointF) {
        float angle = (float) (Math.acos(pointF.x
                / coverRect.width()) * 180 / Math.PI);
        float result = first.setRotate(0 - angle);
        float move = (float) (coverRect.left * (1 - Math.cos(result
                * Math.PI / 180)));
        first.setTransaction(move);
    }

    public void moveLastCover(PointF pointF) {
        float angle = (float) (Math.acos(pointF.x
                / coverRect.width()) * 180 / Math.PI);
        float result = last.setRotate(0 - angle);
        float move = (float) (coverRect.left * (1 - Math.cos(result
                * Math.PI / 180)));
        last.setTransaction(move);
    }

    public void setFirstAngle(float angle) {
        float move = (float) (coverRect.left * (1 - Math.cos(angle
                * Math.PI / 180)));

        first.setRotate(angle);
        first.setTransaction(move);
    }

    public void setLastAngle(float angle) {
        float move = (float) (coverRect.left * (1 - Math.cos(angle
                * Math.PI / 180)));

        last.setRotate(angle);
        last.setTransaction(move);
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        mViewportWidth = width;
        mViewportHeight = height;

        float ratio = (float) width / height;
        mViewRect.top = 1.0f;
        mViewRect.bottom = -1.0f;
        mViewRect.left = -ratio;
        mViewRect.right = ratio;
        updatePageRects();

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        if (USE_PERSPECTIVE_PROJECTION) {
            GLU.gluPerspective(gl, 20f, (float) width / height, .1f, 10f);
        } else {
            GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right,
                    mViewRect.bottom, mViewRect.top);
        }

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_CULL_FACE);

        mObserver.onSurfaceCreated();
    }

    /**
     * Removes CurlMesh from this renderer.
     */
    public synchronized void removeCurlMesh(CurlMesh mesh) {
        while (mCurlMeshes.remove(mesh)) ;
    }

    public synchronized void removeHardMesh(HardSurface hardSurface) {
        while (page.remove(hardSurface)) ;
    }

    /**
     * Change background/clear color.
     */
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    /**
     * Set margins or padding. Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    public synchronized void setMargins(float left, float top, float right,
                                        float bottom) {
        mMargins.left = left;
        mMargins.top = top;
        mMargins.right = right;
        mMargins.bottom = bottom;
        updatePageRects();
    }


    /**
     * Translates screen coordinates into view coordinates.
     */
    public void translate(PointF pt) {
        pt.x = mViewRect.left + (mViewRect.width() * pt.x / mViewportWidth);
        pt.y = mViewRect.top - (-mViewRect.height() * pt.y / mViewportHeight);
    }

    /**
     * Recalculates page rectangles.
     */
    private void updatePageRects() {
        if (mViewRect.width() == 0 || mViewRect.height() == 0)
            return;
        mPageRectRight.set(mViewRect);
        mPageRectRight.left += mViewRect.width() * mMargins.left;
        mPageRectRight.right -= mViewRect.width() * mMargins.right;
        mPageRectRight.top += mViewRect.height() * mMargins.top;
        mPageRectRight.bottom -= mViewRect.height() * mMargins.bottom;

        mPageRectLeft.set(mPageRectRight);
        mPageRectLeft.right = (mPageRectLeft.right + mPageRectLeft.left) / 2;
        mPageRectRight.left = mPageRectLeft.right;

        coverRect.set(mPageRectRight);
        coverRect.right += 0.05f;
        coverRect.top += 0.05f;
        coverRect.bottom -= 0.05f;

        first.setRect(coverRect);
        last.setRect(coverRect);


        int bitmapW = (int) ((mPageRectRight.width() * mViewportWidth) / mViewRect
                .width());
        int bitmapH = (int) ((mPageRectRight.height() * mViewportHeight) / mViewRect
                .height());
        mObserver.onPageSizeChanged(bitmapW, bitmapH);

    }

    /**
     * Observer for waiting render engine/state updates.
     */
    public interface Observer {
        /**
         * Called from onDrawFrame called before rendering is started. This is
         * intended to be used for animation purposes.
         */
        public void onDrawFrame();

        /**
         * Called once page size is changed. Width and height tell the page size
         * in pixels making it possible to update textures accordingly.
         */
        public void onPageSizeChanged(int width, int height);

        /**
         * Called from onSurfaceCreated to enable texture re-initialization etc
         * what needs to be done when this happens.
         */
        public void onSurfaceCreated();
    }
}
