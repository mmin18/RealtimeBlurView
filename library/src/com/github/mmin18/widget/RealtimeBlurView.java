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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.github.mmin18.realtimeblurview.R;

/**
 * Created by mmin18 on 9/21/16.
 */
public class RealtimeBlurView extends View {

	private float mDownsampleFactor; // default 4
	private int mOverlayColor; // default #aaffffff
	private float mBlurRadius; // default 15

	private boolean mDownsampleFactorChanged;
	private Bitmap mBitmapToBlur, mBlurredBitmap;
	private Canvas mBlurringCanvas;
	private RenderScript mRenderScript;
	private ScriptIntrinsicBlur mBlurScript;
	private Allocation mBlurInput, mBlurOutput;
	private boolean mIsRendering;
	private final Rect mRectSrc = new Rect(), mRectDst = new Rect();

	public RealtimeBlurView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RealtimeBlurView);
		mBlurRadius = a.getInt(R.styleable.RealtimeBlurView_blurRadius, 15);
		mDownsampleFactor = a.getInt(R.styleable.RealtimeBlurView_downsampleFactor, 4);
		mOverlayColor = a.getColor(R.styleable.RealtimeBlurView_overlayColor, 0xAAFFFFFF);
		a.recycle();
	}

	public void setBlurRadius(int radius) {
		if (mBlurScript != null) {
			mBlurScript.setRadius(radius);
		}
	}

	public void setDownsampleFactor(int factor) {
		if (factor <= 0) {
			throw new IllegalArgumentException("Downsample factor must be greater than 0.");
		}

		if (mDownsampleFactor != factor) {
			mDownsampleFactor = factor;
			mDownsampleFactorChanged = true;
		}
	}

	public void setOverlayColor(int color) {
		mOverlayColor = color;
	}

	protected boolean prepare() {
		if (mRenderScript == null) {
			mRenderScript = RenderScript.create(getContext());
			mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
			mBlurScript.setRadius(mBlurRadius);
		}

		final int width = getWidth();
		final int height = getHeight();

		int scaledWidth = (int) (width / mDownsampleFactor);
		int scaledHeight = (int) (height / mDownsampleFactor);

		if (mBlurringCanvas == null || mDownsampleFactorChanged || mBlurredBitmap == null
				|| mBlurredBitmap.getWidth() != scaledWidth
				|| mBlurredBitmap.getHeight() != scaledHeight) {

			if (mBlurInput != null) {
				mBlurInput.destroy();
				mBlurInput = null;
			}
			if (mBlurOutput != null) {
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

			mDownsampleFactorChanged = false;

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
			if (isShown() && prepare()) {
				View decor = ((Activity) getContext()).getWindow().getDecorView();

				int x = getLeft(), y = getTop();
				View v = RealtimeBlurView.this;
				while (v.getParent() instanceof ViewGroup) {
					v = (View) v.getParent();
					if (v == decor)
						break;
					x += v.getLeft();
					y += v.getTop();
				}

				if (decor.getBackground() instanceof ColorDrawable) {
					mBitmapToBlur.eraseColor(((ColorDrawable) decor.getBackground()).getColor());
				} else {
					mBitmapToBlur.eraseColor(Color.TRANSPARENT);
				}

				mIsRendering = true;
				int rc = mBlurringCanvas.save();
				try {
					mBlurringCanvas.scale(1.f * mBlurredBitmap.getWidth() / getWidth(), 1.f * mBlurredBitmap.getHeight() / getHeight());
					mBlurringCanvas.translate(-x, -y);
					decor.draw(mBlurringCanvas);
				} catch (StopException e) {
				} finally {
					mBlurringCanvas.restoreToCount(rc);
					mIsRendering = false;
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
		if (mRenderScript != null) {
			mRenderScript.destroy();
			mRenderScript = null;
		}
		if (mBlurScript != null) {
			mBlurScript.destroy();
			mBlurScript = null;
		}
		super.onDetachedFromWindow();
	}

	@Override
	public void draw(Canvas canvas) {
		if (mIsRendering) {
			throw STOP_EXCEPTION;
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

			canvas.drawColor(mOverlayColor);
		}
	}

	private static class StopException extends RuntimeException {
	}

	private static StopException STOP_EXCEPTION = new StopException();
}
