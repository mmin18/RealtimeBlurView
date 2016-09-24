package com.github.mmin18.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

import com.github.mmin18.realtimeblurview.R;

/**
 * A realtime blurring overlay (like iOS UIVisualEffectView). Just put it above
 * the view you want to blur and it doesn't have to be in the same ViewGroup
 * <ul>
 * <li>realtimeBlurRadius (10dp)</li>
 * <li>realtimeDownsampleFactor (4)</li>
 * <li>realtimeOverlayColor (#aaffffff)</li>
 * </ul>
 */
public class RealtimeBlurView extends View {

	private float mDownsampleFactor; // default 4
	private int mOverlayColor; // default #aaffffff
	private float mBlurRadius; // default 10dp (0 < r <= 25)

	private boolean mDirty;
	private Bitmap mBitmapToBlur, mBlurredBitmap;
	private Canvas mBlurringCanvas;
	private RenderScript mRenderScript;
	private ScriptIntrinsicBlur mBlurScript;
	private Allocation mBlurInput, mBlurOutput;
	private boolean mIsRendering;
	private final Rect mRectSrc = new Rect(), mRectDst = new Rect();
	private static int RENDERING_COUNT;

	public RealtimeBlurView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RealtimeBlurView);
		mBlurRadius = a.getDimension(R.styleable.RealtimeBlurView_realtimeBlurRadius,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics()));
		mDownsampleFactor = a.getFloat(R.styleable.RealtimeBlurView_realtimeDownsampleFactor, 4);
		mOverlayColor = a.getColor(R.styleable.RealtimeBlurView_realtimeOverlayColor, 0xAAFFFFFF);
		a.recycle();
	}

	public void setBlurRadius(float radius) {
		if (mBlurRadius != radius) {
			mBlurRadius = radius;
			mDirty = true;
			invalidate();
		}
	}

	public void setDownsampleFactor(float factor) {
		if (factor <= 0) {
			throw new IllegalArgumentException("Downsample factor must be greater than 0.");
		}

		if (mDownsampleFactor != factor) {
			mDownsampleFactor = factor;
			mDirty = true; // may also change blur radius
			releaseBitmap();
			invalidate();
		}
	}

	public void setOverlayColor(int color) {
		if (mOverlayColor != color) {
			mOverlayColor = color;
			invalidate();
		}
	}

	private void releaseBitmap() {
		if (mBlurInput != null) {
			mBlurInput.destroy();
			mBlurInput = null;
		}
		if (mBlurOutput != null) {
			mBlurOutput.destroy();
			mBlurOutput = null;
		}
		if (mBitmapToBlur != null) {
			mBitmapToBlur.recycle();
			mBitmapToBlur = null;
		}
		if (mBlurredBitmap != null) {
			mBlurredBitmap.recycle();
			mBlurredBitmap = null;
		}
	}

	private void releaseScript() {
		if (mRenderScript != null) {
			mRenderScript.destroy();
			mRenderScript = null;
		}
		if (mBlurScript != null) {
			mBlurScript.destroy();
			mBlurScript = null;
		}
	}

	protected void release() {
		releaseBitmap();
		releaseScript();
	}

	protected boolean prepare() {
		if (mBlurRadius == 0) {
			release();
			return false;
		}

		float downsampleFactor = mDownsampleFactor;

		if (mDirty || mRenderScript == null) {
			if (mRenderScript == null) {
				mRenderScript = RenderScript.create(getContext());
				mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
			}

			mDirty = false;
			float radius = mBlurRadius / downsampleFactor;
			if (radius > 25) {
				downsampleFactor = downsampleFactor * radius / 25;
				radius = 25;
			}
			mBlurScript.setRadius(radius);
		}

		final int width = getWidth();
		final int height = getHeight();

		int scaledWidth = (int) (width / downsampleFactor);
		int scaledHeight = (int) (height / downsampleFactor);

		if (mBlurringCanvas == null || mBlurredBitmap == null
				|| mBlurredBitmap.getWidth() != scaledWidth
				|| mBlurredBitmap.getHeight() != scaledHeight) {
			releaseBitmap();

			mBitmapToBlur = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
			if (mBitmapToBlur == null) {
				return false;
			}

			mBlurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
			if (mBlurredBitmap == null) {
				return false;
			}
			mRectSrc.right = scaledWidth;
			mRectSrc.bottom = scaledHeight;

			mBlurringCanvas = new Canvas(mBitmapToBlur);
			mBlurInput = Allocation.createFromBitmap(mRenderScript, mBitmapToBlur,
					Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
			mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());
		}
		return true;
	}

	protected void blur() {
		mBlurInput.copyFrom(mBitmapToBlur);
		mBlurScript.setInput(mBlurInput);
		mBlurScript.forEach(mBlurOutput);
		mBlurOutput.copyTo(mBlurredBitmap);
	}

	private final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
		@Override
		public boolean onPreDraw() {
			final int[] locations = new int[2];
			if (isShown() && prepare()) {
				View decor = ((Activity) getContext()).getWindow().getDecorView();
				decor.getLocationInWindow(locations);
				int x = -locations[0];
				int y = -locations[1];

				getLocationInWindow(locations);
				x += locations[0];
				y += locations[1];

				if (decor.getBackground() instanceof ColorDrawable) {
					mBitmapToBlur.eraseColor(((ColorDrawable) decor.getBackground()).getColor());
				} else {
					mBitmapToBlur.eraseColor(Color.TRANSPARENT);
				}

				int rc = mBlurringCanvas.save();
				mIsRendering = true;
				RENDERING_COUNT++;
				try {
					mBlurringCanvas.scale(1.f * mBlurredBitmap.getWidth() / getWidth(), 1.f * mBlurredBitmap.getHeight() / getHeight());
					mBlurringCanvas.translate(-x, -y);
					decor.draw(mBlurringCanvas);
				} catch (StopException e) {
				} finally {
					mIsRendering = false;
					RENDERING_COUNT--;
					mBlurringCanvas.restoreToCount(rc);
				}

				blur();
			}

			return true;
		}
	};

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnPreDrawListener(preDrawListener);
	}

	@Override
	protected void onDetachedFromWindow() {
		getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
		release();
		super.onDetachedFromWindow();
	}

	@Override
	public void draw(Canvas canvas) {
		if (mIsRendering) {
			// Quit here, don't draw views above me
			throw STOP_EXCEPTION;
		} else if (RENDERING_COUNT > 0) {
			// Doesn't support blurview overlap on another blurview
		} else {
			super.draw(canvas);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mBlurredBitmap != null) {
			mRectDst.right = getWidth();
			mRectDst.bottom = getHeight();
			canvas.drawBitmap(mBlurredBitmap, mRectSrc, mRectDst, null);
		}
		canvas.drawColor(mOverlayColor);
	}

	private static class StopException extends RuntimeException {
	}

	private static StopException STOP_EXCEPTION = new StopException();
}
