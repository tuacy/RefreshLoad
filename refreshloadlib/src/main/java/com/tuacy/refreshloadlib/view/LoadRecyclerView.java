package com.tuacy.refreshloadlib.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.tuacy.refreshloadlib.adapter.LoadRecyclerBaseAdapter;

public class LoadRecyclerView extends RecyclerView {

	private Context                 mContext;
	private LoadRecyclerBaseAdapter mAdapter;
	private LinearLayoutManager     mLayoutManager;
	private int                     mLastVisibleItem;

	public LoadRecyclerView(Context context) {
		this(context, null);
	}

	public LoadRecyclerView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LoadRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}

	private void init() {
		// 监听RecyclerView滑动过程
		addOnScrollListener(new OnScrollListener() {
			// OnScrollListener
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				if (newState == RecyclerView.SCROLL_STATE_IDLE && mLastVisibleItem + 1 == mAdapter.getItemCount()) {

				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				mLastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
			}
		});
	}

	@Override
	public void setAdapter(Adapter adapter) {
		super.setAdapter(adapter);
		if (!(adapter instanceof LoadRecyclerBaseAdapter)) {
			throw new IllegalArgumentException("LoadRecyclerView adapter must instanceof LoadRecyclerBaseAdapter");
		}
		mAdapter = (LoadRecyclerBaseAdapter) adapter;
	}

	@Override
	public void setLayoutManager(LayoutManager layout) {
		super.setLayoutManager(layout);
		if (!(layout instanceof LinearLayoutManager)) {
			throw new IllegalArgumentException("LoadRecyclerView layoutManager must instanceof LinearLayoutManager");
		}
		mLayoutManager = (LinearLayoutManager) layout;
	}

}
