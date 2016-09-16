package com.tuacy.refreshload;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tuacy.refreshloadlib.adapter.LoadRecyclerBaseAdapter;
import com.tuacy.refreshloadlib.view.DefaultTextLoadingView;
import com.tuacy.refreshloadlib.view.LoadRecyclerView;
import com.tuacy.refreshloadlib.view.loadingview.DefaultProgressLoadingView;

import java.util.ArrayList;
import java.util.List;

public class LoadAdapter extends LoadRecyclerBaseAdapter {

	private Context      mContext;
	private List<String> mData;

	public LoadAdapter(Context context, List<String> data) {
		super();
		mContext = context;
		mData = data;
	}

	public void setData(List<String> data) {
		mData = data;
		notifyDataSetChanged();
	}

	public void loadModeData(List<String> load) {
		if (mData == null) {
			mData = new ArrayList<>();
		}
		for (String temp : load) {
			mData.add(temp);
		}
		notifyDataSetChanged();
	}

	@Override
	public int onItemCount() {
		return mData == null ? 0 : mData.size();
	}

	@Override
	public RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
		return new ItemHolder(LayoutInflater.from(mContext).inflate(R.layout.item_list_content, parent, false));
	}

	@Override
	public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
		ItemHolder itemHolder = (ItemHolder) holder;
		itemHolder.mViewContent.setText(mData.get(position));
	}

	@Override
	public RecyclerView.ViewHolder onCreateLoadViewHolder(ViewGroup parent, int viewType, int loadState) {
		return new LoadHolder(LayoutInflater.from(mContext).inflate(R.layout.item_list_load_progress, parent, false));
	}

	@Override
	public void onBindLoadViewHolder(RecyclerView.ViewHolder holder, int position, int loadState) {
		LoadHolder loadHolder = (LoadHolder) holder;
		switch (loadState) {
			case LoadRecyclerView.LOAD_STATE_PREPARE:
				loadHolder.mNoLoadData.setVisibility(View.GONE);
				loadHolder.mViewLoad.setVisibility(View.VISIBLE);
				loadHolder.mViewLoad.prepareLoading();
				break;
			case LoadRecyclerView.LOAD_STATE_DOING:
				loadHolder.mNoLoadData.setVisibility(View.GONE);
				loadHolder.mViewLoad.setVisibility(View.VISIBLE);
				loadHolder.mViewLoad.doLoading();
				break;
			case LoadRecyclerView.LOAD_STATE_COMPLETE_SINGLE:
				loadHolder.mNoLoadData.setVisibility(View.GONE);
				loadHolder.mViewLoad.setVisibility(View.VISIBLE);
				loadHolder.mViewLoad.singleLoadingComplete();
				break;
			case LoadRecyclerView.LOAD_STATE_COMPLETE_ALL:
				loadHolder.mNoLoadData.setVisibility(View.VISIBLE);
				loadHolder.mViewLoad.setVisibility(View.GONE);
				loadHolder.mViewLoad.allLoadingComplete();
				break;
		}
	}

	@Override
	public RecyclerView.ViewHolder onCreateEmptyViewHolder(ViewGroup parent, int viewType) {
		return new EmptyHolder(LayoutInflater.from(mContext).inflate(R.layout.item_list_empty, parent, false));
	}

	@Override
	public void onBindEmptyViewHolder(RecyclerView.ViewHolder holder, int position) {
		EmptyHolder emptyHolder = (EmptyHolder) holder;
	}

	private class ItemHolder extends RecyclerView.ViewHolder {

		private TextView mViewContent;

		ItemHolder(View itemView) {
			super(itemView);
			mViewContent = (TextView) itemView.findViewById(R.id.text_view_content);
		}
	}

	//	private class LoadHolder extends RecyclerView.ViewHolder {
	//
	//		private DefaultTextLoadingView mViewLoad;
	//		private TextView               mNoLoadData;
	//
	//		LoadHolder(View itemView) {
	//			super(itemView);
	//			mViewLoad = (DefaultTextLoadingView) itemView.findViewById(R.id.layout_loading);
	//			mNoLoadData = (TextView) itemView.findViewById(R.id.text_view_all_load_complete);
	//		}
	//	}

	private class LoadHolder extends RecyclerView.ViewHolder {

		private DefaultProgressLoadingView mViewLoad;
		private TextView                   mNoLoadData;

		LoadHolder(View itemView) {
			super(itemView);
			mViewLoad = (DefaultProgressLoadingView) itemView.findViewById(R.id.layout_loading);
			mNoLoadData = (TextView) itemView.findViewById(R.id.text_view_all_load_complete);
		}
	}

	private class EmptyHolder extends RecyclerView.ViewHolder {

		private TextView mViewEmpty;

		EmptyHolder(View itemView) {
			super(itemView);
			mViewEmpty = (TextView) itemView.findViewById(R.id.text_view_empty);

		}
	}
}
