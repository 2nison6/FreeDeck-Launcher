package com.nison.freedeck.data.apps;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** The Silk browser gets a friendlier name and its real icon on a 16:9 card, because it ships without a TV banner. */
public final class AppOverrides {

    public static final class Override {
        @StringRes
        public final int label;
        @DrawableRes
        public final int banner;

        Override(@StringRes int label, @DrawableRes int banner) {
            this.label = label;
            this.banner = banner;
        }
    }

    private AppOverrides() {
    }

    /** The Silk browser, shown as "Internet" with its own icon centred on white. */
    @Nullable
    public static Override forPackage(String packageName) {
        switch (packageName) {
            case "com.amazon.cloud9":
                return new Override(R.string.app_name_internet, R.drawable.app_banner_internet);
            default:
                return null;
        }
    }
}
