package com.tuacy.refreshload;

import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.tuacy.refreshload.base.BaseWeakReferenceHandler;
import com.tuacy.refreshloadlib.view.LoadRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, LoadRecyclerView.OnLoadMoreListener {

	private static final int MESSAGE_REFRESH = 0x101;
	private static final int MESSAGE_LOAD    = 0x102;

	private LoadAdapter        mAdapter;
	private SwipeRefreshLayout mSwipeRefresh;
	private LoadRecyclerView   mLoadRecyclerView;
	private Handler            mHandler;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mHandler = new MainHandle(this);
		initView();
		initEvent();
		initData();
	}

	private void initView() {
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
		mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light,
											  android.R.color.holo_orange_light, android.R.color.holo_green_light);
		mSwipeRefresh.setOnRefreshListener(this);
		mLoadRecyclerView = (LoadRecyclerView) findViewById(R.id.recycler_view_record);
		mLoadRecyclerView.setHasFixedSize(true);
		mLoadRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		mLoadRecyclerView.setOnLoadMoreListener(this);
	}

	private void initEvent() {

	}

	private void initData() {
		mAdapter = new LoadAdapter(this, null);
		mLoadRecyclerView.setAdapter(mAdapter);
	}

	private List<String> obtainTestList() {
		List<String> data = new ArrayList<>();
		data.add(" init 1");
		data.add(" init 2");
		data.add(" init 3");
		data.add(" init 4");
		data.add(" init 5");
		data.add(" init 6");
		return data;
	}

	private void dealWithMessage(Message msg) {
		switch (msg.what) {
			case MESSAGE_REFRESH:
				List<String> refreshData = new ArrayList<>();
				refreshData.add(" refresh 1");
				refreshData.add(" refresh 2");
				mAdapter.setData(refreshData);
				mSwipeRefresh.setRefreshing(false);
				mLoadRecyclerView.completeLoadSingle();
				break;
			case MESSAGE_LOAD:
				List<String> loadData = new ArrayList<>();
				loadData.add(" load 1");
				loadData.add(" load 2");
				mAdapter.loadModeData(loadData);
				mLoadRecyclerView.completeLoadSingle();
				break;
		}
	}

	@Override
	public void onRefresh() {
		// 下拉刷新走起
		mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, 3000);
	}

	@Override
	public void onLoadMore() {
		// 上拉加载更多
		mHandler.sendEmptyMessageDelayed(MESSAGE_LOAD, 3000);
	}

	private static class MainHandle extends BaseWeakReferenceHandler<MainActivity> {

		public MainHandle(MainActivity reference) {
			super(reference);
		}

		@Override
		public void referenceHandleMessage(MainActivity reference, Message msg) {
			reference.dealWithMessage(msg);
		}
	}
}
