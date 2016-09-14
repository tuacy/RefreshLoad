package com.tuacy.refreshloadlib.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.tuacy.refreshloadlib.view.LoadRecyclerView;

/**
 * 用最后一个item来显示正在加载中
 */
public abstract class LoadRecyclerBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TYPE_ITEM   = 0;
	private static final int TYPE_FOOTER = 1;
	private static final int TYPE_EMPTY  = 2;

	private int mLoadState;

	public LoadRecyclerBaseAdapter() {
		mLoadState = LoadRecyclerView.LOAD_STATE_START;
	}

	public void setLoadState(int state) {
		mLoadState = state;
		// 只刷新最后一项，即load的那一项
		notifyItemChanged(getItemCount() - 1);
	}

	public abstract int onItemCount();

	/**
	 * 内容区域
	 */
	public abstract RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType);

	public abstract void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position);

	/**
	 * 加载的时候显示的load区域
	 */
	public abstract RecyclerView.ViewHolder onCreateLoadViewHolder(ViewGroup parent, int viewType, int loadState);

	public abstract void onBindLoadViewHolder(RecyclerView.ViewHolder holder, final int position, int loadState);

	/**
	 * 没有内容
	 */
	public abstract RecyclerView.ViewHolder onCreateEmptyViewHolder(ViewGroup parent, int viewType);

	public abstract void onBindEmptyViewHolder(RecyclerView.ViewHolder holder, final int position);

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_ITEM:
				return onCreateItemViewHolder(parent, viewType);
			case TYPE_FOOTER:
				return onCreateLoadViewHolder(parent, viewType, mLoadState);
			case TYPE_EMPTY:
				return onCreateEmptyViewHolder(parent, viewType);
		}
		return null;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		switch (getItemViewType(position)) {
			case TYPE_ITEM:
				onBindItemViewHolder(holder, position);
				break;
			case TYPE_FOOTER:
				onBindLoadViewHolder(holder, position, mLoadState);
				break;
			case TYPE_EMPTY:
				onBindEmptyViewHolder(holder, position);
				break;
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (getItemCount() == 1 && position == 0) {
			// 说明是没有数据的，LIST为空
			return TYPE_EMPTY;
		}
		if (position + 1 == getItemCount()) {
			// 最后一个用来显示加载中
			return TYPE_FOOTER;
		} else {
			return TYPE_ITEM;
		}
	}

	/**
	 * 最后一个用来显示加载中
	 *
	 * @return onItemCount() + 1
	 */
	@Override
	public int getItemCount() {
		return onItemCount() + 1;
	}
}
