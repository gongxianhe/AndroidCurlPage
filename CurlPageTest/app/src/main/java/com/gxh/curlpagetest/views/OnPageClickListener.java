package com.gxh.curlpagetest.views;

/**
 * Created by fft123 on 2016/12/6.
 */
public interface OnPageClickListener {
    public void onClickPage(int mCurrentIndex, boolean isFront);

    public boolean validEnableClick(int mCurrentIndex, boolean isFront);
}
