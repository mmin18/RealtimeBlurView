package com.github.mmin18.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

final class BlurImpl {
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mBlurScript;
    private Allocation mBlurInput, mBlurOutput;

    boolean setupScript(Context context, boolean debug) {
        if (mRenderScript == null) {
            try {
                mRenderScript = RenderScript.create(context);
                mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
            } catch (android.support.v8.renderscript.RSRuntimeException e) {
                if (debug) {
                    if (e.getMessage() != null && e.getMessage().startsWith("Error loading RS jni library: java.lang.UnsatisfiedLinkError:")) {
                        throw new RuntimeException("Error loading RS jni library, Upgrade buildToolsVersion=\"24.0.2\" or higher may solve this issue");
                    } else {
                        throw e;
                    }
                } else {
                    // In release mode, just ignore
                    releaseScript();
                    return false;
                }
            }
        }

        return true;
    }

    void releaseScript() {
        if (mRenderScript != null) {
            mRenderScript.destroy();
            mRenderScript = null;
        }
        if (mBlurScript != null) {
            mBlurScript.destroy();
            mBlurScript = null;
        }
    }

    void setupBitmap(Bitmap bitmapToBlur) {
        mBlurInput = Allocation.createFromBitmap(mRenderScript, bitmapToBlur,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());
    }

    void releaseBitmap() {
        if (mBlurInput != null) {
            mBlurInput.destroy();
            mBlurInput = null;
        }
        if (mBlurOutput != null) {
            mBlurOutput.destroy();
            mBlurOutput = null;
        }
    }

    void setRadius(float radius) {
        mBlurScript.setRadius(radius);
    }

    void blur(Bitmap bitmapToBlur, Bitmap blurredBitmap) {
        mBlurInput.copyFrom(bitmapToBlur);
        mBlurScript.setInput(mBlurInput);
        mBlurScript.forEach(mBlurOutput);
        mBlurOutput.copyTo(blurredBitmap);
    }
}
