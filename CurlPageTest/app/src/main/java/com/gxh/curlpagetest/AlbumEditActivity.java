package com.gxh.curlpagetest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;
import android.widget.Toast;

import com.gxh.curlpagetest.views.AlbumProgressBar;
import com.gxh.curlpagetest.views.AlbumView;
import com.gxh.curlpagetest.views.BitmapTools;
import com.gxh.curlpagetest.views.CurlPage;
import com.gxh.curlpagetest.views.OnFlipedLastPageListener;
import com.gxh.curlpagetest.views.OnPageClickListener;


/**
 * Created by fft123 on 2016/11/29.
 * ###########################GXH软硬翻页#####################################
 * ################################如果想要全软翻,请自行去掉封面和尾页,自行修改###########################################
 */
public class AlbumEditActivity extends FragmentActivity implements OnPageClickListener, OnFlipedLastPageListener {


    private AlbumProgressBar album_progress_bar;
    private AlbumView album_view;
    private int pageSize = 20;
    private int currentIndex = 0;
    int lastIndex = -1;
    Bitmap shadowLine;

    Bitmap[] frontBacks = new Bitmap[4];  // 长度为4   0和1 为 书皮正反面   2和3 为 书尾正反面
    Bitmap[] pages = new Bitmap[2];   // 所有页 在测试中 一样的  实际中大家根据需求进行多样化

    boolean isSoft = false;  // 是不是软翻

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        shadowLine = BitmapFactory.decodeResource(getResources(), R.drawable.bg_photo_album_shadow);
        frontBacks[0] = BitmapFactory.decodeResource(getResources(), R.drawable.xiaoxin);
        frontBacks[1] = BitmapFactory.decodeResource(getResources(), R.drawable.xiaoxinback);
        frontBacks[2] = BitmapFactory.decodeResource(getResources(), R.drawable.xiaoxin);
        frontBacks[3] = BitmapFactory.decodeResource(getResources(), R.drawable.xiaoxinback);
        pages[0] = BitmapFactory.decodeResource(getResources(), R.drawable.page);
        pages[1] = BitmapFactory.decodeResource(getResources(), R.drawable.pageback);

        album_progress_bar = (AlbumProgressBar) findViewById(R.id.album_progress_bar);
        album_view = (AlbumView) findViewById(R.id.album_view);
        album_view.setSizeChangedObserver(new SizeChangedObserver());
        album_view.setAlbumProgressBar(album_progress_bar);
        album_view.setOnPageClickListener(this);
        album_view.setOnFlipedLastPageListener(this);

        album_view.setPageProvider(new PageProvider(), isSoft); //只传一个参数默认是软翻

        if (lastIndex != -1 && lastIndex != 0) {
            album_view.setCurrentIndex(lastIndex);
        } else {
            album_view.setCurrentIndex(currentIndex);
        }
        album_progress_bar.setPageNum(20);
        album_progress_bar.setCurrentIndex(1);
        album_view.requestRender();

        if (getLastCustomNonConfigurationInstance() != null) {
            lastIndex = (Integer) getLastCustomNonConfigurationInstance();
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return album_view.getCurrentIndex();
    }

    @Override
    protected void onResume() {
        super.onResume();
        album_view.onResume();
    }

    @Override
    public void onClickPage(int mCurrentIndex, boolean isFront) {
        int index = 0;
        if (isFront) {
            index = mCurrentIndex * 2 + 2;
        } else {
            index = mCurrentIndex * 2 + 3;
        }
        Toast.makeText(this, "点击了   页数: " + mCurrentIndex + "页面: " + (isFront ? "前页" : "后页"), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean validEnableClick(int mCurrentIndex, boolean isFront) {
        //这个方法是验证该页能不能点击  根据需求而来,没有要求就返回true就行了
        return true;
    }

    @Override
    public void onFlippedLastPage() {
        //最后一页翻过监听,也是根据需求来
        Toast.makeText(this, "书翻完了", Toast.LENGTH_LONG).show();
    }


    private class PageProvider implements AlbumView.PageProvider {


        @Override
        public Bitmap[] getFrontBitmap() {

            Bitmap front = getTexture(frontBacks[0], true, true, false);
            Bitmap back = getTexture(frontBacks[1], false, true, false);

            Bitmap[] covers = new Bitmap[2];
            covers[0] = front;
            covers[1] = back;

            return covers;
        }

        @Override
        public Bitmap[] getBackBitmap() {
            Bitmap front = getTexture(frontBacks[2], true, false, false);
            Bitmap back = getTexture(frontBacks[3], false, false, false);

            Bitmap[] covers = new Bitmap[2];
            covers[0] = front;
            covers[1] = back;

            return covers;
        }

        @Override
        public int getPageCount() {
            return pageSize / 2 - 2;    //pageSize/2  因为是正反面 - 2  减去封面和尾页
        }

        private Bitmap getTexture(Bitmap bitmap, boolean isFront, boolean isFirst, boolean isPage) {
            // Bitmap original size.
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            // Bitmap size expanded to next power of two. This is done due to
            // the requirement on many devices, texture width and height should
            // be power of two.
            int newW = getNextHighestPO2(w);
            int newH = getNextHighestPO2(h);

            // TODO: Is there another way to create a bigger Bitmap and copy
            // original Bitmap to it more efficiently? Immutable bitmap anyone?
            Bitmap bitmapTex = Bitmap.createBitmap(newW, newH, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(bitmapTex);
            c.drawBitmap(bitmap, 0, 0, null);


            float texX = (float) w / newW;
            float texY = (float) h / newH;

            float[] rect = new float[4];
            rect[0] = 0;
            rect[1] = 0;
            rect[2] = texX;
            rect[3] = texY;

            if (!isPage) {
                if (isFirst) {
                    if (isFront) {
                        album_view.setFirstFrontRect(rect);
                    } else {
                        album_view.setFirstBackRect(rect);
                    }
                } else {
                    if (isFront) {
                        album_view.setLastFrontRect(rect);
                    } else {
                        album_view.setLastBackRect(rect);
                    }
                }
            }

            return bitmapTex;
        }

        private int getNextHighestPO2(int n) {
            n -= 1;
            n = n | (n >> 1);
            n = n | (n >> 2);
            n = n | (n >> 4);
            n = n | (n >> 8);
            n = n | (n >> 16);
            n = n | (n >> 32);
            return n + 1;
        }

        @Override
        public void updatePage(CurlPage page, int width, int height, int index) {
            Bitmap front = pages[0];
            Bitmap back = pages[1];
            switch (index) {
                default: {
                    Bitmap frontBitmapWithLine = BitmapTools.drawShadowLine(front, shadowLine);
                    page.setTexture(frontBitmapWithLine, CurlPage.SIDE_FRONT);


                    //这里要判断一下软硬 是因为外国大神些的翻页 没有对正反顶点做处理 所以在软翻时候要翻转一下...
                    //而在硬翻那 我做了处理 则不需要 直接设置就行
                    if (isSoft) {
                        Matrix matrix = new Matrix();
                        matrix.postScale(-1, 1); // 镜像水平翻转
                        Bitmap backPhoto = Bitmap.createBitmap(back, 0, 0,
                                back.getWidth(), back.getHeight(), matrix, true);
                        page.setTexture(backPhoto, CurlPage.SIDE_BACK);
                    } else {
                        page.setTexture(back, CurlPage.SIDE_BACK);
                    }
                    break;
                }
            }
        }

    }


    private class SizeChangedObserver implements AlbumView.SizeChangedObserver {
        @Override
        public void onSizeChanged(int w, int h) {
            album_view.setMargins(.1f, .05f, .01f, .2f);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //可以做一下释放图片资源
    }
}
