package com.tuacy.refreshloadlib.refresh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class MaterialProgressDrawable extends Drawable implements Animatable {

	private static final Interpolator LINEAR_INTERPOLATOR   = new LinearInterpolator();
	private static final Interpolator MATERIAL_INTERPOLATOR = new FastOutSlowInInterpolator();

	private static final float FULL_ROTATION = 1080.0f;

	@Retention(RetentionPolicy.CLASS)
	@IntDef({LARGE,
			 DEFAULT})
	public @interface ProgressDrawableSize {

	}

	// Maps to ProgressBar.Large style
	static final int LARGE   = 0;
	// Maps to ProgressBar default style
	static final int DEFAULT = 1;

	// Maps to ProgressBar default style
	private static final int   CIRCLE_DIAMETER = 40;
	private static final float CENTER_RADIUS   = 8.75f; //should add up to 10 when + stroke_width
	private static final float STROKE_WIDTH    = 2.5f;

	// Maps to ProgressBar.Large style
	private static final int   CIRCLE_DIAMETER_LARGE = 56;
	private static final float CENTER_RADIUS_LARGE   = 12.5f;
	private static final float STROKE_WIDTH_LARGE    = 3f;

	private final int[] COLORS = new int[]{Color.BLACK};

	/**
	 * The value in the linear interpolator for animating the drawable at which the color transition should start
	 */
	private static final float COLOR_START_DELAY_OFFSET    = 0.75f;
	private static final float END_TRIM_START_DELAY_OFFSET = 0.5f;
	private static final float START_TRIM_DURATION_OFFSET  = 0.5f;

	/**
	 * The duration of a single progress spin in milliseconds.
	 */
	private static final int ANIMATION_DURATION = 1332;

	/**
	 * The number of points in the progress "star".
	 */
	private static final float                NUM_POINTS = 5f;
	/**
	 * The list of animators operating on this drawable.
	 */
	private final        ArrayList<Animation> mAnimators = new ArrayList<Animation>();

	/**
	 * The indicator ring, used to manage animation state.
	 */
	private final MaterialProgressDrawable.Ring mRing;

	/**
	 * Canvas rotation in degrees.
	 */
	private float mRotation;

	/**
	 * Layout info for the arrowhead in dp
	 */
	private static final int   ARROW_WIDTH        = 10;
	private static final int   ARROW_HEIGHT       = 5;
	private static final float ARROW_OFFSET_ANGLE = 5;

	/**
	 * Layout info for the arrowhead for the large spinner in dp
	 */
	private static final int   ARROW_WIDTH_LARGE  = 12;
	private static final int   ARROW_HEIGHT_LARGE = 6;
	private static final float MAX_PROGRESS_ARC   = .8f;

	private Resources mResources;
	private View      mParent;
	private Animation mAnimation;
	private float     mRotationCount;
	private double    mWidth;
	private double    mHeight;
	boolean mFinishing;

	public MaterialProgressDrawable(Context context, View parent) {
		mParent = parent;
		mResources = context.getResources();

		mRing = new MaterialProgressDrawable.Ring(mCallback);
		mRing.setColors(COLORS);

		updateSizes(DEFAULT);
		setupAnimators();
	}

	private void setSizeParameters(double progressCircleWidth,
								   double progressCircleHeight,
								   double centerRadius,
								   double strokeWidth,
								   float arrowWidth,
								   float arrowHeight) {
		final MaterialProgressDrawable.Ring ring = mRing;
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final float screenDensity = metrics.density;

		mWidth = progressCircleWidth * screenDensity;
		mHeight = progressCircleHeight * screenDensity;
		ring.setStrokeWidth((float) strokeWidth * screenDensity);
		ring.setCenterRadius(centerRadius * screenDensity);
		ring.setColorIndex(0);
		ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity);
		ring.setInsets((int) mWidth, (int) mHeight);
	}

	/**
	 * 设置大小，可以选LARGE、DEFAULT
	 */
	public void updateSizes(@MaterialProgressDrawable.ProgressDrawableSize int size) {
		if (size == LARGE) {
			setSizeParameters(CIRCLE_DIAMETER_LARGE, CIRCLE_DIAMETER_LARGE, CENTER_RADIUS_LARGE, STROKE_WIDTH_LARGE, ARROW_WIDTH_LARGE,
							  ARROW_HEIGHT_LARGE);
		} else {
			setSizeParameters(CIRCLE_DIAMETER, CIRCLE_DIAMETER, CENTER_RADIUS, STROKE_WIDTH, ARROW_WIDTH, ARROW_HEIGHT);
		}
	}

	/**
	 * 设置是否显示箭头
	 */
	public void showArrow(boolean show) {
		mRing.setShowArrow(show);
	}

	/**
	 * 设置箭头缩放大小，0f~1f
	 */
	public void setArrowScale(float scale) {
		mRing.setArrowScale(scale);
	}

	/**
	 * 设置进度条的开始和结尾，也就是长度，范围0f~1f，比如setStartEndTrim(0f,0.8f)
	 *
	 * @param startAngle 进度条开始角度
	 * @param endAngle   精度条结束角度
	 */
	public void setStartEndTrim(float startAngle, float endAngle) {
		mRing.setStartTrim(startAngle);
		mRing.setEndTrim(endAngle);
	}

	/**
	 * 设置旋转角度，0f~1f
	 * SwipeRefreshLayout在下拉的过程中MaterialProgressDrawable是一个转圈的变化（这里的转圈不是释放的时候的那个转圈哦），
	 * 这里说的是在下拉的过程中哦
	 * @param rotation Rotation is from [0..1]
	 */
	public void setProgressRotation(float rotation) {
		mRing.setRotation(rotation);
	}

	/**
	 * 设置背景颜色
	 */
	public void setBackgroundColor(int color) {
		mRing.setBackgroundColor(color);
	}

	/**
	 * 设置进度条的颜色，可以是多种颜色，转一圈换一个
	 */
	public void setColorSchemeColors(int... colors) {
		mRing.setColors(colors);
		mRing.setColorIndex(0);
	}

	@Override
	public int getIntrinsicHeight() {
		// 给view测量的时候提供尺寸
		return (int) mHeight;
	}

	@Override
	public int getIntrinsicWidth() {
		// 给view测量的时候提供尺寸
		return (int) mWidth;
	}

	@Override
	public void draw(Canvas c) {
		// 自定义Drawable的时候draw函数是关键部分
		final Rect bounds = getBounds(); // 获取Drawable的区域
		final int saveCount = c.save();
		// 旋转mRotation角度
		c.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY());
		// 这个里面就开始画箭头和转圈的小圆环了
		mRing.draw(c, bounds);
		c.restoreToCount(saveCount);
	}

	// 设置透明度，0-255，注意:默认一开始透明度是0
	@Override
	public void setAlpha(int alpha) {
		mRing.setAlpha(alpha);
	}

	public int getAlpha() {
		return mRing.getAlpha();
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		mRing.setColorFilter(colorFilter);
	}

	void setRotation(float rotation) {
		mRotation = rotation;
		invalidateSelf();
	}

	@SuppressWarnings("unused")
	private float getRotation() {
		return mRotation;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public boolean isRunning() {
		final ArrayList<Animation> animators = mAnimators;
		final int N = animators.size();
		for (int i = 0; i < N; i++) {
			final Animation animator = animators.get(i);
			if (animator.hasStarted() && !animator.hasEnded()) {
				return true;
			}
		}
		return false;
	}

	// MaterialProgressDrawable释放的时候开始转圈动画，没转一圈换一个颜色
	@Override
	public void start() {
		mAnimation.reset();
		// 进度圆环保存一些mStartTrim，mEndTrim，mRotation设置信息
		mRing.storeOriginals();
		if (mRing.getEndTrim() != mRing.getStartTrim()) {
			// 有进度圆环的时候,这个时候做的事情会先慢慢的把这个现有的圆环慢慢的变小，然后在开始转圈
			mFinishing = true;
			mAnimation.setDuration(ANIMATION_DURATION / 2);
			mParent.startAnimation(mAnimation);
		} else {
			// 没有进度圆环的时候，直接开始转圈的动画
			mRing.setColorIndex(0);
			mRing.resetOriginals();
			mAnimation.setDuration(ANIMATION_DURATION);
			mParent.startAnimation(mAnimation);
		}
	}

	// MaterialProgressDrawable加载完成的时候终止转圈的动画，并且将进度条长度归零
	@Override
	public void stop() {
		mParent.clearAnimation();
		setRotation(0);
		mRing.setShowArrow(false);
		mRing.setColorIndex(0);
		// 并且将进度条长度归零
//		mRing.resetOriginals();
	}

	private float getMinProgressArc(MaterialProgressDrawable.Ring ring) {
		return (float) Math.toRadians(ring.getStrokeWidth() / (2 * Math.PI * ring.getCenterRadius()));
	}

	// Adapted from ArgbEvaluator.java
	private int evaluateColorChange(float fraction, int startValue, int endValue) {
		int startInt = (Integer) startValue;
		int startA = (startInt >> 24) & 0xff;
		int startR = (startInt >> 16) & 0xff;
		int startG = (startInt >> 8) & 0xff;
		int startB = startInt & 0xff;

		int endInt = (Integer) endValue;
		int endA = (endInt >> 24) & 0xff;
		int endR = (endInt >> 16) & 0xff;
		int endG = (endInt >> 8) & 0xff;
		int endB = endInt & 0xff;

		return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
			   (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
			   (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
			   (int) ((startB + (int) (fraction * (endB - startB))));
	}

	/**
	 * Update the ring color if this is within the last 25% of the animation. The new ring color will be a translation from the starting
	 * ring color to the next color.
	 */
	private void updateRingColor(float interpolatedTime, MaterialProgressDrawable.Ring ring) {
		if (interpolatedTime > COLOR_START_DELAY_OFFSET) {
			// scale the interpolatedTime so that the full
			// transformation from 0 - 1 takes place in the
			// remaining time
			ring.setColor(evaluateColorChange((interpolatedTime - COLOR_START_DELAY_OFFSET) / (1.0f - COLOR_START_DELAY_OFFSET),
											  ring.getStartingColor(), ring.getNextColor()));
		}
	}

	// 在有进度圆环的时候我们去启动转圈的动画的时候是要先把这个圆环慢慢的变小消失
	private void applyFinishTranslation(float interpolatedTime, MaterialProgressDrawable.Ring ring) {
		// 进度圆环的颜色有一个过渡的效果
		updateRingColor(interpolatedTime, ring);
		// 一次动画要转的rotation
		float targetRotation = (float) (Math.floor(ring.getStartingRotation() / MAX_PROGRESS_ARC) + 1f);
		final float minProgressArc = getMinProgressArc(ring);
		final float startTrim = ring.getStartingStartTrim() +
								(ring.getStartingEndTrim() - minProgressArc - ring.getStartingStartTrim()) * interpolatedTime;
		// 在一圈的过程中进度圆环是慢慢变小的所以setEndTrim是没变化的
		ring.setStartTrim(startTrim);
		ring.setEndTrim(ring.getStartingEndTrim());
		final float rotation = ring.getStartingRotation() + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
		// 在一圈的过程中进度圆环会慢慢往前旋转的
		ring.setRotation(rotation);
	}

	private void setupAnimators() {
		final MaterialProgressDrawable.Ring ring = mRing;
		final Animation animation = new Animation() {
			@Override
			public void applyTransformation(float interpolatedTime, Transformation t) {
				if (mFinishing) {
					// 在有进度圆环的时候我们去启动转圈的动画的时候是要先把这个圆环慢慢的变小消失
					applyFinishTranslation(interpolatedTime, ring);
				} else {
					final float minProgressArc = getMinProgressArc(ring);
					final float startingEndTrim = ring.getStartingEndTrim();
					final float startingTrim = ring.getStartingStartTrim();
					final float startingRotation = ring.getStartingRotation();
					// 每次repeat的动画在最后的25%的过程中颜色有过渡的效果
					updateRingColor(interpolatedTime, ring);

					// 每次repeat的动画的前50%的时候圆环的起始角度有一个往前移的动作
					if (interpolatedTime <= START_TRIM_DURATION_OFFSET) {
						final float scaledTime = (interpolatedTime) / (1.0f - START_TRIM_DURATION_OFFSET);
						final float startTrim = startingTrim +
												((MAX_PROGRESS_ARC - minProgressArc) * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime));
						ring.setStartTrim(startTrim);
					}

					// 每次repeat的动画的后50%的时候圆环的结束角度有一个往前移的动作
					if (interpolatedTime > END_TRIM_START_DELAY_OFFSET) {
						final float minArc = MAX_PROGRESS_ARC - minProgressArc;
						float scaledTime = (interpolatedTime - START_TRIM_DURATION_OFFSET) / (1.0f - START_TRIM_DURATION_OFFSET);
						final float endTrim = startingEndTrim + (minArc * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime));
						ring.setEndTrim(endTrim);
					}

					final float rotation = startingRotation + (0.25f * interpolatedTime);
					// 圆环旋转的效果
					ring.setRotation(rotation);

					float groupRotation = ((FULL_ROTATION / NUM_POINTS) * interpolatedTime) +
										  (FULL_ROTATION * (mRotationCount / NUM_POINTS));
					setRotation(groupRotation);
				}
			}
		};
		animation.setRepeatCount(Animation.INFINITE);
		animation.setRepeatMode(Animation.RESTART);
		animation.setInterpolator(LINEAR_INTERPOLATOR);
		animation.setAnimationListener(new Animation.AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				mRotationCount = 0;
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// do nothing
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// 转一圈圆环换一个颜色
				ring.storeOriginals();
				ring.goToNextColor();
				ring.setStartTrim(ring.getEndTrim());
				if (mFinishing) {
					// 在SwipeRefreshLayout中调用MaterialProgressDrawable类start函数的时候，
					// 如果有圆环第一次动画就是圆环慢慢消失，这里表示消失完成了
					mFinishing = false;
					animation.setDuration(ANIMATION_DURATION);
					ring.setShowArrow(false);
				} else {
					mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
				}
			}
		});
		mAnimation = animation;
	}

	private final Callback mCallback = new Callback() {
		@Override
		public void invalidateDrawable(Drawable d) {
			invalidateSelf();
		}

		@Override
		public void scheduleDrawable(Drawable d, Runnable what, long when) {
			scheduleSelf(what, when);
		}

		@Override
		public void unscheduleDrawable(Drawable d, Runnable what) {
			unscheduleSelf(what);
		}
	};

	private static class Ring {

		private final RectF mTempBounds = new RectF();
		private final Paint mPaint      = new Paint();
		private final Paint mArrowPaint = new Paint();

		private final Callback mCallback;

		private float mStartTrim   = 0.0f;
		private float mEndTrim     = 0.0f;
		private float mRotation    = 0.0f;
		private float mStrokeWidth = 5.0f;
		private float mStrokeInset = 2.5f;

		private int[]   mColors;
		// mColorIndex represents the offset into the available mColors that the
		// progress circle should currently display. As the progress circle is
		// animating, the mColorIndex moves by one to the next available color.
		private int     mColorIndex;
		private float   mStartingStartTrim;
		private float   mStartingEndTrim;
		private float   mStartingRotation;
		private boolean mShowArrow;
		private Path    mArrow;
		private float   mArrowScale;
		private double  mRingCenterRadius;
		private int     mArrowWidth;
		private int     mArrowHeight;
		private int     mAlpha;
		private final Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private int mBackgroundColor;
		private int mCurrentColor;

		public Ring(Callback callback) {
			mCallback = callback;

			mPaint.setStrokeCap(Paint.Cap.SQUARE);
			mPaint.setAntiAlias(true);
			mPaint.setStyle(Paint.Style.STROKE);

			mArrowPaint.setStyle(Paint.Style.FILL);
			mArrowPaint.setAntiAlias(true);
		}

		public void setBackgroundColor(int color) {
			mBackgroundColor = color;
		}

		/**
		 * Set the dimensions of the arrowhead.
		 *
		 * @param width  Width of the hypotenuse of the arrow head
		 * @param height Height of the arrow point
		 */
		public void setArrowDimensions(float width, float height) {
			mArrowWidth = (int) width;
			mArrowHeight = (int) height;
		}

		/**
		 * Draw the progress spinner
		 */
		public void draw(Canvas c, Rect bounds) {
			final RectF arcBounds = mTempBounds;
			arcBounds.set(bounds);
			// 进度条相对于外圈的一个内边距
			arcBounds.inset(mStrokeInset, mStrokeInset);

			final float startAngle = (mStartTrim + mRotation) * 360;
			final float endAngle = (mEndTrim + mRotation) * 360;
			float sweepAngle = endAngle - startAngle;

			mPaint.setColor(mCurrentColor);
			// 画进度圆环（环的宽度setStrokeWidth）
			c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);

			// 如果需要的话，画箭头
			drawTriangle(c, startAngle, sweepAngle, bounds);

			if (mAlpha < 255) {
				// 在上面覆盖一层alpha，达到透明的效果
				mCirclePaint.setColor(mBackgroundColor);
				mCirclePaint.setAlpha(255 - mAlpha);
				c.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), bounds.width() / 2, mCirclePaint);
			}
		}

		private void drawTriangle(Canvas c, float startAngle, float sweepAngle, Rect bounds) {
			if (mShowArrow) {
				// 如果现实箭头
				if (mArrow == null) {
					mArrow = new Path();
					mArrow.setFillType(Path.FillType.EVEN_ODD);
				} else {
					mArrow.reset();
				}

				// 找到三角形箭头要偏移的位置（x，y方向要偏移的位置）
				float inset = (int) mStrokeInset / 2 * mArrowScale;
				float x = (float) (mRingCenterRadius * Math.cos(0) + bounds.exactCenterX());
				float y = (float) (mRingCenterRadius * Math.sin(0) + bounds.exactCenterY());

				// 先确定三角形箭头的三个点，在偏移到0度角的位置，然后再旋转进度条扫过的角度，在封闭形成三角形箭头
				mArrow.moveTo(0, 0);
				mArrow.lineTo(mArrowWidth * mArrowScale, 0);
				mArrow.lineTo((mArrowWidth * mArrowScale / 2), (mArrowHeight * mArrowScale));
				mArrow.offset(x - inset, y);
				mArrow.close();
				// draw a triangle
				mArrowPaint.setColor(mCurrentColor);
				c.rotate(startAngle + sweepAngle - ARROW_OFFSET_ANGLE, bounds.exactCenterX(), bounds.exactCenterY());
				c.drawPath(mArrow, mArrowPaint);
			}
		}

		/**
		 * Set the colors the progress spinner alternates between.
		 *
		 * @param colors Array of integers describing the colors. Must be non-<code>null</code>.
		 */
		public void setColors(@NonNull int[] colors) {
			mColors = colors;
			// if colors are reset, make sure to reset the color index as well
			setColorIndex(0);
		}

		/**
		 * Set the absolute color of the progress spinner. This is should only be used when animating between current and next color when
		 * the spinner is rotating.
		 *
		 * @param color int describing the color.
		 */
		public void setColor(int color) {
			mCurrentColor = color;
		}

		/**
		 * @param index Index into the color array of the color to display in the progress spinner.
		 */
		public void setColorIndex(int index) {
			mColorIndex = index;
			mCurrentColor = mColors[mColorIndex];
		}

		/**
		 * @return int describing the next color the progress spinner should use when drawing.
		 */
		public int getNextColor() {
			return mColors[getNextColorIndex()];
		}

		private int getNextColorIndex() {
			return (mColorIndex + 1) % (mColors.length);
		}

		/**
		 * Proceed to the next available ring color. This will automatically wrap back to the beginning of colors.
		 */
		public void goToNextColor() {
			setColorIndex(getNextColorIndex());
		}

		public void setColorFilter(ColorFilter filter) {
			mPaint.setColorFilter(filter);
			invalidateSelf();
		}

		/**
		 * @param alpha Set the alpha of the progress spinner and associated arrowhead.
		 */
		public void setAlpha(int alpha) {
			mAlpha = alpha;
		}

		/**
		 * @return Current alpha of the progress spinner and arrowhead.
		 */
		public int getAlpha() {
			return mAlpha;
		}

		/**
		 * @param strokeWidth Set the stroke width of the progress spinner in pixels.
		 */
		public void setStrokeWidth(float strokeWidth) {
			mStrokeWidth = strokeWidth;
			mPaint.setStrokeWidth(strokeWidth);
			invalidateSelf();
		}

		@SuppressWarnings("unused")
		public float getStrokeWidth() {
			return mStrokeWidth;
		}

		@SuppressWarnings("unused")
		/**
		 * 设置进度开始的角度(进度是一段圆环哦)
		 */
		public void setStartTrim(float startTrim) {
			mStartTrim = startTrim;
			invalidateSelf();
		}

		@SuppressWarnings("unused")
		public float getStartTrim() {
			return mStartTrim;
		}

		public float getStartingStartTrim() {
			return mStartingStartTrim;
		}

		public float getStartingEndTrim() {
			return mStartingEndTrim;
		}

		public int getStartingColor() {
			return mColors[mColorIndex];
		}

		@SuppressWarnings("unused")
		/**
		 * 设置进度结束的角度
		 */
		public void setEndTrim(float endTrim) {
			mEndTrim = endTrim;
			invalidateSelf();
		}

		@SuppressWarnings("unused")
		public float getEndTrim() {
			return mEndTrim;
		}

		@SuppressWarnings("unused")
		public void setRotation(float rotation) {
			mRotation = rotation;
			invalidateSelf();
		}

		@SuppressWarnings("unused")
		public float getRotation() {
			return mRotation;
		}

		public void setInsets(int width, int height) {
			final float minEdge = (float) Math.min(width, height);
			float insets;
			if (mRingCenterRadius <= 0 || minEdge < 0) {
				insets = (float) Math.ceil(mStrokeWidth / 2.0f);
			} else {
				insets = (float) (minEdge / 2.0f - mRingCenterRadius);
			}
			mStrokeInset = insets;
		}

		@SuppressWarnings("unused")
		public float getInsets() {
			return mStrokeInset;
		}

		/**
		 * @param centerRadius Inner radius in px of the circle the progress spinner arc traces.
		 */
		public void setCenterRadius(double centerRadius) {
			mRingCenterRadius = centerRadius;
		}

		public double getCenterRadius() {
			return mRingCenterRadius;
		}

		/**
		 * @param show Set to true to show the arrow head on the progress spinner.
		 */
		public void setShowArrow(boolean show) {
			if (mShowArrow != show) {
				mShowArrow = show;
				invalidateSelf();
			}
		}

		/**
		 * @param scale Set the scale of the arrowhead for the spinner.
		 */
		public void setArrowScale(float scale) {
			if (scale != mArrowScale) {
				mArrowScale = scale;
				invalidateSelf();
			}
		}

		/**
		 * @return The amount the progress spinner is currently rotated, between [0..1].
		 */
		public float getStartingRotation() {
			return mStartingRotation;
		}

		/**
		 * If the start / end trim are offset to begin with, store them so that animation starts from that offset.
		 */
		public void storeOriginals() {
			mStartingStartTrim = mStartTrim;
			mStartingEndTrim = mEndTrim;
			mStartingRotation = mRotation;
		}

		/**
		 * Reset the progress spinner to default rotation, start and end angles.
		 */
		public void resetOriginals() {
			mStartingStartTrim = 0;
			mStartingEndTrim = 0;
			mStartingRotation = 0;
			setStartTrim(0);
			setEndTrim(0);
			setRotation(0);
		}

		private void invalidateSelf() {
			mCallback.invalidateDrawable(null);
		}
	}
}
