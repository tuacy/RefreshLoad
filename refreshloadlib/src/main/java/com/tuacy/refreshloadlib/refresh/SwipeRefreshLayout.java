package com.tuacy.refreshloadlib.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the contents of a view via a vertical swipe gesture. The activity
 * that instantiates this view should add an OnRefreshListener to be notified whenever the swipe to refresh gesture is completed. The
 * SwipeRefreshLayout will notify the listener each and every time the gesture is completed again; the listener is responsible for correctly
 * determining when to actually initiate a refresh of its content. If the listener determines there should not be a refresh, it must call
 * setRefreshing(false) to cancel any visual indication of a refresh. If an activity wishes to show just the progress animation, it should
 * call setRefreshing(true). To disable the gesture and progress animation, call setEnabled(false) on the view. <p> This layout should be
 * made the parent of the view that will be refreshed as a result of the gesture and can only support one direct child. This view will also
 * be made the target of the gesture and will be forced to match both the width and the height supplied in this layout. The
 * SwipeRefreshLayout does not provide accessibility events; instead, a menu item must be provided to allow refresh of the content wherever
 * this gesture is used. </p>
 */
public class SwipeRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {

	// Maps to ProgressBar.Large style
	public static final int LARGE   = MaterialProgressDrawable.LARGE;
	// Maps to ProgressBar default style
	public static final int DEFAULT = MaterialProgressDrawable.DEFAULT;

	private static final String LOG_TAG = SwipeRefreshLayout.class.getSimpleName();

	private static final int MAX_ALPHA               = 255;
	private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

	private static final int CIRCLE_DIAMETER       = 40;
	private static final int CIRCLE_DIAMETER_LARGE = 56;

	private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
	private static final int   INVALID_POINTER                 = -1;
	private static final float DRAG_RATE                       = .5f;

	// Max amount of circle that can be filled by progress during swipe gesture,
	// where 1.0 is a full circle
	private static final float MAX_PROGRESS_ANGLE = .8f;

	private static final int SCALE_DOWN_DURATION = 150;

	private static final int ALPHA_ANIMATION_DURATION = 300;

	private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

	private static final int ANIMATE_TO_START_DURATION = 200;

	// Default background for the progress spinner
	private static final int CIRCLE_BG_LIGHT       = 0xFFFAFAFA;
	// Default offset in dips from the top of the view to where the progress spinner should stop
	private static final int DEFAULT_CIRCLE_TARGET = 64;

	private View              mTarget; // the target of the gesture
	private OnRefreshListener mListener;
	private boolean mRefreshing = false;
	private int mTouchSlop;
	// 刷新状态的距离
	private float mTotalDragDistance = -1;
	// If nested scrolling is enabled, the total amount that needed to be
	// consumed by this as the nested scrolling parent is used in place of the
	// overscroll determined by MOVE events in the onTouch handler
	private       float                       mTotalUnconsumed;
	private final NestedScrollingParentHelper mNestedScrollingParentHelper;
	private final NestedScrollingChildHelper  mNestedScrollingChildHelper;
	private final int[] mParentScrollConsumed = new int[2];

	private int mMediumAnimationDuration;
	private int mCurrentTargetOffsetTop;
	// Whether or not the starting offset has been determined.
	private boolean mOriginalOffsetCalculated = false;

	private float   mInitialMotionY;
	private float   mInitialDownY;
	private boolean mIsBeingDragged;
	private int mActivePointerId = INVALID_POINTER;
	// Whether this item is scaled up rather than clipped
	private boolean mScale;

	// Target is returning to its start offset because it was cancelled or a
	// refresh was triggered.
	private       boolean                mReturningToStart;
	private final DecelerateInterpolator mDecelerateInterpolator;
	private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};

	private CircleImageView mCircleView;
	private int mCircleViewIndex = -1;

	protected int mFrom;

	private float mStartingScale;

	// mCircleView初始的位置（小圆圈滑动的开始位置）
	protected int mOriginalOffsetTop;

	private MaterialProgressDrawable mProgress;

	private Animation mScaleAnimation;

	private Animation mScaleDownAnimation;

	// alpha动画的变化，从当前值到STARTING_PROGRESS_ALPHA的变化
	private Animation mAlphaStartAnimation;
	// alpha动画的变化，从当前值到MAX_ALPHA的变化
	private Animation mAlphaMaxAnimation;

	private Animation mScaleDownToStartAnimation;

	// 进入刷新状态的时候小圆圈相对于初始位置的偏移量
	private float mSpinnerFinalOffset;

	private boolean mNotify;

	private int mCircleWidth;

	private int mCircleHeight;

	// Whether the client has set a custom starting position;
	private boolean mUsingCustomStart;

	private AnimationListener mRefreshListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (mRefreshing) {
				// 进入刷新状态
				mProgress.setAlpha(MAX_ALPHA);
				// 小圆圈里面的精度条开始转圈
				mProgress.start();
				if (mNotify) {
					if (mListener != null) {
						// 回调我们设置的刷新
						mListener.onRefresh();
					}
				}
			} else {
				// 进入不刷新的状态，小圆圈里面的进度条停止转圈
				mProgress.stop();
				// 小圆圈不可见
				mCircleView.setVisibility(View.GONE);
				setColorViewAlpha(MAX_ALPHA);
				// Return the circle to its start position
				if (mScale) {
					// 小圆圈设置了缩放，把缩放设置为0
					setAnimationProgress(0 /* animation complete and view is hidden */);
				} else {
					// 小圆圈没有设置缩放，小圆圈返回到其实位置
					setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop, true /* requires update */);
				}
			}
			mCurrentTargetOffsetTop = mCircleView.getTop();
		}
	};

	private void setColorViewAlpha(int targetAlpha) {
		mCircleView.getBackground().setAlpha(targetAlpha);
		mProgress.setAlpha(targetAlpha);
	}

	/**
	 * The refresh indicator starting and resting position is always positioned near the top of the refreshing content. This position is a
	 * consistent location, but can be adjusted in either direction based on whether or not there is a toolbar or actionbar present.
	 *
	 * @param scale Set to true if there is no view at a higher z-order than where the progress spinner is set to appear.
	 * @param start The offset in pixels from the top of this view at which the progress spinner should appear.
	 * @param end   The offset in pixels from the top of this view at which the progress spinner should come to rest after a successful
	 *              swipe gesture.
	 */
	public void setProgressViewOffset(boolean scale, int start, int end) {
		mScale = scale;
		mCircleView.setVisibility(View.GONE);
		mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
		mSpinnerFinalOffset = end;
		mUsingCustomStart = true;
		mCircleView.invalidate();
	}

	/**
	 * The refresh indicator resting position is always positioned near the top of the refreshing content. This position is a consistent
	 * location, but can be adjusted in either direction based on whether or not there is a toolbar or actionbar present.
	 *
	 * @param scale Set to true if there is no view at a higher z-order than where the progress spinner is set to appear.
	 * @param end   The offset in pixels from the top of this view at which the progress spinner should come to rest after a successful
	 *              swipe gesture.
	 */
	public void setProgressViewEndTarget(boolean scale, int end) {
		mSpinnerFinalOffset = end;
		mScale = scale;
		mCircleView.invalidate();
	}

	/**
	 * One of DEFAULT, or LARGE.
	 */
	public void setSize(int size) {
		if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
			return;
		}
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		if (size == MaterialProgressDrawable.LARGE) {
			mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
		} else {
			mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
		}
		// force the bounds of the progress circle inside the circle view to
		// update by setting it to null before updating its size and then
		// re-setting it
		mCircleView.setImageDrawable(null);
		mProgress.updateSizes(size);
		mCircleView.setImageDrawable(mProgress);
	}

	/**
	 * Simple constructor to use when creating a SwipeRefreshLayout from code.
	 */
	public SwipeRefreshLayout(Context context) {
		this(context, null);
	}

	/**
	 * Constructor that is called when inflating SwipeRefreshLayout from XML.
	 */
	public SwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

		setWillNotDraw(false);
		mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

		final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
		setEnabled(a.getBoolean(0, true));
		a.recycle();

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
		mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

		createProgressView();
		ViewCompat.setChildrenDrawingOrderEnabled(this, true);
		// the absolute offset has to take into account that the circle starts at an offset
		mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
		mTotalDragDistance = mSpinnerFinalOffset;
		mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

		mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
		setNestedScrollingEnabled(true);
	}

	protected int getChildDrawingOrder(int childCount, int i) {
		if (mCircleViewIndex < 0) {
			return i;
		} else if (i == childCount - 1) {
			// Draw the selected child last
			return mCircleViewIndex;
		} else if (i >= mCircleViewIndex) {
			// Move the children after the selected child earlier one
			return i + 1;
		} else {
			// Keep the children before the selected child the same
			return i;
		}
	}

	private void createProgressView() {
		mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
		mProgress = new MaterialProgressDrawable(getContext(), this);
		mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
		mCircleView.setImageDrawable(mProgress);
		mCircleView.setVisibility(View.GONE);
		addView(mCircleView);
	}

	/**
	 * Set the listener to be notified when a refresh is triggered via the swipe gesture.
	 */
	public void setOnRefreshListener(OnRefreshListener listener) {
		mListener = listener;
	}

	/**
	 * Pre API 11, alpha is used to make the progress circle appear instead of scale.
	 */
	private boolean isAlphaUsedForScale() {
		return android.os.Build.VERSION.SDK_INT < 11;
	}

	/**
	 * Notify the widget that refresh state has changed. Do not call this when refresh is triggered by a swipe gesture.
	 *
	 * @param refreshing Whether or not the view should show refresh progress.
	 */
	public void setRefreshing(boolean refreshing) {
		if (refreshing && mRefreshing != refreshing) {
			// scale and show
			mRefreshing = refreshing;
			// 进入刷新状态小圆圈停留的位置
			int endTarget = 0;
			if (!mUsingCustomStart) {
				endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
			} else {
				endTarget = (int) mSpinnerFinalOffset;
			}
			setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop, true /* requires update */);
			mNotify = false;
			// 把小圆圈设置scale设置为可见
			startScaleUpAnimation(mRefreshListener);
		} else {
			setRefreshing(refreshing, false /* notify */);
		}
	}

	// 小圆圈scale设置为1
	private void startScaleUpAnimation(AnimationListener listener) {
		mCircleView.setVisibility(View.VISIBLE);
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			// Pre API 11, alpha is used in place of scale up to show the
			// progress circle appearing.
			// Don't adjust the alpha during appearance otherwise.
			mProgress.setAlpha(MAX_ALPHA);
		}
		mScaleAnimation = new Animation() {
			@Override
			public void applyTransformation(float interpolatedTime, Transformation t) {
				setAnimationProgress(interpolatedTime);
			}
		};
		mScaleAnimation.setDuration(mMediumAnimationDuration);
		if (listener != null) {
			mCircleView.setAnimationListener(listener);
		}
		mCircleView.clearAnimation();
		mCircleView.startAnimation(mScaleAnimation);
	}

	/**
	 * mCircleView做缩放操作
	 */
	private void setAnimationProgress(float progress) {
		if (isAlphaUsedForScale()) {
			setColorViewAlpha((int) (progress * MAX_ALPHA));
		} else {
			ViewCompat.setScaleX(mCircleView, progress);
			ViewCompat.setScaleY(mCircleView, progress);
		}
	}

	/**
	 * 是指是否进入刷新状态
	 * @param refreshing: 是否进入刷新状态
	 * @param notify:是否通知上层，SwipeRefreshLayout的时候定义OnRefreshListener的监听
	 */
	private void setRefreshing(boolean refreshing, final boolean notify) {
		if (mRefreshing != refreshing) {
			// 当前状态不相同
			mNotify = notify;
			ensureTarget();
			mRefreshing = refreshing;
			if (mRefreshing) {
				// 进入刷新状态，
				animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
			} else {
				// 进入非刷新状态，直接scale缩小为0了
				startScaleDownAnimation(mRefreshListener);
			}
		}
	}

	private void startScaleDownAnimation(AnimationListener listener) {
		mScaleDownAnimation = new Animation() {
			@Override
			public void applyTransformation(float interpolatedTime, Transformation t) {
				setAnimationProgress(1 - interpolatedTime);
			}
		};
		mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
		mCircleView.setAnimationListener(listener);
		mCircleView.clearAnimation();
		mCircleView.startAnimation(mScaleDownAnimation);
	}

	// 启动一个alpha变化的动画，从当前值到STARTING_PROGRESS_ALPHA的变化
	private void startProgressAlphaStartAnimation() {
		mAlphaStartAnimation = startAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
	}

	// 启动一个alpha变化的动画，从当前值到MAX_ALPHA的变化
	private void startProgressAlphaMaxAnimation() {
		mAlphaMaxAnimation = startAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
	}

	private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
		// Pre API 11, alpha is used in place of scale. Don't also use it to
		// show the trigger point.
		if (mScale && isAlphaUsedForScale()) {
			return null;
		}
		Animation alpha = new Animation() {
			@Override
			public void applyTransformation(float interpolatedTime, Transformation t) {
				mProgress.setAlpha((int) (startingAlpha + ((endingAlpha - startingAlpha) * interpolatedTime)));
			}
		};
		alpha.setDuration(ALPHA_ANIMATION_DURATION);
		// Clear out the previous animation listeners.
		mCircleView.setAnimationListener(null);
		mCircleView.clearAnimation();
		mCircleView.startAnimation(alpha);
		return alpha;
	}

	/**
	 * @deprecated Use {@link #setProgressBackgroundColorSchemeResource(int)}
	 */
	@Deprecated
	public void setProgressBackgroundColor(int colorRes) {
		setProgressBackgroundColorSchemeResource(colorRes);
	}

	/**
	 * Set the background color of the progress spinner disc.
	 *
	 * @param colorRes Resource id of the color.
	 */
	public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes) {
		setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
	}

	/**
	 * Set the background color of the progress spinner disc.
	 */
	public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
		mCircleView.setBackgroundColor(color);
		mProgress.setBackgroundColor(color);
	}

	/**
	 * Set the color resources used in the progress animation from color resources. The first color will also be the color of the bar that
	 * grows in response to a user swipe gesture.
	 */
	public void setColorSchemeResources(@ColorRes int... colorResIds) {
		final Resources res = getResources();
		int[] colorRes = new int[colorResIds.length];
		for (int i = 0; i < colorResIds.length; i++) {
			colorRes[i] = res.getColor(colorResIds[i]);
		}
		setColorSchemeColors(colorRes);
	}

	/**
	 * Set the colors used in the progress animation. The first color will also be the color of the bar that grows in response to a user
	 * swipe gesture.
	 */
	public void setColorSchemeColors(int... colors) {
		ensureTarget();
		mProgress.setColorSchemeColors(colors);
	}

	/**
	 * @return Whether the SwipeRefreshWidget is actively showing refresh progress.
	 */
	public boolean isRefreshing() {
		return mRefreshing;
	}

	private void ensureTarget() {
		// 取了第一个不是mCircleView的view作为mTarget View
		if (mTarget == null) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				if (!child.equals(mCircleView)) {
					mTarget = child;
					break;
				}
			}
		}
	}

	/**
	 * Set the distance to trigger a sync in dips
	 */
	public void setDistanceToTriggerSync(int distance) {
		mTotalDragDistance = distance;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int width = getMeasuredWidth();
		final int height = getMeasuredHeight();
		if (getChildCount() == 0) {
			return;
		}
		if (mTarget == null) {
			ensureTarget();
		}
		if (mTarget == null) {
			return;
		}
		final View child = mTarget;
		final int childLeft = getPaddingLeft();
		final int childTop = getPaddingTop();
		final int childWidth = width - getPaddingLeft() - getPaddingRight();
		final int childHeight = height - getPaddingTop() - getPaddingBottom();
		// 设置mTarget的位置，正常布局没啥看头
		child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
		int circleWidth = mCircleView.getMeasuredWidth();
		int circleHeight = mCircleView.getMeasuredHeight();
		// 设置mCircleView，也是正常布局就偏移了mCurrentTargetOffsetTop的高度，这个好理解咱mCircleView是会上下滑动的
		mCircleView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop, (width / 2 + circleWidth / 2),
						   mCurrentTargetOffsetTop + circleHeight);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mTarget == null) {
			ensureTarget();
		}
		if (mTarget == null) {
			return;
		}
		// mTarget这个就是咱们的内容控件，直接适用了SwipeRefreshLayout的整个大小
		mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
		// mCircleView这个就是咱们下拉和加载的时候显示的那个小圆圈在构造函数中addView，给了确定的大小，具体可以参SwipeRefreshLayout的构造函数
		mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
							MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
		// 确定mCircleView初始的偏移位置和当前位置
		if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
			mOriginalOffsetCalculated = true;
			mCurrentTargetOffsetTop = mOriginalOffsetTop = -mCircleView.getMeasuredHeight();
		}
		// mCircleView在SwipeRefreshLayout中的子View的index
		mCircleViewIndex = -1;
		// Get the index of the circleview.
		for (int index = 0; index < getChildCount(); index++) {
			if (getChildAt(index) == mCircleView) {
				mCircleViewIndex = index;
				break;
			}
		}
	}

	/**
	 * Get the diameter of the progress circle that is displayed as part of the swipe to refresh layout. This is not valid until a measure
	 * pass has completed.
	 *
	 * @return Diameter in pixels of the progress circle view.
	 */
	public int getProgressCircleDiameter() {
		return mCircleView != null ? mCircleView.getMeasuredHeight() : 0;
	}

	/**
	 * 就是去判断mTarget是否有向上滑动，有一个向上的scroll。如果有这个时候肯定是不能下拉刷新的吧
	 */
	public boolean canChildScrollUp() {
		if (android.os.Build.VERSION.SDK_INT < 14) {
			if (mTarget instanceof AbsListView) {
				final AbsListView absListView = (AbsListView) mTarget;
				return absListView.getChildCount() > 0 &&
					   (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
			} else {
				return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
			}
		} else {
			return ViewCompat.canScrollVertically(mTarget, -1);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		ensureTarget();

		final int action = MotionEventCompat.getActionMasked(ev);

		// mReturningToStart好像没啥作用，一直是false
		if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
			mReturningToStart = false;
		}

		// 如果mTarget这个时候有向上滑动有scroll y（这个时候是不满足下拉刷新的条件的），或者正在刷新。事件不拦截个字View去处理。
		// 从这里也可以看出当正在刷新的时候子View还是会想要按键事件的。
		if (!isEnabled() || mReturningToStart || canChildScrollUp() || mRefreshing) {
			// 不拦截
			return false;
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN://ACTION_DOWN这个时间是不拦截的
				// mCircleView移动到起始位置。(mOriginalOffsetTop设置的初始位置+mCircleView设置的top位置)
				setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCircleView.getTop(), true);
				mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
				// 标记下拉是否开始了
				mIsBeingDragged = false;
				final float initialDownY = getMotionEventY(ev, mActivePointerId);
				if (initialDownY == -1) {
					return false;
				}
				mInitialDownY = initialDownY;
				break;

			case MotionEvent.ACTION_MOVE:
				if (mActivePointerId == INVALID_POINTER) {
					Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
					return false;
				}

				final float y = getMotionEventY(ev, mActivePointerId);
				if (y == -1) {
					return false;
				}
				final float yDiff = y - mInitialDownY;
				// y方向有滑动
				if (yDiff > mTouchSlop && !mIsBeingDragged) {
					mInitialMotionY = mInitialDownY + mTouchSlop;
					// 下拉开始，从这个时候开始当前事件一直到ACTION_UP之间的事件我们是会拦截下来的
					mIsBeingDragged = true;
					mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
				}
				break;

			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				break;
		}
		return mIsBeingDragged;
	}

	private float getMotionEventY(MotionEvent ev, int activePointerId) {
		final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
		if (index < 0) {
			return -1;
		}
		return MotionEventCompat.getY(ev, index);
	}

	@Override
	public void requestDisallowInterceptTouchEvent(boolean b) {
		// if this is a List < L or another view that doesn't support nested
		// scrolling, ignore this request so that the vertical scroll event
		// isn't stolen
		if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView) ||
			(mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
			// Nope.
		} else {
			super.requestDisallowInterceptTouchEvent(b);
		}
	}

	// NestedScrollingParent

	@Override
	public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
		if (isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0) {
			// Dispatch up to the nested parent
			startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
			return true;
		}
		return false;
	}

	@Override
	public void onNestedScrollAccepted(View child, View target, int axes) {
		// Reset the counter of how much leftover scroll needs to be consumed.
		mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
		mTotalUnconsumed = 0;
	}

	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
		// If we are in the middle of consuming, a scroll, then we want to move the spinner back up
		// before allowing the list to scroll
		if (dy > 0 && mTotalUnconsumed > 0) {
			if (dy > mTotalUnconsumed) {
				consumed[1] = dy - (int) mTotalUnconsumed;
				mTotalUnconsumed = 0;
			} else {
				mTotalUnconsumed -= dy;
				consumed[1] = dy;

			}
			moveSpinner(mTotalUnconsumed);
		}

		// Now let our nested parent consume the leftovers
		final int[] parentConsumed = mParentScrollConsumed;
		if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
			consumed[0] += parentConsumed[0];
			consumed[1] += parentConsumed[1];
		}
	}

	@Override
	public int getNestedScrollAxes() {
		return mNestedScrollingParentHelper.getNestedScrollAxes();
	}

	@Override
	public void onStopNestedScroll(View target) {
		mNestedScrollingParentHelper.onStopNestedScroll(target);
		// Finish the spinner for nested scrolling if we ever consumed any
		// unconsumed nested scroll
		if (mTotalUnconsumed > 0) {
			finishSpinner(mTotalUnconsumed);
			mTotalUnconsumed = 0;
		}
		// Dispatch up our nested parent
		stopNestedScroll();
	}

	@Override
	public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
		if (dyUnconsumed < 0) {
			dyUnconsumed = Math.abs(dyUnconsumed);
			mTotalUnconsumed += dyUnconsumed;
			moveSpinner(mTotalUnconsumed);
		}
		// Dispatch up to the nested parent
		dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dxConsumed, null);
	}

	// NestedScrollingChild

	@Override
	public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
		return false;
	}

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return mNestedScrollingChildHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return mNestedScrollingChildHelper.startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		mNestedScrollingChildHelper.stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return mNestedScrollingChildHelper.hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}

	private boolean isAnimationRunning(Animation animation) {
		return animation != null && animation.hasStarted() && !animation.hasEnded();
	}

	/**
	 * 下拉过程中调用该函数
	 * @param overscrollTop:表示y轴上下拉的距离
	 */
	private void moveSpinner(float overscrollTop) {
		mProgress.showArrow(true);
		// 相对于刷新距离滑动了百分之多少（注意如果超过了刷新的距离这个值会大于1的）
		float originalDragPercent = overscrollTop / mTotalDragDistance;
		// 控制最大值为1 dragPercent == 1 表示滑动距离已经到了刷新的条件了
		float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
		// 调整下百分比(小于0.4的情况下设置为0)
		float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
		// 相对于进入刷新的位置的偏移量，注意这个值可能是负数。负数表示还没有达到刷新的距离
		float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
		// 这里去计算小圆圈在Y轴上面可以滑动到的距离（targetY）为啥要这样算就没搞明白
		float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset - mOriginalOffsetTop : mSpinnerFinalOffset;
		float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2) / slingshotDist);
		float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
		float extraMove = (slingshotDist) * tensionPercent * 2;
		int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
		// 在手指滑动的过程中mCircleView小圆圈是可见的
		if (mCircleView.getVisibility() != View.VISIBLE) {
			mCircleView.setVisibility(View.VISIBLE);
		}
		if (!mScale) {
			// 在滑动过程中小圆圈设置不缩放，x,y scale都设置为1
			ViewCompat.setScaleX(mCircleView, 1f);
			ViewCompat.setScaleY(mCircleView, 1f);
		}
		if (overscrollTop < mTotalDragDistance) {
			// 还没达到刷新的距离的时候
			if (mScale) {
				// 如果设置了小圆圈在滑动的过程中可以缩放，scale慢慢的变大
				setAnimationProgress(overscrollTop / mTotalDragDistance);
			}
			if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA && !isAnimationRunning(mAlphaStartAnimation)) {
				// 其实这里也可以看出来，在没有达到刷新距离的时候，alpha会尽量保持是STARTING_PROGRESS_ALPHA的(相对来说模糊点)
				startProgressAlphaStartAnimation();
			}
			float strokeStart = adjustedPercent * .8f;
			// 设置小圆圈里面进度条的开始和结束位置(在还没有达到刷新距离的时候小圆圈里面进度条是慢慢变大的，最多达到80%的圈)
			mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
			// 设置mCircleView小圆圈里面进度条箭头的缩放大小(在还没有达到刷新距离的时候小圆圈进度条箭头是慢慢变大的)
			mProgress.setArrowScale(Math.min(1f, adjustedPercent));
		} else {
			// 达到了刷新的距离的时候（注意这个时候小圆圈里面进度条占80%，并且是可见的）
			if (mProgress.getAlpha() < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
				// 其实这里也可以看出来，在达到刷新距离的时候，alpha会尽量保持是MAX_ALPHA的(完全显示)
				startProgressAlphaMaxAnimation();
			}
		}
		float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
		// 设置小圆圈进度条的旋转角度，在下拉的过程中mCircleView小圆圈是一点一点往前旋转的
		mProgress.setProgressRotation(rotation);
		// mCircleView会随着手指往下移动
		setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true /* requires update */);
	}

	/**
	 * 下拉结束的时候调用该函数
	 * @param overscrollTop: 表示y轴上下拉的距离
	 */
	private void finishSpinner(float overscrollTop) {
		if (overscrollTop > mTotalDragDistance) {
			// 下拉结束的时候达到了刷新的距离，这个时候就要告诉上层该进入刷新了
			setRefreshing(true, true /* notify */);
		} else {
			// 下拉结束的时候还没有达到刷新的距离
			mRefreshing = false;
			// 小圆圈进度条消失
			mProgress.setStartEndTrim(0f, 0f);
			AnimationListener listener = null;
			if (!mScale) {
				// 小圆圈没有设置缩放
				listener = new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (!mScale) {
							// 如果小圆圈没有设置缩放，当会到了初始位置之后scale缩小为0，不可见
							startScaleDownAnimation(null);
						}
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

				};
			}
			// 小圆圈从当前位置返回到初始位置
			animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
			// 小圆圈里面进度条不显示箭头了
			mProgress.showArrow(false);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = MotionEventCompat.getActionMasked(ev);

		if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
			mReturningToStart = false;
		}
		// 如果mTarget这个时候有向上滑动有scroll, SwipeRefreshLayout不对该事件做处理
		if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
			return false;
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
				mIsBeingDragged = false;
				break;

			case MotionEvent.ACTION_MOVE: {
				final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
				if (pointerIndex < 0) {
					Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
					return false;
				}

				final float y = MotionEventCompat.getY(ev, pointerIndex);
				final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
				if (mIsBeingDragged) {
					if (overscrollTop > 0) {
						// 可以处理下拉了，mCircleView会随着手指往下移动了
						moveSpinner(overscrollTop);
					} else {
						return false;
					}
				}
				break;
			}
			case MotionEventCompat.ACTION_POINTER_DOWN: {
				final int index = MotionEventCompat.getActionIndex(ev);
				mActivePointerId = MotionEventCompat.getPointerId(ev, index);
				break;
			}

			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				if (mActivePointerId == INVALID_POINTER) {
					if (action == MotionEvent.ACTION_UP) {
						Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
					}
					return false;
				}
				final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
				mIsBeingDragged = false;
				// 是否触摸的时候，mCircleView会到指定的位置，必要的话进入刷新的状态
				finishSpinner(overscrollTop);
				mActivePointerId = INVALID_POINTER;
				return false;
			}
		}

		return true;
	}

	private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
		mFrom = from;
		mAnimateToCorrectPosition.reset();
		mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
		mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
		if (listener != null) {
			mCircleView.setAnimationListener(listener);
		}
		mCircleView.clearAnimation();
		mCircleView.startAnimation(mAnimateToCorrectPosition);
	}

	private void peek(int from, AnimationListener listener) {
		mFrom = from;
		mPeek.reset();
		mPeek.setDuration(500);
		mPeek.setInterpolator(mDecelerateInterpolator);
		if (listener != null) {
			mCircleView.setAnimationListener(listener);
		}
		mCircleView.clearAnimation();
		mCircleView.startAnimation(mPeek);
	}

	private void animateOffsetToStartPosition(int from, AnimationListener listener) {
		if (mScale) {
			// Scale the item back down
			startScaleDownReturnToStartAnimation(from, listener);
		} else {
			mFrom = from;
			mAnimateToStartPosition.reset();
			mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
			mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
			if (listener != null) {
				mCircleView.setAnimationListener(listener);
			}
			mCircleView.clearAnimation();
			mCircleView.startAnimation(mAnimateToStartPosition);
		}
	}

	private final Animation mAnimateToCorrectPosition = new Animation() {
		@Override
		public void applyTransformation(float interpolatedTime, Transformation t) {
			int targetTop = 0;
			int endTarget = 0;
			if (!mUsingCustomStart) {
				endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
			} else {
				endTarget = (int) mSpinnerFinalOffset;
			}
			targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
			int offset = targetTop - mCircleView.getTop();
			setTargetOffsetTopAndBottom(offset, false /* requires update */);
			mProgress.setArrowScale(1 - interpolatedTime);
		}
	};

	private final Animation mPeek = new Animation() {
		@Override
		public void applyTransformation(float interpolatedTime, Transformation t) {
			int targetTop = 0;
			int endTarget = 0;
			if (!mUsingCustomStart) {
				endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
			} else {
				endTarget = (int) mSpinnerFinalOffset; //mSpinnerFinalOffset;
			}
			targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
			int offset = targetTop - mCircleView.getTop();
			setTargetOffsetTopAndBottom(offset, false /* requires update */);
			mProgress.setArrowScale(1 - interpolatedTime);
		}
	};

	// 小圆圈慢慢的返回到开始的位置
	private void moveToStart(float interpolatedTime) {
		int targetTop = 0;
		targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
		int offset = targetTop - mCircleView.getTop();
		setTargetOffsetTopAndBottom(offset, false /* requires update */);
	}

	// 小圆圈从当前位置返回初始位置
	private final Animation mAnimateToStartPosition = new Animation() {
		@Override
		public void applyTransformation(float interpolatedTime, Transformation t) {
			moveToStart(interpolatedTime);
		}
	};

	private void startScaleDownReturnToStartAnimation(int from, AnimationListener listener) {
		mFrom = from;
		if (isAlphaUsedForScale()) {
			mStartingScale = mProgress.getAlpha();
		} else {
			mStartingScale = ViewCompat.getScaleX(mCircleView);
		}
		mScaleDownToStartAnimation = new Animation() {
			@Override
			public void applyTransformation(float interpolatedTime, Transformation t) {
				float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
				setAnimationProgress(targetScale);
				moveToStart(interpolatedTime);
			}
		};
		mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
		if (listener != null) {
			mCircleView.setAnimationListener(listener);
		}
		mCircleView.clearAnimation();
		mCircleView.startAnimation(mScaleDownToStartAnimation);
	}

	private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
		mCircleView.bringToFront();
		mCircleView.offsetTopAndBottom(offset);
		// 记录当前的偏移位置
		mCurrentTargetOffsetTop = mCircleView.getTop();
		if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
			invalidate();
		}
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
		}
	}

	/**
	 * Classes that wish to be notified when the swipe gesture correctly triggers a refresh should implement this interface.
	 */
	public interface OnRefreshListener {

		public void onRefresh();
	}
}
