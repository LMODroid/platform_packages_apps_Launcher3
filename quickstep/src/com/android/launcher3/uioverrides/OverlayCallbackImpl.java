/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.uioverrides;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.systemui.plugins.shared.LauncherOverlayManager;
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay;
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks;

import com.google.android.libraries.gsa.launcherclient.LauncherClient;
import com.google.android.libraries.gsa.launcherclient.LauncherClientCallbacks;

import java.io.PrintWriter;

/**
 * Implements {@link LauncherOverlay} and passes all the corresponding events to {@link
 * LauncherClient}. {@see setClient}
 *
 * <p>Implements {@link LauncherClientCallbacks} and sends all the corresponding callbacks to {@link
 * Launcher}.
 */
public class OverlayCallbackImpl
        implements LauncherOverlay, LauncherClientCallbacks, LauncherOverlayManager,
        SharedPreferences.OnSharedPreferenceChangeListener, ActivityLifecycleCallbacks,
        View.OnAttachStateChangeListener, OnDeviceProfileChangeListener {

    private static final String KEY_ENABLE_MINUS_ONE = "pref_enable_minus_one";

    private final Launcher mLauncher;
    private final LauncherClient mClient;

    private LauncherOverlayCallbacks mLauncherOverlayCallbacks;
    private boolean mWasOverlayAttached = false;

    public OverlayCallbackImpl(Launcher launcher) {
        SharedPreferences prefs = LauncherPrefs.getPrefs(launcher);

        mLauncher = launcher;
        mClient = new LauncherClient(mLauncher, this, getClientOptions(prefs));
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Launcher no longer forwards its lifecycle, window and device profile events to the
        // overlay manager, so follow them directly.
        mLauncher.registerActivityLifecycleCallbacks(this);
        mLauncher.addOnDeviceProfileChangeListener(this);
        View decorView = mLauncher.getWindow().getDecorView();
        decorView.addOnAttachStateChangeListener(this);
        if (decorView.isAttachedToWindow()) {
            mClient.onAttachedToWindow();
        }
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        mClient.reattachOverlay();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        mClient.onAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        mClient.onDetachedFromWindow();
    }

    @Override
    public void dump(String prefix, PrintWriter w) {
        mClient.dump(prefix, w);
    }

    @Override
    public void openOverlay() {
        mClient.showOverlay(true);
    }

    @Override
    public void hideOverlay(boolean animate) {
        mClient.hideOverlay(animate);
    }

    @Override
    public void hideOverlay(int duration) {
        mClient.hideOverlay(duration);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) { }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        mClient.onStart();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mClient.onResume();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        mClient.onPause();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mClient.onStop();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) { }

    @Override
    public void onActivityDestroyed() {
        mLauncher.unregisterActivityLifecycleCallbacks(this);
        mLauncher.removeOnDeviceProfileChangeListener(this);
        mLauncher.getWindow().getDecorView().removeOnAttachStateChangeListener(this);
        mClient.onDestroy();
        LauncherPrefs.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (KEY_ENABLE_MINUS_ONE.equals(key)) {
            mClient.setClientOptions(getClientOptions(prefs));
        }
    }

    @Override
    public void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive) {
        if (overlayAttached != mWasOverlayAttached) {
            mWasOverlayAttached = overlayAttached;
            mLauncher.setLauncherOverlay(overlayAttached ? this : null);
        }
    }

    @Override
    public void onOverlayScrollChanged(float progress) {
        if (mLauncherOverlayCallbacks != null) {
            mLauncherOverlayCallbacks.onOverlayScrollChanged(progress);
        }
    }

    @Override
    public void onScrollInteractionBegin() {
        mClient.startMove();
    }

    @Override
    public void onScrollInteractionEnd() {
        mClient.endMove();
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        mClient.updateMove(progress);
    }

    @Override
    public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) {
        mLauncherOverlayCallbacks = callbacks;
    }

    private LauncherClient.ClientOptions getClientOptions(SharedPreferences prefs) {
        return new LauncherClient.ClientOptions(
                prefs.getBoolean(KEY_ENABLE_MINUS_ONE, true),
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }
}
