package com.android.launcher3.icons;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.icons.pack.IconResolver;

import static com.android.launcher3.icons.BaseIconFactory.CONFIG_HINT_NO_WRAP;

@SuppressWarnings("unused")
public class ThirdPartyIconProvider extends LauncherIconProvider {
    private final Context mContext;

    public ThirdPartyIconProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int iconDpi, String themedIconPack) {
        ComponentKey key = new ComponentKey(
                launcherActivityInfo.getComponentName(), launcherActivityInfo.getUser());

        IconResolver.DefaultDrawableProvider fallback =
                () -> super.getIcon(launcherActivityInfo, iconDpi, themedIconPack);
        Drawable icon = ThirdPartyIconUtils.getByKey(mContext, key, iconDpi, fallback);

        if (icon == null) {
            return fallback.get();
        }
        icon.setChangingConfigurations(icon.getChangingConfigurations() | CONFIG_HINT_NO_WRAP);
        return icon;
    }

    @Override
    public Drawable getIcon(ActivityInfo activityInfo, int iconDpi, String themedIconPack) {
        ComponentKey key = new ComponentKey(activityInfo.getComponentName(), UserHandle.CURRENT);

        IconResolver.DefaultDrawableProvider fallback =
                () -> super.getIcon(activityInfo, iconDpi, themedIconPack);
        Drawable icon = ThirdPartyIconUtils.getByKey(mContext, key, iconDpi, fallback);

        if (icon == null) {
            return fallback.get();
        }
        icon.setChangingConfigurations(icon.getChangingConfigurations() | CONFIG_HINT_NO_WRAP);
        return icon;
    }
}
