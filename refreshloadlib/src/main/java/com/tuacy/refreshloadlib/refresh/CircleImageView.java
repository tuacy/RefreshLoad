package com.tuacy.refreshloadlib.refresh;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.view.ViewCompat;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * Private class created to work around issues with AnimationListeners being called before the animation is actually complete and support
 * shadows on older platforms.
 *
 * @hide
 */
public class CircleImageView extends ImageView {

	private static final int   KEY_SHADOW_COLOR  = 0x1E000000;
	private static final int   FILL_SHADOW_COLOR = 0x3D000000;
	// PX
	private static final float X_OFFSET          = 0f;
	private static final float Y_OFFSET          = 1.75f;
	private static final float SHADOW_RADIUS     = 3.5f;
	private static final int   SHADOW_ELEVATION  = 4;

	private Animation.AnimationListener mListener;
	// 底部阴影的大小
	private int                         mShadowRadius;

	public CircleImageView(Context context, int color, final float radius) {
		super(context);
		final float density = getContext().getResources().getDisplayMetrics().density;
		final int diameter = (int) (radius * density * 2);
		final int shadowYOffset = (int) (density * Y_OFFSET);
		final int shadowXOffset = (int) (density * X_OFFSET);

		mShadowRadius = (int) (density * SHADOW_RADIUS);

		ShapeDrawable circle;
		if (elevationSupported()) {
			// 确保是一个圆形
			circle = new ShapeDrawable(new OvalShape());
			// 如果版本支持阴影的设置，直接调用setElevation函数设置阴影效果
			ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
		} else {
			// 如果版本不支持阴影效果的设置，没办了只能自己去实现一个类似的效果了。
			// OvalShadow是继承自OvalShape自定义的一个类，用来实现类似的阴影效果(这个可能是我们的一个学习的点)。
			OvalShape oval = new OvalShadow(mShadowRadius, diameter);
			circle = new ShapeDrawable(oval);
			// 关闭硬件加速，要不绘制的阴影没有效果
			ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, circle.getPaint());
			// 设置阴影层，Y方向稍微偏移了一点点
			circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset, KEY_SHADOW_COLOR);
			final int padding = mShadowRadius;
			// 保证接下的内容不会绘制到阴影上面去，但是阴影被覆盖住。
			setPadding(padding, padding, padding, padding);
		}
		circle.getPaint().setColor(color);
		setBackgroundDrawable(circle);
	}

	private boolean elevationSupported() {
		return android.os.Build.VERSION.SDK_INT >= 21;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!elevationSupported()) {
			// 如果不支持阴影效果，把阴影的范围加进去重新设置控件的大小
			setMeasuredDimension(getMeasuredWidth() + mShadowRadius * 2, getMeasuredHeight() + mShadowRadius * 2);
		}
	}

	public void setAnimationListener(Animation.AnimationListener listener) {
		mListener = listener;
	}

	@Override
	public void onAnimationStart() {
		super.onAnimationStart();
		if (mListener != null) {
			mListener.onAnimationStart(getAnimation());
		}
	}

	@Override
	public void onAnimationEnd() {
		super.onAnimationEnd();
		if (mListener != null) {
			mListener.onAnimationEnd(getAnimation());
		}
	}

	/**
	 * Update the background color of the circle image view.
	 *
	 * @param colorRes Id of a color resource.
	 */
	public void setBackgroundColorRes(int colorRes) {
		setBackgroundColor(getContext().getResources().getColor(colorRes));
	}

	@Override
	public void setBackgroundColor(int color) {
		if (getBackground() instanceof ShapeDrawable) {
			((ShapeDrawable) getBackground()).getPaint().setColor(color);
		}
	}

	/**
	 * 继承自OvalShape，先保证图像是圆形的。重写draw方法实现一个类似阴影的效果
	 */
	private class OvalShadow extends OvalShape {

		private RadialGradient mRadialGradient;
		private Paint          mShadowPaint;
		private int            mCircleDiameter;

		public OvalShadow(int shadowRadius, int circleDiameter) {
			super();
			// 画阴影的paint
			mShadowPaint = new Paint();
			// 阴影的范围大小
			mShadowRadius = shadowRadius;
			// 直径
			mCircleDiameter = circleDiameter;
			// 环形渲染，达到阴影的效果
			mRadialGradient = new RadialGradient(mCircleDiameter / 2, mCircleDiameter / 2, mShadowRadius, new int[]{FILL_SHADOW_COLOR,
																													Color.TRANSPARENT},
												 null, Shader.TileMode.CLAMP);
			mShadowPaint.setShader(mRadialGradient);
		}

		@Override
		public void draw(Canvas canvas, Paint paint) {
			final int viewWidth = CircleImageView.this.getWidth();
			final int viewHeight = CircleImageView.this.getHeight();
			// 先画上阴影效果
			canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2 + mShadowRadius), mShadowPaint);
			// 画上内容
			canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2), paint);
		}
	}
}
