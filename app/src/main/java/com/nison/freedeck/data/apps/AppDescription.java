package com.nison.freedeck.data.apps;

import androidx.annotation.Nullable;

/** Small text helpers for the details shown about an app. */
public final class AppDescription {

    private AppDescription() {
    }

    /** Collapses runs of whitespace (including line breaks) to single spaces; null when nothing is left. */
    @Nullable
    public static String clean(@Nullable CharSequence raw) {
        if (raw == null) {
            return null;
        }
        String collapsed = raw.toString().replaceAll("\\s+", " ").trim();
        return collapsed.isEmpty() ? null : collapsed;
    }

    /** Joins the parts that have text, skipping null and blank ones. */
    public static String join(String separator, @Nullable String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(separator);
            }
            joined.append(part.trim());
        }
        return joined.toString();
    }
}
