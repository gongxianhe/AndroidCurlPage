package com.gxh.curlpagetest.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gxh.curlpagetest.R;


/**
 * Created by fft123 on 2016/11/29.
 */
public class AlbumProgressBar extends LinearLayout {

    private Context mContext;
    private LayoutInflater inflater;
    private View rootView;
    private TextView tv_progress;
    private ImageView img_progress_bar;

    private float tv_progress_width;
    private float img_progress_bar_width;

    private int pageNumber;
    private int pageSize;

    private float onePageWidth;

    private int currentIndex;

    public AlbumProgressBar(Context context) {
        this(context, null);

    }

    public AlbumProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public AlbumProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        inflater = LayoutInflater.from(context);
        init();
    }

    private void init() {

        rootView = inflater.inflate(R.layout.album_progress, this, true);
        tv_progress = (TextView) rootView.findViewById(R.id.tv_progress);
        img_progress_bar = (ImageView) rootView.findViewById(R.id.img_progress_bar);

        int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        tv_progress.measure(w, h);
        tv_progress_width = tv_progress.getMeasuredWidth();

        int w2 = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int h2 = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        img_progress_bar.measure(w2, h2);
        img_progress_bar_width = img_progress_bar.getMeasuredWidth() - tv_progress_width / 2;

    }

    public void setPageNum(int pageNum) {
        this.pageSize = pageNum / 2 - 2;
        this.pageNumber = pageNum - 2 * 3 - 1;
        onePageWidth = img_progress_bar_width / (pageNum / 2 + 1);
    }

    public void setCurrentIndex(final int pageIndex) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentIndex = pageIndex;
                if (pageIndex == 0) {
                    tv_progress.setX((pageIndex - 1) * onePageWidth + tv_progress_width / 2);
                    tv_progress.setText("封面");
                } else {
                    if (pageIndex == 1) {
                        tv_progress.setText("封面");
                    } else if (pageIndex == 2) {
                        tv_progress.setText("扉页");
                    } else if (pageIndex == pageSize + 2 || pageIndex == pageSize + 2 + 1) {
                        tv_progress.setText("封底");
                    } else {
                        tv_progress.setText(2 * (pageIndex - 2) - 1 + "/" + pageNumber);
                    }

                    if (pageIndex == pageSize + 2 + 1) {
                        tv_progress.setX(img_progress_bar_width - tv_progress_width);
                    } else {
                        tv_progress.setX((currentIndex - 1) * onePageWidth);
                    }
                }
            }
        });

    }


}
