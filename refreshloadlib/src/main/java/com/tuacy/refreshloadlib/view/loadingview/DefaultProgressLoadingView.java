package com.tuacy.refreshloadlib.view.loadingview;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.tuacy.refreshloadlib.refresh.CircleImageView;
import com.tuacy.refreshloadlib.refresh.MaterialProgressDrawable;

public class DefaultProgressLoadingView extends LinearLayout {

	// Default background for the progress spinner
	private static final int   CIRCLE_BG_LIGHT    = 0xFFFAFAFA;
	// Default offset in dips from the top of the view to where the progress spinner should stop
	private static final int   CIRCLE_DIAMETER    = 40;
	private static final float MAX_PROGRESS_ANGLE = .8f;
	private static final int   MAX_ALPHA          = 255;

	private int mColorResIds[] = {android.R.color.holo_blue_light,
								  android.R.color.holo_red_light,
								  android.R.color.holo_orange_light,
								  android.R.color.holo_green_light};


	private Context                  mContext;
	private CircleImageView          mCircleView;
	private MaterialProgressDrawable mProgress;

	public DefaultProgressLoadingView(Context context) {
		this(context, null);
	}

	public DefaultProgressLoadingView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DefaultProgressLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
		mProgress = new MaterialProgressDrawable(getContext(), this);
		mCircleView.setImageDrawable(mProgress);
		addView(mCircleView);
		final Resources res = getResources();
		int[] colorRes = new int[mColorResIds.length];
		for (int i = 0; i < mColorResIds.length; i++) {
			colorRes[i] = res.getColor(mColorResIds[i]);
		}
		mProgress.setColorSchemeColors(colorRes);
		mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
		mProgress.setStartEndTrim(0f, MAX_PROGRESS_ANGLE);
		mProgress.setAlpha(MAX_ALPHA);
		mProgress.start();
	}

	public void prepareLoading() {
		mProgress.setAlpha(MAX_ALPHA);
		mProgress.start();
	}

	public void doLoading() {
		mProgress.setAlpha(MAX_ALPHA);
		mProgress.start();
	}

	public void singleLoadingComplete() {
		mProgress.setAlpha(MAX_ALPHA);
		mProgress.stop();
	}

	public void allLoadingComplete() {
		mProgress.setAlpha(0);
		mProgress.stop();
	}
}
