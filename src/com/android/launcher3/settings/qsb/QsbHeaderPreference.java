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

import static com.android.launcher3.Utilities.showQSB;
import static com.android.launcher3.qsb.QsbContainerView.getSearchWidgetProviderInfo;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.R;
import com.android.launcher3.settings.widget.RadioHeaderPreference;

public class QsbHeaderPreference extends RadioHeaderPreference {
    private static final String TAG = "QsbHeaderPreference";

    private ImageView mQsbPreview;

    public QsbHeaderPreference(Context context) {
        this(context, null);
    }

    public QsbHeaderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public QsbHeaderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setLayoutResource(R.layout.preference_widget_qsb_preview);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        mQsbPreview = (ImageView) holder.findViewById(R.id.qsb_preview_image);
        onRadioElementSelected(null);
    }

    @Override
    public void onRadioElementSelected(String key) {
        if (mQsbPreview == null) {
            return;
        }

        final AppWidgetProviderInfo provider = getSearchWidgetProviderInfo(getContext());
        final Drawable preview = provider != null
                ? provider.loadPreviewImage(getContext(), 0) : null;
        if (provider == null || preview == null || !showQSB(getContext())) {
            setVisible(false);
        } else {
            setVisible(true);
            mQsbPreview.setImageDrawable(preview);
        }
    }
}
