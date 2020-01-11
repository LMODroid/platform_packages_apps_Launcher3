package com.android.launcher3.icons.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import com.android.launcher3.util.Preconditions;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class CustomClock {

    public static Drawable getClock(Context context, Drawable drawable, Metadata metadata) {
        ClockLayers clone = getClockLayers(drawable, metadata).clone();
        if (clone != null) {
            clone.updateAngles();
            return clone.mDrawable;
        }
        return null;
    }

    private static ClockLayers getClockLayers(Drawable drawableForDensity, Metadata metadata) {
        Preconditions.assertWorkerThread();
        ClockLayers layers = new ClockLayers();
        layers.setDrawable(drawableForDensity.mutate());
        layers.mHourIndex = metadata.HOUR_LAYER_INDEX;
        layers.mMinuteIndex = metadata.MINUTE_LAYER_INDEX;
        layers.mSecondIndex = metadata.SECOND_LAYER_INDEX;
        layers.mDefaultHour = metadata.DEFAULT_HOUR;
        layers.mDefaultMinute = metadata.DEFAULT_MINUTE;
        layers.mDefaultSecond = metadata.DEFAULT_SECOND;

        LayerDrawable layerDrawable = layers.mLayerDrawable;
        int numberOfLayers = layerDrawable.getNumberOfLayers();
        if (layers.mHourIndex < 0 || layers.mHourIndex >= numberOfLayers) {
            layers.mHourIndex = -1;
        }
        if (layers.mMinuteIndex < 0 || layers.mMinuteIndex >= numberOfLayers) {
            layers.mMinuteIndex = -1;
        }
        if (layers.mSecondIndex < 0 || layers.mSecondIndex >= numberOfLayers) {
            layers.mSecondIndex = -1;
        }
        return layers;
    }

    public static class Metadata {
        final int HOUR_LAYER_INDEX;
        final int MINUTE_LAYER_INDEX;
        final int SECOND_LAYER_INDEX;

        final int DEFAULT_HOUR;
        final int DEFAULT_MINUTE;
        final int DEFAULT_SECOND;

        public Metadata(int hourIndex, int minuteIndex, int secondIndex,
                        int defaultHour, int defaultMinute, int defaultSecond) {
            HOUR_LAYER_INDEX = hourIndex;
            MINUTE_LAYER_INDEX = minuteIndex;
            SECOND_LAYER_INDEX = secondIndex;
            DEFAULT_HOUR = defaultHour;
            DEFAULT_MINUTE = defaultMinute;
            DEFAULT_SECOND = defaultSecond;
        }
    }
}
