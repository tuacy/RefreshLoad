package com.tuacy.refreshloadlib.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tuacy.refreshloadlib.R;

/**
 * 用最后一个item来显示正在加载中
 */
public abstract class LoadRecyclerBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	protected static final int TYPE_ITEM   = 0;
	protected static final int TYPE_FOOTER = 1;

	public void fullyLoad() {

	}

	public abstract int onItemCount();

	/**
	 * item viewHolder
	 */
	public abstract RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType);

	/**
	 * bind itemViewHolder
	 */
	public abstract void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position);

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_ITEM:
				return onCreateItemViewHolder(parent, viewType);
			case TYPE_FOOTER:
				return new LoadViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_load, parent, false));
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
				LoadViewHolder loadHolder = (LoadViewHolder) holder;
				break;
		}
	}

	@Override
	public int getItemViewType(int position) {
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

	class LoadViewHolder extends RecyclerView.ViewHolder {

		TextView mLoadText;

		public LoadViewHolder(View view) {
			super(view);
			mLoadText = (TextView) view.findViewById(R.id.text_view_load_text);
		}

	}
}
