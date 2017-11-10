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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

/**
 * OpenGL ES View.
 *
 * @author harism
 */
public class AlbumView extends GLSurfaceView implements View.OnTouchListener,
        CurlRenderer.Observer {

    // Curl state. We are flipping none, left or right page.
    private static final int CURL_LEFT = 1;
    private static final int CURL_NONE = 0;
    private static final int CURL_RIGHT = 2;

    // Constants for mAnimationTargetEvent.
    private static final int SET_CURL_TO_LEFT = 1;
    private static final int SET_CURL_TO_RIGHT = 2;

    // Shows one page at the center of view.
    public static final int SHOW_ONE_PAGE = 1;
    // Shows two pages side by side.
    public static final int SHOW_TWO_PAGES = 2;

    private boolean mAllowLastPageCurl = true;
    public boolean isFirstFlipped = false;
    public boolean isFirstFlipping = false;

    public boolean isSecondFlipped = false;
    public boolean isSecondFlipping = false;

    public boolean isPageFlipping = false;

    public boolean is_soft = false;

    private boolean mAnimate = false;
    private long mAnimationDurationTime = 300;
    private PointF mAnimationSource = new PointF();
    private long mAnimationStartTime;
    private PointF mAnimationTarget = new PointF();
    private int mAnimationTargetEvent;

    private PointF mCurlDir = new PointF();

    private PointF mCurlPos = new PointF();
    private int mCurlState = CURL_NONE;
    // Current bitmap index. This is always showed as front of right page.
    private int mCurrentIndex = 0;

    // Start position for dragging.
    private PointF mDragStartPos = new PointF();

    private boolean mEnableTouchPressure = false;
    // Bitmap size. These are updated from renderer once it's initialized.
    private int mPageBitmapHeight = -1;

    private int mPageBitmapWidth = -1;
    // Page meshes. Left and right meshes are 'static' while curl is used to
    // show page flipping.
    //软翻页
    private CurlMesh mPageCurl;
    private CurlMesh mPageLeft;
    private CurlMesh mPageRight;

    private PageProvider mPageProvider;

    private HardSurface pageCurl;
    private HardSurface pageLeft;
    private HardSurface pageRight;

    private PointerPosition mPointerPos = new PointerPosition();

    private CurlRenderer mRenderer;
    private boolean mRenderLeftPage = true;
    private SizeChangedObserver mSizeChangedObserver;

    // One page is the default.
    private int mViewMode = SHOW_TWO_PAGES;

    private Bitmap[] frontBitmap = new Bitmap[2];
    private Bitmap[] backBitmap = new Bitmap[2];

    private float[] firstFrontRect = new float[4];
    private float[] firstBackRect = new float[4];

    private float[] lastFrontRect = new float[4];
    private float[] lastBackRect = new float[4];

    private AlbumProgressBar progressBar;

    private OnPageClickListener onPageClickListener;

    private OnFlipedLastPageListener onFlipedLastPageListener;

    private VelocityTracker mVelocityTracker = null;

    /**
     * Default constructor.
     */
    public AlbumView(Context ctx) {
        super(ctx);
        init(ctx);
    }

    /**
     * Default constructor.
     */
    public AlbumView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx);
    }

    /**
     * Default constructor.
     */
    public AlbumView(Context ctx, AttributeSet attrs, int defStyle) {
        this(ctx, attrs);
    }

    /**
     * Get current page index. Page indices are zero based values presenting
     * page being shown on right side of the book.
     */
    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void setAlbumProgressBar(AlbumProgressBar albumProgressBar) {
        this.progressBar = albumProgressBar;
    }

    public void setOnPageClickListener(OnPageClickListener onPageClickListener) {
        this.onPageClickListener = onPageClickListener;
    }

    public void setOnFlipedLastPageListener(OnFlipedLastPageListener onFlipedLastPageListener) {
        this.onFlipedLastPageListener = onFlipedLastPageListener;
    }

    public boolean isSoft() {
        return is_soft;
    }

    /**
     * 初始化  大兄弟
     */
    public void init(Context ctx) {
        this.setZOrderOnTop(true);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mRenderer = new CurlRenderer(this, this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setOnTouchListener(this);

        // Even though left and right pages are static we have to allocate room
        // for curl on them too as we are switching meshes. Another way would be
        // to swap texture ids only.
        mPageLeft = new CurlMesh(10);
        mPageRight = new CurlMesh(10);
        mPageCurl = new CurlMesh(10);

        pageCurl = new HardSurface();
        pageRight = new HardSurface();
        pageLeft = new HardSurface();

        mPageLeft.setFlipTexture(true);
        mPageRight.setFlipTexture(false);
    }

    public void setFirstFrontRect(float[] frontRect) {

        this.firstFrontRect = frontRect;
    }

    public void setFirstBackRect(float[] backRect) {
        this.firstBackRect = backRect;
    }

    public void setLastFrontRect(float[] frontRect) {
        this.lastFrontRect = frontRect;
    }

    public void setLastBackRect(float[] backRect) {
        this.lastBackRect = backRect;
    }


    public float[] getFirstBackRect() {
        return this.firstBackRect;
    }

    public float[] getFirstFrontRect() {
        return this.firstFrontRect;
    }

    public float[] getLastFrontRect() {
        return this.lastFrontRect;
    }

    public float[] getLastBackRect() {
        return this.lastBackRect;
    }


    public Bitmap[] getFrontCoverBitmap() {
        if (this.mPageProvider != null) {
            return this.mPageProvider.getFrontBitmap();
        }
        return null;
    }

    public Bitmap[] getBackCoverBitmap() {
        if (this.mPageProvider != null) {
            return this.mPageProvider.getBackBitmap();
        }
        return null;
    }


    @Override
    public void onDrawFrame() {
        // We are not animating.
        if (mAnimate == false || !is_soft) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        // If animation is done.
        if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
            if (mAnimationTargetEvent == SET_CURL_TO_RIGHT) {
                // Switch curled page to right.
                CurlMesh right = mPageCurl;
                CurlMesh curl = mPageRight;
                right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                right.setFlipTexture(false);
                right.reset();
                mRenderer.removeCurlMesh(curl);
                mPageCurl = curl;
                mPageRight = right;
                // If we were curling left page update current index.
                if (mCurlState == CURL_LEFT) {
                    --mCurrentIndex;
                }
            } else if (mAnimationTargetEvent == SET_CURL_TO_LEFT) {
                // Switch curled page to left.
                CurlMesh left = mPageCurl;
                CurlMesh curl = mPageLeft;
                left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                left.setFlipTexture(true);
                left.reset();
                mRenderer.removeCurlMesh(curl);
                if (!mRenderLeftPage) {
                    mRenderer.removeCurlMesh(left);
                }
                mPageCurl = curl;
                mPageLeft = left;
                // If we were curling right page update current index.
                if (mCurlState == CURL_RIGHT) {
                    ++mCurrentIndex;
                }
            }
            if(this.progressBar != null) {
                this.progressBar.setCurrentIndex(mCurrentIndex + 2);
            }
            mCurlState = CURL_NONE;
            mAnimate = false;
            requestRender();
        } else {
            mPointerPos.mPos.set(mAnimationSource);
            float t = 1f - ((float) (currentTime - mAnimationStartTime) / mAnimationDurationTime);
            t = 1f - (t * t * t * (3 - 2 * t));
            mPointerPos.mPos.x += (mAnimationTarget.x - mAnimationSource.x) * t;
            mPointerPos.mPos.y += (mAnimationTarget.y - mAnimationSource.y) * t;
            updateCurlPos(mPointerPos);
        }
    }

    @Override
    public void onPageSizeChanged(int width, int height) {
        mPageBitmapWidth = width;
        mPageBitmapHeight = height;
        updatePages();
        requestRender();
    }

    @Override
    public void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        try {
            requestRender();
        } catch (Exception e) {

        }
        if (mSizeChangedObserver != null) {
            mSizeChangedObserver.onSizeChanged(w, h);
        }
    }

    @Override
    public void onSurfaceCreated() {
        // In case surface is recreated, let page meshes drop allocated texture
        // ids and ask for new ones. There's no need to set textures here as
        // onPageSizeChanged should be called later on.
        mPageLeft.resetTexture();
        mPageRight.resetTexture();
        mPageCurl.resetTexture();
    }


    public boolean isOnRect(RectF paramRectF, PointF paramPointF) {
        return (paramPointF.x > paramRectF.left)
                && (paramPointF.x <= paramRectF.right)
                && (paramPointF.y > paramRectF.bottom)
                && (paramPointF.y <= paramRectF.top);
    }

    public boolean canFlipFirstCover() {
        return (this.isFirstFlipped
                && (isOnRect(this.mRenderer.getPageRect(CURL_LEFT),
                this.mPointerPos.mPos)) && this.mCurrentIndex == 0);
    }

    public boolean canFlipLastCover() {
        return ((this.getCurrentIndex() == mPageProvider.getPageCount() && (isOnRect(this.mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), this.mPointerPos.mPos)) && !isSecondFlipped) || (this.isSecondFlipped && (isOnRect(this.mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), this.mPointerPos.mPos))));
    }


    float downX;
    float downY;
    long lastDownTime;
    boolean isMove;
    boolean isLong;

    @Override
    public boolean onTouch(View view, MotionEvent me) {
        // No dragging during animation at the moment.
        // TODO: Stop animation on touch event and return to drag mode.
        if (mAnimate || mPageProvider == null) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = mVelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(me);

        // We need page rects quite extensively so get them for later use.
        RectF rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
        RectF leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);

        // Store pointer position.
        mPointerPos.mPos.set(me.getX(), me.getY());
        mRenderer.translate(mPointerPos.mPos);
        if (mEnableTouchPressure) {
            mPointerPos.mPressure = me.getPressure();
        } else {
            mPointerPos.mPressure = 0.8f;
        }

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                // Once we receive pointer down eventits position is mapped to
                // right or left edge of page and that'll be the position from where
                // user is holding the paper to make curl happen.
                mDragStartPos.set(mPointerPos.mPos);

                // First we make sure it's not over or below page. Pages are
                // supposed to be same height so it really doesn't matter do we use
                // left or right one.
                if (mDragStartPos.y > rightRect.top) {
                    mDragStartPos.y = rightRect.top;
                } else if (mDragStartPos.y < rightRect.bottom) {
                    mDragStartPos.y = rightRect.bottom;
                }

                // Then we have to make decisions for the user whether curl is going
                // to happen from left or right, and on which page.
                // If we have an open book and pointer is on the left from right
                // page we'll mark drag position to left edge of left page.
                // Additionally checking mCurrentIndex is higher than zero tells
                // us there is a visible page at all.
                if (isFirstFlipped && !isSecondFlipped) {
                    if (mDragStartPos.x < rightRect.left && mCurrentIndex > 0 && !mAnimate) {
                        mDragStartPos.x = leftRect.left;
                        startCurl(CURL_LEFT);
                        downX = me.getX();
                        downY = me.getY();
                        lastDownTime = me.getEventTime();
                    }
                    // Otherwise check pointer is on right page's side.
                    else if (mDragStartPos.x >= rightRect.left
                            && mCurrentIndex < mPageProvider.getPageCount() && !mAnimate) {
                        mDragStartPos.x = rightRect.right;
                        if (!mAllowLastPageCurl
                                && mCurrentIndex >= mPageProvider.getPageCount() - 1) {
                            return false;
                        }
                        startCurl(CURL_RIGHT);
                        downX = me.getX();
                        downY = me.getY();
                        lastDownTime = me.getEventTime();
                    }
                }
                // If we have are in curl state, let this case clause flow through
                // to next one. We have pointer position and drag position defined
                // and this will create first render request given these points.
                // if (mCurlState == CURL_NONE) {
                // return false;
                // }
            }
            case MotionEvent.ACTION_MOVE: {

                if (isFirstFlipped) {
                    if ((canFlipFirstCover() || this.isFirstFlipping) && !isPageFlipping) {
                        mRenderer.moveFirstCover(mPointerPos.mPos);
                        requestRender();
                        isFirstFlipping = true;
                    } else {
                        if (((canFlipLastCover()) || isSecondFlipping) && !isPageFlipping) {
                            mRenderer.moveLastCover(mPointerPos.mPos);
                            requestRender();
                            isSecondFlipping = true;
                        } else {
                            isPageFlipping = true;
                            if (Math.abs(me.getX() - downX) > 10 && !mAnimate) {
                                isMove = true;
                                updateCurlPos(mPointerPos);
                            }
                        }
                    }
                } else {
                    if (isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos) || isFirstFlipping) {
                        mRenderer.moveFirstCover(mPointerPos.mPos);
                        isFirstFlipping = true;
                        requestRender();
                    }

                }

                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (isFirstFlipping) {
                    float a = (float) (Math.acos(mPointerPos.mPos.x
                            / rightRect.width()) * 180 / Math.PI);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    if (((0 - a) < -90)) {
                        if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() > 1000)) {
                            smoothFirst((int) (0 - a), false);
                        } else {
                            smoothFirst((int) (0 - a), true);
                        }
                    } else {
                        if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() < -1000)) {
                            smoothFirst((int) (0 - a), true);
                        } else {
                            smoothFirst((int) (0 - a), false);
                        }
                    }

                } else {
                    if (isSecondFlipping) {
                        float a = (float) (Math.acos(mPointerPos.mPos.x
                                / rightRect.width()) * 180 / Math.PI);
                        mVelocityTracker.computeCurrentVelocity(1000);
                        if ((0 - a) < -90) {
                            if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() > 1000)) {
                                smoothLast((int) (0 - a), false);
                            } else {
                                smoothLast((int) (0 - a), true);
                            }
                        } else {
                            if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() < -1000)) {
                                smoothLast((int) (0 - a), true);
                            } else {
                                smoothLast((int) (0 - a), false);
                            }
                        }

                    } else {
                        if ((mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) && isPageFlipping) {
                            // Animation source is the point from where animation
                            // starts.
                            // Also it's handled in a way we actually simulate touch
                            // events
                            // meaning the output is exactly the same as if user drags
                            // the
                            // page to other side. While not producing the best looking
                            // result (which is easier done by altering curl position
                            // and/or
                            // direction directly), this is done in a hope it made code
                            // a
                            // bit more readable and easier to maintain.
                            int clickCurrentIndex = 0;
                            boolean isClickFront = true;
                            if (mCurrentIndex != 0 && isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), mPointerPos.mPos)) {
                                isClickFront = false;
                                clickCurrentIndex = mCurrentIndex - 1;
                            } else if (mCurrentIndex != mPageProvider.getPageCount() && isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos)) {
                                isClickFront = true;
                                clickCurrentIndex = mCurrentIndex;
                            }
                            if (!isMove && onPageClickListener.validEnableClick(clickCurrentIndex, isClickFront)) {
                                if (mCurrentIndex != 0 && !(mCurrentIndex == 1 && isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), mPointerPos.mPos)) && !(mCurrentIndex == mPageProvider.getPageCount() && isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos))) {
                                    if (onPageClickListener != null) {
                                        onPageClickListener.onClickPage(clickCurrentIndex, isClickFront);
                                    }
                                }
                            } else {
                                if (is_soft) {

                                    mAnimationSource.set(mPointerPos.mPos);
                                    mAnimationStartTime = System.currentTimeMillis();

                                    mVelocityTracker.computeCurrentVelocity(1000);
                                    // Given the explanation, here we decide whether to simulate
                                    // drag to left or right end.
                                    if (mPointerPos.mPos.x > rightRect.left) {
                                        // On right side target is always right page's right
                                        // border.

                                        if ((mCurlState == CURL_RIGHT && mVelocityTracker.getXVelocity() < -1000)) {
                                            mAnimationTarget.set(mDragStartPos);
                                            if (mCurlState == CURL_RIGHT
                                                    || mViewMode == SHOW_TWO_PAGES) {
                                                mAnimationTarget.x = leftRect.left;
                                            } else {
                                                mAnimationTarget.x = rightRect.left;
                                            }
                                            mAnimationTargetEvent = SET_CURL_TO_LEFT;
                                        } else {
                                            mAnimationTarget.set(mDragStartPos);
                                            mAnimationTarget.x = mRenderer
                                                    .getPageRect(CurlRenderer.PAGE_RIGHT).right;
                                            mAnimationTargetEvent = SET_CURL_TO_RIGHT;
                                        }
                                    } else {
                                        // On left side target depends on visible pages.

                                        if ((mCurlState == CURL_LEFT && mVelocityTracker.getXVelocity() > 1000)) {
                                            mAnimationTarget.set(mDragStartPos);
                                            mAnimationTarget.x = mRenderer
                                                    .getPageRect(CurlRenderer.PAGE_RIGHT).right;
                                            mAnimationTargetEvent = SET_CURL_TO_RIGHT;
                                        } else {

                                            mAnimationTarget.set(mDragStartPos);
                                            if (mCurlState == CURL_RIGHT
                                                    || mViewMode == SHOW_TWO_PAGES) {
                                                mAnimationTarget.x = leftRect.left;
                                            } else {
                                                mAnimationTarget.x = rightRect.left;
                                            }
                                            mAnimationTargetEvent = SET_CURL_TO_LEFT;
                                        }
                                    }
                                    mAnimate = true;
                                    requestRender();
                                } else {
                                    float a = (float) (Math.acos(mPointerPos.mPos.x
                                            / rightRect.width()) * 180 / Math.PI);
                                    mVelocityTracker.computeCurrentVelocity(1000);
                                    if (((0 - a) < -90)) {
                                        if (Float.isNaN(0 - a)) {
                                            a = 180;
                                        }
                                        if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() > 1000)) {
                                            smoothPage((int) (0 - a), false);
                                        } else {
                                            Log.i("gxh", (0 - a) + "");
                                            smoothPage((int) (0 - a), true);
                                        }
                                    } else {
                                        if (Float.isNaN(0 - a)) {
                                            a = 180;
                                        }
                                        if ((isOnRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT), mPointerPos.mPos) && mVelocityTracker.getXVelocity() < -1000)) {
                                            Log.i("gxh", (0 - a) + "");
                                            smoothPage((int) (0 - a), true);
                                        } else {
                                            Log.i("gxh", (0 - a) + "");
                                            smoothPage((int) (0 - a), false);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                isMove = false;
                isPageFlipping = false;
                break;
            }

        }

        return true;
    }

    public void smoothFirst(final int current, final boolean isFlip) {

        new Thread() {
            @Override
            public void run() {
                int position = 0;
                int temp = current;
                try {
                    if (isFlip) {
                        while (position >= -180) {
                            temp -= 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            firsthandler.sendMessage(m);
                        }

                    } else {
                        while (position <= 0) {
                            temp += 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            firsthandler.sendMessage(m);
                        }
                    }


                    if (position + 1 == -180) {
                        isFirstFlipped = true;
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progressBar != null) {
                                    progressBar.setCurrentIndex(2);
                                }
                            }
                        });

                    } else if (position - 1 == 0) {
                        isFirstFlipped = false;
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progressBar != null) {
                                    progressBar.setCurrentIndex(1);
                                }
                            }
                        });

                    }

                    isFirstFlipping = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void smoothPage(final int current, final boolean isFlip) {
        mAnimate = true;
        new Thread() {
            @Override
            public void run() {
                int position = 0;
                int temp = current;
                try {
                    if (isFlip) {
                        while (position >= -180) {
                            temp -= 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            if (mCurlState == CURL_RIGHT) {
                                m.what = 1;
                            } else {
                                m.what = 0;
                            }
                            pagehandler.sendMessage(m);
                        }

                    } else {
                        while (position <= 0) {
                            temp += 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            if (mCurlState == CURL_LEFT) {
                                m.what = 1;
                            } else {
                                m.what = 0;
                            }
                            pagehandler.sendMessage(m);
                        }
                    }


                    if (position + 1 == -180) {
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

//                                progressBar.setCurrentIndex(2);
                            }
                        });

                    } else if (position - 1 == 0) {
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                progressBar.setCurrentIndex(1);
                            }
                        });
                    }
//
//                    isFirstFlipping = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void smoothLast(final int current, final boolean isFlip) {

        new Thread() {
            @Override
            public void run() {
                int position = 0;
                int temp = current;
                try {
                    if (isFlip) {
                        while (position >= -180) {
                            temp -= 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            lasthandler.sendMessage(m);
                        }

                    } else {
                        while (position <= 0) {
                            temp += 1;
                            position = temp;
                            Thread.sleep(5);
                            Message m = new Message();
                            m.obj = position;
                            lasthandler.sendMessage(m);
                        }
                    }

                    if (position + 1 == -180) {
                        isSecondFlipped = true;
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progressBar != null) {
                                    progressBar.setCurrentIndex(mPageProvider.getPageCount() + 2 + 1);
                                }
                                if (onFlipedLastPageListener != null) {
                                    onFlipedLastPageListener.onFlippedLastPage();
                                }
                            }
                        });

                    } else if (position - 1 == 0) {
                        isSecondFlipped = false;
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(progressBar != null) {
                                    progressBar.setCurrentIndex(mPageProvider.getPageCount() + 2);
                                }
                            }
                        });

                    }

                    isSecondFlipping = false;


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    Handler firsthandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int angle = (int) msg.obj;
            mRenderer.setFirstAngle(angle);
            requestRender();
        }
    };

    Handler pagehandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int angle = (int) msg.obj - 1;
            float result = pageCurl.setRotate(angle);
            float move = (float) (mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left * (1 - Math.cos(result
                    * Math.PI / 180)));
            pageCurl.setTransaction(move);
            if (angle == -180) {
                if (msg.what == 1) {
                    mCurrentIndex++;
                    if(progressBar != null) {
                        progressBar.setCurrentIndex(mCurrentIndex + 2);
                    }
                }


                HardSurface left = pageCurl;
                HardSurface curl = pageLeft;

                mRenderer.removeHardMesh(curl);
                pageCurl = curl;
                pageLeft = left;
                mCurlState = CURL_NONE;
                mAnimate = false;
//                move = (float) (mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left * (1 - Math.cos(-180
//                        * Math.PI / 180)));
//                pageLeft.setTransaction(move);
            }
            if (angle + 2 == 0) {
                if (msg.what == 1) {
                    mCurrentIndex--;
                    if(progressBar != null) {
                        progressBar.setCurrentIndex(mCurrentIndex + 2);
                    }
                }

                HardSurface right = pageCurl;
                HardSurface curl = pageRight;

                mRenderer.removeHardMesh(curl);

                pageCurl = curl;
                pageRight = right;
                pageRight.setRotate(0);
                mCurlState = CURL_NONE;
                mAnimate = false;
            }
            requestRender();

        }
    };

    Handler lasthandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int angle = (int) msg.obj;
            mRenderer.setLastAngle(angle);
            requestRender();
        }
    };

    /**
     * Allow the last page to curl.
     */
    public void setAllowLastPageCurl(boolean allowLastPageCurl) {
        mAllowLastPageCurl = allowLastPageCurl;
    }

    /**
     * Sets background color - or OpenGL clear color to be more precise. Color
     * is a 32bit value consisting of 0xAARRGGBB and is extracted using
     * android.graphics.Color eventually.
     */
    @Override
    public void setBackgroundColor(int color) {
        mRenderer.setBackgroundColor(color);
        requestRender();
    }

    /**
     * Sets mPageCurl curl position.
     */
    private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {

        // First reposition curl so that page doesn't 'rip off' from book.
        if (mCurlState == CURL_RIGHT
                || (mCurlState == CURL_LEFT && mViewMode == SHOW_ONE_PAGE)) {
            RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
            if (curlPos.x >= pageRect.right) {
                mPageCurl.reset();
                requestRender();
                return;
            }
            if (curlPos.x < pageRect.left) {
                curlPos.x = pageRect.left;
            }
            if (curlDir.y != 0) {
                float diffX = curlPos.x - pageRect.left;
                float leftY = curlPos.y + (diffX * curlDir.x / curlDir.y);
                if (curlDir.y < 0 && leftY < pageRect.top) {
                    curlDir.x = curlPos.y - pageRect.top;
                    curlDir.y = pageRect.left - curlPos.x;
                } else if (curlDir.y > 0 && leftY > pageRect.bottom) {
                    curlDir.x = pageRect.bottom - curlPos.y;
                    curlDir.y = curlPos.x - pageRect.left;
                }
            }
        } else if (mCurlState == CURL_LEFT) {
            RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
            if (curlPos.x <= pageRect.left) {
                mPageCurl.reset();
                requestRender();
                return;
            }
            if (curlPos.x > pageRect.right) {
                curlPos.x = pageRect.right;
            }
            if (curlDir.y != 0) {
                float diffX = curlPos.x - pageRect.right;
                float rightY = curlPos.y + (diffX * curlDir.x / curlDir.y);
                if (curlDir.y < 0 && rightY < pageRect.top) {
                    curlDir.x = pageRect.top - curlPos.y;
                    curlDir.y = curlPos.x - pageRect.right;
                } else if (curlDir.y > 0 && rightY > pageRect.bottom) {
                    curlDir.x = curlPos.y - pageRect.bottom;
                    curlDir.y = pageRect.right - curlPos.x;
                }
            }
        }

        // Finally normalize direction vector and do rendering.
        double dist = Math.sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y);
        if (dist != 0) {
            curlDir.x /= dist;
            curlDir.y /= dist;
            mPageCurl.curl(curlPos, curlDir, radius);
        } else {
            mPageCurl.reset();
        }

        requestRender();
    }

    /**
     * Set current page index. Page indices are zero based values presenting
     * page being shown on right side of the book. E.g if you set value to 4;
     * right side front facing bitmap will be with index 4, back facing 5 and
     * for left side page index 3 is front facing, and index 2 back facing (once
     * page is on left side it's flipped over).
     * <p/>
     * Current index is rounded to closest value divisible with 2.
     */
    public void setCurrentIndex(int index) {

        if (mPageProvider == null || index < 0) {
            mCurrentIndex = 0;
        } else {
            if (mAllowLastPageCurl) {
                mCurrentIndex = Math.min(index, mPageProvider.getPageCount());
            } else {
                mCurrentIndex = Math.min(index,
                        mPageProvider.getPageCount() - 1);
            }
        }

        if (index != 0) {
            mRenderer.setFirstAngle(-180f);
            isFirstFlipped = true;
            if(this.progressBar != null) {
                this.progressBar.setCurrentIndex(mCurrentIndex + 2);
            }
        }
        updatePages();
        requestRender();
    }

    /**
     * If set to true, touch event pressure information is used to adjust curl
     * radius. The more you press, the flatter the curl becomes. This is
     * somewhat experimental and results may vary significantly between devices.
     * On emulator pressure information seems to be flat 1.0f which is maximum
     * value and therefore not very much of use.
     */
    public void setEnableTouchPressure(boolean enableTouchPressure) {
        mEnableTouchPressure = enableTouchPressure;
    }

    /**
     * Set margins (or padding). Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    public void setMargins(float left, float top, float right, float bottom) {
        try {
            mRenderer.setMargins(left, top, right, bottom);
        } catch (Exception e) {

        }
    }

    /**
     * Update/set page provider.
     */
    public void setPageProvider(PageProvider pageProvider, boolean is_soft) {
        mPageProvider = pageProvider;
        this.is_soft = is_soft;
        mCurrentIndex = 0;
        updatePages();
        requestRender();
    }

    public void setPageProvider(PageProvider pageProvider) {
        mPageProvider = pageProvider;
        this.is_soft = true;
        mCurrentIndex = 0;
        updatePages();
        requestRender();
    }

    /**
     * Setter for whether left side page is rendered. This is useful mostly for
     * situations where right (main) page is aligned to left side of screen and
     * left page is not visible anyway.
     */
    public void setRenderLeftPage(boolean renderLeftPage) {
        mRenderLeftPage = renderLeftPage;
    }

    /**
     * Sets SizeChangedObserver for this View. Call back method is called from
     * this View's onSizeChanged method.
     */
    public void setSizeChangedObserver(SizeChangedObserver observer) {
        mSizeChangedObserver = observer;
    }

    /**
     * Switches meshes and loads new bitmaps if available. Updated to support 2
     * pages in landscape
     */
    private void startCurl(int page) {
        switch (page) {

            // Once right side page is curled, first right page is assigned into
            // curled page. And if there are more bitmaps available new bitmap is
            // loaded into right side mesh.
            case CURL_RIGHT: {
                if (is_soft) {
                    // Remove meshes from renderer.
                    mRenderer.removeCurlMesh(mPageLeft);
                    mRenderer.removeCurlMesh(mPageRight);
                    mRenderer.removeCurlMesh(mPageCurl);

                    // We are curling right page.
                    CurlMesh curl = mPageRight;
                    mPageRight = mPageCurl;
                    mPageCurl = curl;

                    if (mCurrentIndex > 0) {
                        mPageLeft.setFlipTexture(true);
                        mPageLeft
                                .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                        mPageLeft.reset();
                        if (mRenderLeftPage) {
                            mRenderer.addCurlMesh(mPageLeft);
                        }
                    }
                    if (mCurrentIndex < mPageProvider.getPageCount() - 1) {
                        updatePage(mPageRight.getTexturePage(), mCurrentIndex + 1);
                        mPageRight.setRect(mRenderer
                                .getPageRect(CurlRenderer.PAGE_RIGHT));
                        mPageRight.setFlipTexture(false);
                        mPageRight.reset();
                        mRenderer.addCurlMesh(mPageRight);
                    }

                    // Add curled page to renderer.
                    mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                    mPageCurl.setFlipTexture(false);
                    mPageCurl.reset();
                    mRenderer.addCurlMesh(mPageCurl);

                    mCurlState = CURL_RIGHT;
                    break;
                } else {
                    mRenderer.removeHardMesh(pageLeft);
                    mRenderer.removeHardMesh(pageRight);
                    mRenderer.removeHardMesh(pageCurl);

                    // We are curling right page.
                    HardSurface curl = pageRight;
                    pageRight = pageCurl;
                    pageCurl = curl;

                    if (mCurrentIndex > 0) {
                        pageLeft
                                .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                        if (mRenderLeftPage) {
                            mRenderer.addHardMesh(pageLeft);
                        }
                    }
                    if (mCurrentIndex < mPageProvider.getPageCount() - 1) {
                        CurlPage tempRight = new CurlPage();
                        mPageProvider.updatePage(tempRight, mPageBitmapWidth, mPageBitmapHeight,
                                mCurrentIndex + 1);
                        RectF frontRect = new RectF();
                        Bitmap tempFront = tempRight.getTexture(frontRect, CurlPage.SIDE_FRONT);
                        RectF backRect = new RectF();
                        Bitmap tempBack = tempRight.getTexture(backRect, CurlPage.SIDE_BACK);

                        pageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                        pageRight.setFrontTextureRect(frontRect);
                        pageRight.setBackTextureRect(backRect);
                        Bitmap[] tempBitmaps = new Bitmap[2];
                        tempBitmaps[0] = tempFront;
                        tempBitmaps[1] = tempBack;
                        pageRight.setCoverBitmap(tempBitmaps);
                        pageRight.setRotate(0);
                        pageRight.setTransaction(0);
                        mRenderer.addHardMesh(pageRight);
                    }

                    pageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                    mRenderer.addHardMesh(pageCurl);

                    mCurlState = CURL_RIGHT;
                    break;
                }
            }

            // On left side curl, left page is assigned to curled page. And if
            // there are more bitmaps available before currentIndex, new bitmap
            // is loaded into left page.
            case CURL_LEFT: {
                if (is_soft) {
                    // Remove meshes from renderer.
                    mRenderer.removeCurlMesh(mPageLeft);
                    mRenderer.removeCurlMesh(mPageRight);
                    mRenderer.removeCurlMesh(mPageCurl);

                    // We are curling left page.
                    CurlMesh curl = mPageLeft;
                    mPageLeft = mPageCurl;
                    mPageCurl = curl;

                    if (mCurrentIndex > 1) {
                        updatePage(mPageLeft.getTexturePage(), mCurrentIndex - 2);
                        mPageLeft.setFlipTexture(true);
                        mPageLeft
                                .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                        mPageLeft.reset();
                        if (mRenderLeftPage) {
                            mRenderer.addCurlMesh(mPageLeft);
                        }
                    }

                    // If there is something to show on right page add it to renderer.
                    if (mCurrentIndex < mPageProvider.getPageCount()) {
                        mPageRight.setFlipTexture(false);
                        mPageRight.setRect(mRenderer
                                .getPageRect(CurlRenderer.PAGE_RIGHT));
                        mPageRight.reset();
                        mRenderer.addCurlMesh(mPageRight);
                    }

                    // How dragging previous page happens depends on view mode.
                    if (mViewMode == SHOW_ONE_PAGE
                            || (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {
                        mPageCurl.setRect(mRenderer
                                .getPageRect(CurlRenderer.PAGE_RIGHT));
                        mPageCurl.setFlipTexture(false);
                    } else {
                        mPageCurl
                                .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                        mPageCurl.setFlipTexture(true);
                    }
                    mPageCurl.reset();
                    mRenderer.addCurlMesh(mPageCurl);

                    mCurlState = CURL_LEFT;
                    break;
                } else {
                    // Remove meshes from renderer.
                    mRenderer.removeHardMesh(pageLeft);
                    mRenderer.removeHardMesh(pageRight);
                    mRenderer.removeHardMesh(pageCurl);

                    // We are curling left page.
                    HardSurface curl = pageLeft;
                    pageLeft = pageCurl;
                    pageCurl = curl;

                    if (mCurrentIndex > 1) {
                        CurlPage tempLeft = new CurlPage();
                        mPageProvider.updatePage(tempLeft, mPageBitmapWidth, mPageBitmapHeight,
                                mCurrentIndex - 2);
                        RectF frontRect = new RectF();
                        Bitmap tempFront = tempLeft.getTexture(frontRect, CurlPage.SIDE_FRONT);
                        RectF backRect = new RectF();
                        Bitmap tempBack = tempLeft.getTexture(backRect, CurlPage.SIDE_BACK);

                        pageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));

                        pageLeft.setFrontTextureRect(frontRect);
                        pageLeft.setBackTextureRect(backRect);
                        Bitmap[] tempBitmaps = new Bitmap[2];
                        tempBitmaps[0] = tempFront;
                        tempBitmaps[1] = tempBack;
                        pageLeft.setCoverBitmap(tempBitmaps);
                        pageLeft.setRotate(-180);
                        float move = (float) (mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left * (1 - Math.cos(-180
                                * Math.PI / 180)));
                        pageLeft.setTransaction(move);
                        if (mRenderLeftPage) {
                            mRenderer.addHardMesh(pageLeft);
                        }
                    }

                    // If there is something to show on right page add it to renderer.
                    if (mCurrentIndex < mPageProvider.getPageCount()) {
                        mPageRight.setRect(mRenderer
                                .getPageRect(CurlRenderer.PAGE_RIGHT));
                        mRenderer.addHardMesh(pageRight);
                    }

                    pageCurl
                            .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                    mRenderer.addHardMesh(pageCurl);

                    mCurlState = CURL_LEFT;
                    break;
                }
            }

        }
    }

    /**
     * Updates curl position.
     */
    private void updateCurlPos(PointerPosition pointerPos) {

        if (is_soft) {
            // Default curl radius.
            double radius = mRenderer.getPageRect(CURL_RIGHT).width() / 3;
            // TODO: This is not an optimal solution. Based on feedback received so
            // far; pressure is not very accurate, it may be better not to map
            // coefficient to range [0f, 1f] but something like [.2f, 1f] instead.
            // Leaving it as is until get my hands on a real device. On emulator
            // this doesn't work anyway.
            radius *= Math.max(1f - pointerPos.mPressure, 0f);
            // NOTE: Here we set pointerPos to mCurlPos. It might be a bit confusing
            // later to see e.g "mCurlPos.x - mDragStartPos.x" used. But it's
            // actually pointerPos we are doing calculations against. Why? Simply to
            // optimize code a bit with the cost of making it unreadable. Otherwise
            // we had to this in both of the next if-else branches.
            mCurlPos.set(pointerPos.mPos);

            // If curl happens on right page, or on left page on two page mode,
            // we'll calculate curl position from pointerPos.
            if (mCurlState == CURL_RIGHT
                    || (mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES)) {

                mCurlDir.x = mCurlPos.x - mDragStartPos.x;
                mCurlDir.y = mCurlPos.y - mDragStartPos.y;
                float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
                        * mCurlDir.y);

                // Adjust curl radius so that if page is dragged far enough on
                // opposite side, radius gets closer to zero.
                float pageWidth = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)
                        .width();
                double curlLen = radius * Math.PI;
                if (dist > (pageWidth * 2) - curlLen) {
                    curlLen = Math.max((pageWidth * 2) - dist, 0f);
                    radius = curlLen / Math.PI;
                }

                // Actual curl position calculation.
                if (dist >= curlLen) {
                    double translate = (dist - curlLen) / 2;
                    if (mViewMode == SHOW_TWO_PAGES) {
                        mCurlPos.x -= mCurlDir.x * translate / dist;
                    } else {
                        float pageLeftX = mRenderer
                                .getPageRect(CurlRenderer.PAGE_RIGHT).left;
                        radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius),
                                0f);
                    }
                    mCurlPos.y -= mCurlDir.y * translate / dist;
                } else {
                    double angle = Math.PI * Math.sqrt(dist / curlLen);
                    double translate = radius * Math.sin(angle);
                    mCurlPos.x += mCurlDir.x * translate / dist;
                    mCurlPos.y += mCurlDir.y * translate / dist;
                }
            }
            // Otherwise we'll let curl follow pointer position.
            else if (mCurlState == CURL_LEFT) {

                // Adjust radius regarding how close to page edge we are.
                float pageLeftX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left;
                radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius), 0f);

                float pageRightX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
                mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
                mCurlDir.x = mCurlPos.x + mDragStartPos.x;
                mCurlDir.y = mCurlPos.y - mDragStartPos.y;
            }

            setCurlPos(mCurlPos, mCurlDir, radius);
        } else {
            float angle = (float) (Math.acos(pointerPos.mPos.x
                    / mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).width()) * 180 / Math.PI);
            float result = pageCurl.setRotate(0 - angle);
            if (result <= 0 && result >= -180) {
                float move = (float) (mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left * (1 - Math.cos(result
                        * Math.PI / 180)));
                pageCurl.setTransaction(move);
                requestRender();
            }
        }
    }

    /**
     * Updates given CurlPage via PageProvider for page located at index.
     */
    private void updatePage(CurlPage page, int index) {
        // First reset page to initial state.
        page.reset();
        // Ask page provider to fill it up with bitmaps and colors.
        mPageProvider.updatePage(page, mPageBitmapWidth, mPageBitmapHeight,
                index);
    }

    /**
     * Updates bitmaps for page meshes.
     */
    private void updatePages() {
        if (mPageProvider == null || mPageBitmapWidth <= 0
                || mPageBitmapHeight <= 0) {
            return;
        }

        if (is_soft) {
            //妹的  软翻
            // Remove meshes from renderer.
            mRenderer.removeCurlMesh(mPageLeft);
            mRenderer.removeCurlMesh(mPageRight);
            mRenderer.removeCurlMesh(mPageCurl);

            int leftIdx = mCurrentIndex - 1;
            int rightIdx = mCurrentIndex;
            int curlIdx = -1;
            if (mCurlState == CURL_LEFT) {
                curlIdx = leftIdx;
                --leftIdx;
            } else if (mCurlState == CURL_RIGHT) {
                curlIdx = rightIdx;
                ++rightIdx;
            }

            if (rightIdx >= 0 && rightIdx < mPageProvider.getPageCount()) {
                updatePage(mPageRight.getTexturePage(), rightIdx);
                mPageRight.setFlipTexture(false);
                mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                mPageRight.reset();
                mRenderer.addCurlMesh(mPageRight);
            }
            if (leftIdx >= 0 && leftIdx < mPageProvider.getPageCount()) {
                updatePage(mPageLeft.getTexturePage(), leftIdx);
                mPageLeft.setFlipTexture(true);
                mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                mPageLeft.reset();
                if (mRenderLeftPage) {
                    mRenderer.addCurlMesh(mPageLeft);
                }
            }
            if (curlIdx >= 0 && curlIdx < mPageProvider.getPageCount()) {
                updatePage(mPageCurl.getTexturePage(), curlIdx);

                if (mCurlState == CURL_RIGHT) {
                    mPageCurl.setFlipTexture(true);
                    mPageCurl.setRect(mRenderer
                            .getPageRect(CurlRenderer.PAGE_RIGHT));
                } else {
                    mPageCurl.setFlipTexture(false);
                    mPageCurl
                            .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
                }

                mPageCurl.reset();
                mRenderer.addCurlMesh(mPageCurl);
            }
        } else {
            mRenderer.removeHardMesh(pageLeft);
            mRenderer.removeHardMesh(pageRight);
            mRenderer.removeHardMesh(pageCurl);

            int leftIdx = mCurrentIndex - 1;
            int rightIdx = mCurrentIndex;
            int curlIdx = -1;
            if (mCurlState == CURL_LEFT) {
                curlIdx = leftIdx;
                --leftIdx;
            } else if (mCurlState == CURL_RIGHT) {
                curlIdx = rightIdx;
                ++rightIdx;
            }

            if (rightIdx >= 0 && rightIdx < mPageProvider.getPageCount()) {
//                updatePage(mPageRight.getTexturePage(), rightIdx);
                CurlPage tempRight = new CurlPage();
                mPageProvider.updatePage(tempRight, mPageBitmapWidth, mPageBitmapHeight,
                        rightIdx);
                RectF frontRect = new RectF();
                Bitmap tempFront = tempRight.getTexture(frontRect, CurlPage.SIDE_FRONT);
                RectF backRect = new RectF();
                Bitmap tempBack = tempRight.getTexture(backRect, CurlPage.SIDE_BACK);

                pageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
                pageRight.setFrontTextureRect(frontRect);
                pageRight.setBackTextureRect(backRect);
                Bitmap[] tempBitmaps = new Bitmap[2];
                tempBitmaps[0] = tempFront;
                tempBitmaps[1] = tempBack;
                pageRight.setCoverBitmap(tempBitmaps);
//                mPageRight.reset();
                mRenderer.addHardMesh(pageRight);
            }
            if (leftIdx >= 0 && leftIdx < mPageProvider.getPageCount()) {
//                updatePage(mPageLeft.getTexturePage(), leftIdx);

                CurlPage tempLeft = new CurlPage();
                mPageProvider.updatePage(tempLeft, mPageBitmapWidth, mPageBitmapHeight,
                        leftIdx);
                RectF frontRect = new RectF();
                Bitmap tempFront = tempLeft.getTexture(frontRect, CurlPage.SIDE_FRONT);
                RectF backRect = new RectF();
                Bitmap tempBack = tempLeft.getTexture(backRect, CurlPage.SIDE_BACK);

                pageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));

                pageLeft.setFrontTextureRect(frontRect);
                pageLeft.setBackTextureRect(backRect);
                Bitmap[] tempBitmaps = new Bitmap[2];
                tempBitmaps[0] = tempFront;
                tempBitmaps[1] = tempBack;
                pageLeft.setCoverBitmap(tempBitmaps);
                pageLeft.setRotate(-180);
                float move = (float) (mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left * (1 - Math.cos(-180
                        * Math.PI / 180)));
                pageLeft.setTransaction(move);
//                mPageLeft.reset();
                if (mRenderLeftPage) {
                    mRenderer.addHardMesh(pageLeft);
                }
            }
            if (curlIdx >= 0 && curlIdx < mPageProvider.getPageCount()) {
//                updatePage(mPageCurl.getTexturePage(), curlIdx);
                CurlPage tempLeft = new CurlPage();
                mPageProvider.updatePage(tempLeft, mPageBitmapWidth, mPageBitmapHeight,
                        curlIdx);
                RectF frontRect = new RectF();
                Bitmap tempFront = tempLeft.getTexture(frontRect, CurlPage.SIDE_FRONT);
                RectF backRect = new RectF();
                Bitmap tempBack = tempLeft.getTexture(backRect, CurlPage.SIDE_BACK);
//                if (mCurlState == CURL_RIGHT) {
                pageCurl.setRect(mRenderer
                        .getPageRect(CurlRenderer.PAGE_RIGHT));

                pageCurl.setFrontTextureRect(frontRect);
                pageCurl.setBackTextureRect(backRect);
                Bitmap[] tempBitmaps = new Bitmap[2];
                tempBitmaps[0] = tempFront;
                tempBitmaps[1] = tempBack;
                pageCurl.setCoverBitmap(tempBitmaps);
//                } else {
//                    mPageCurl
//                            .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
//                }
//                mPageCurl.reset();
                mRenderer.addHardMesh(pageCurl);
            }
        }
    }

    /**
     * Provider for feeding 'book' with bitmaps which are used for rendering
     * pages.
     */
    public interface PageProvider {

        public Bitmap[] getFrontBitmap();

        public Bitmap[] getBackBitmap();

        /**
         * Return number of pages available.
         */
        public int getPageCount();

        /**
         * Called once new bitmaps/textures are needed. Width and height are in
         * pixels telling the size it will be drawn on screen and following them
         * ensures that aspect ratio remains. But it's possible to return bitmap
         * of any size though. You should use provided CurlPage for storing page
         * information for requested page number.<br/>
         * <br/>
         * Index is a number between 0 and getBitmapCount() - 1.
         */
        public void updatePage(CurlPage page, int width, int height, int index);
    }

    /**
     * Simple holder for pointer position.
     */
    private class PointerPosition {
        PointF mPos = new PointF();
        float mPressure;
    }

    /**
     * Observer interface for handling CurlView size changes.
     */
    public interface SizeChangedObserver {

        /**
         * Called once CurlView size changes.
         */
        public void onSizeChanged(int width, int height);
    }

}
