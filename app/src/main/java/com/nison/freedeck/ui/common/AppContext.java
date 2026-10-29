package com.nison.freedeck.ui.common;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import com.nison.freedeck.data.settings.AppSettings;

import java.util.Locale;

/**
 * A copy of a context that looks resources up in the launcher's own colour mode and language rather than the TV's, so
 * every screen, popup and background job reads the same strings.
 */
public final class AppContext {

    private AppContext() {
    }

    public static Context wrap(Context base) {
        AppSettings settings = AppSettings.from(base);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | settings.themeMode().uiNightMode;
        Locale locale = settings.language().locale();
        Locale.setDefault(locale);
        configuration.setLocales(new LocaleList(locale));
        configuration.setLayoutDirection(locale);
        return base.createConfigurationContext(configuration);
    }
}
