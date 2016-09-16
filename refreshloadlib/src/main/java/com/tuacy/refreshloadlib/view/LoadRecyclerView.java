package com.tuacy.refreshloadlib.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.tuacy.refreshloadlib.adapter.LoadRecyclerBaseAdapter;

public class LoadRecyclerView extends RecyclerView {

	public static final int LOAD_STATE_PREPARE         = 0;
	public static final int LOAD_STATE_DOING           = 1;
	public static final int LOAD_STATE_COMPLETE_SINGLE = 2;
	public static final int LOAD_STATE_COMPLETE_ALL    = 3;

	private LoadRecyclerBaseAdapter mAdapter;
	private LinearLayoutManager     mLayoutManager;
	private int                     mLastVisibleItem;
	private OnLoadMoreListener      mListener;
	private int                     mCurrentLoadState;

	public interface OnLoadMoreListener {

		void onLoadMore();
	}

	public LoadRecyclerView(Context context) {
		this(context, null);
	}

	public LoadRecyclerView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LoadRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mCurrentLoadState = LOAD_STATE_PREPARE;
		// 监听RecyclerView滑动过程
		addOnScrollListener(new OnScrollListener() {
			// OnScrollListener
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				if (newState == RecyclerView.SCROLL_STATE_IDLE) {
					// 为了避免当没有满一个屏幕的时候加载不了的问题
					mLastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
				}
				if (newState == RecyclerView.SCROLL_STATE_IDLE && mCurrentLoadState != LOAD_STATE_COMPLETE_ALL &&
					mLastVisibleItem + 1 == mAdapter.getItemCount() && mListener != null &&
					mCurrentLoadState != LOAD_STATE_DOING) {
					mCurrentLoadState = LOAD_STATE_DOING;
					mAdapter.setLoadState(mCurrentLoadState);
					mListener.onLoadMore();
				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				mLastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
			}
		});
	}

	public void setOnLoadMoreListener(OnLoadMoreListener listener) {
		mListener = listener;
	}

	/**
	 * 加载完成
	 */
	public void completeLoadSingle() {
		mCurrentLoadState = LOAD_STATE_COMPLETE_SINGLE;
		mCurrentLoadState = LOAD_STATE_PREPARE;
		mAdapter.setLoadState(mCurrentLoadState);
	}

	public void completeLoadAll() {
		mCurrentLoadState = LOAD_STATE_COMPLETE_ALL;
		mAdapter.setLoadState(mCurrentLoadState);
	}

	public void reset() {
		mCurrentLoadState = LOAD_STATE_PREPARE;
		mAdapter.setLoadState(mCurrentLoadState);
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
