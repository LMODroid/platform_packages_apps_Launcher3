/*
 * Copyright (C) 2020 Shift GmbH
 *           (C) 2025 The LibreMobileOS Foundation
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
package com.android.launcher3.settings.qsb;

import static com.android.settingslib.utils.applications.AppUtils.getApplicationLabel;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.settings.widget.RadioSettingsFragment;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QsbSettingsFragment extends RadioSettingsFragment {
    private static final String TAG = "QsbSettingsFragment";
    private static final String KEY_DISABLED = "disabled";

    private static final ComponentName GSA_QSB_SETTINGS_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.googlequicksearchbox/com.google.android.apps.search.googleapp.searchwidget.settings.customization.WidgetCustomizationActivity");

    private static final IntentFilter PKG_UPDATE_INTENT = new IntentFilter();
    static {
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_INSTALL);
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_ADDED);
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_CHANGED);
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_REMOVED);
        PKG_UPDATE_INTENT.addDataScheme("package");
    }

    private final BroadcastReceiver broadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadPreferences();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        reloadPreferences();
        getActivity().registerReceiver(broadCastReceiver, PKG_UPDATE_INTENT);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(broadCastReceiver);
    }

    @Override
    protected List<SelectorWithWidgetPreference> getPreferences(Context context) {
        final ComponentName currentProvider = getCurrentQsbProvider();
        final List<SelectorWithWidgetPreference> prefsList = new ArrayList<>();

        // Add the disabled entry first
        boolean isDisabled = currentProvider == null;
        final SelectorWithWidgetPreference disabledPref = buildPreference(context, KEY_DISABLED,
                context.getString(R.string.qsb_disabled), null, isDisabled);
        prefsList.add(disabledPref);
        if (isDisabled) {
            setSelectedPreference(disabledPref);
        }

        for (final AppWidgetProviderInfo info : getAvailableQsbProviders(context)) {
            final boolean isCurrent = info.provider.equals(currentProvider);
            final SelectorWithWidgetPreference pref = buildPreference(context,
                    info.provider.flattenToString(), info.loadLabel(context.getPackageManager()),
                    getApplicationLabel(context.getPackageManager(), info.provider.getPackageName())
                            .toString(),
                    isCurrent);
            prefsList.add(pref);

            if (isCurrent) {
                setSelectedPreference(pref);
            }
        }

        return prefsList;
    }

    @Override
    public void onSelected(String key) {
        boolean isEnabled = Utilities.isQSBEnabled(getActivity());
        boolean enable = key != KEY_DISABLED;
        boolean needsRestart = false;
        if (enable != isEnabled) {
            LauncherPrefs.getPrefs(getActivity()).edit()
                    .putBoolean(Utilities.KEY_DOCK_SEARCH, enable)
                    .apply();
            // Restart launcher if we toggle QSB visibility.
            LauncherAppState.getInstance(getContext()).setNeedsRestart();
        }
        if (enable) {
            String packageName = getPackageNameFromComponent(key);
            Settings.Secure.putString(getActivity().getContentResolver(),
                    QsbContainerView.SEARCH_ENGINE_SETTINGS_KEY, packageName);
            LauncherPrefs.getPrefs(getActivity()).edit()
                    .putString(QsbContainerView.SEARCH_COMPONENT_PREF_KEY, key)
                    .apply();
        }
        super.onSelected(key);
    }

    @Override
    protected QsbHeaderPreference getHeader(Context context) {
        return new QsbHeaderPreference(context);
    }

    private ComponentName getCurrentQsbProvider() {
        if (!Utilities.showQSB(getActivity())) {
            return null;
        }
        return QsbContainerView.getSearchComponentName(getActivity());
    }

    private Set<AppWidgetProviderInfo> getAvailableQsbProviders(Context context) {
        final Set<AppWidgetProviderInfo> qsbProviders = new LinkedHashSet<>();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (AppWidgetProviderInfo info : appWidgetManager.getInstalledProvidersForProfile(null)) {
            if (QsbContainerView.isQsbWidget(context, info)) {
                Log.d(TAG, "adding: " + info.provider);
                qsbProviders.add(info);
            }
        }
        return qsbProviders;
    }

    private SelectorWithWidgetPreference buildPreference(Context context, String key,
            String label, String pkgLabel, boolean isChecked) {
        final SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(context);
        pref.setKey(key);
        pref.setTitle(label);
        pref.setPersistent(false);
        pref.setChecked(isChecked);

        // Show app name as the summary if widget name doesn't have it.
        if (pkgLabel != null && !label.startsWith(pkgLabel)) {
            pref.setSummary(pkgLabel);
        }

        // Add known settings activity for google widget.
        if (key != KEY_DISABLED && getPackageNameFromComponent(key).equals(Utilities.GSA_PACKAGE)) {
            Intent intent = new Intent().setComponent(GSA_QSB_SETTINGS_ACTIVITY);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                pref.setExtraWidgetOnClickListener((v) -> {
                    context.startActivity(intent);
                });
            }
        }

        return pref;
    }

    private static final String getPackageNameFromComponent(String c) {
        return ComponentName.unflattenFromString(c).getPackageName();
    }
}
