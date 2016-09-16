package com.tuacy.refreshloadlib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tuacy.refreshloadlib.R;

public class DefaultTextLoadingView extends LinearLayout {

	private Context      mContext;
	private DotsTextView mViewDotsText;
	private TextView     mViewLoadingText;

	public DefaultTextLoadingView(Context context) {
		this(context, null);
	}

	public DefaultTextLoadingView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DefaultTextLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		View viewParent = LayoutInflater.from(mContext).inflate(R.layout.layout_default_loading_text_view, this, true);
		mViewLoadingText = (TextView) viewParent.findViewById(R.id.text_view_loading_text);
		mViewDotsText = (DotsTextView) viewParent.findViewById(R.id.view_dots);
	}

	public void prepareLoading() {
		mViewLoadingText.setText(R.string.start_loading);
		if (!mViewDotsText.isHide()) {
			mViewDotsText.hide();
		}
		if (mViewDotsText.isPlaying()) {
			mViewDotsText.stop();
		}
	}

	public void doLoading() {
		mViewLoadingText.setText(R.string.do_loading);
		if (mViewDotsText.isHide()) {
			mViewDotsText.show();
		}
		if (!mViewDotsText.isPlaying()) {
			mViewDotsText.start();
		}
	}

	public void singleLoadingComplete() {
		mViewLoadingText.setText(R.string.no_loading);
		if (mViewDotsText.isHide()) {
			mViewDotsText.show();
		}
		if (mViewDotsText.isPlaying()) {
			mViewDotsText.stop();
		}
	}

	public void allLoadingComplete() {
		mViewLoadingText.setText(R.string.no_loading);
		if (mViewDotsText.isPlaying()) {
			mViewDotsText.stop();
		}
		if (!mViewDotsText.isHide()) {
			mViewDotsText.hide();
		}
	}

}
