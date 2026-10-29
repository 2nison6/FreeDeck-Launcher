package com.nison.freedeck.data.settings;

import java.util.Locale;

/**
 * The languages the launcher can be shown in. Each is listed by its own name, in its own script, so a person can find
 * theirs without being able to read the current language. English is the default.
 */
public enum AppLanguage {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    BENGALI("bn", "বাংলা"),
    MARATHI("mr", "मराठी"),
    TELUGU("te", "తెలుగు"),
    TAMIL("ta", "தமிழ்"),
    GUJARATI("gu", "ગુજરાતી"),
    URDU("ur", "اردو"),
    KANNADA("kn", "ಕನ್ನಡ"),
    ODIA("or", "ଓଡ଼ିଆ"),
    MALAYALAM("ml", "മലയാളം"),
    PUNJABI("pa", "ਪੰਜਾਬੀ"),
    ASSAMESE("as", "অসমীয়া");

    public static final AppLanguage DEFAULT = ENGLISH;

    /** The language code, which is also what is saved. */
    public final String code;
    /** The language's own name, in its own script. */
    public final String nativeName;

    AppLanguage(String code, String nativeName) {
        this.code = code;
        this.nativeName = nativeName;
    }

    public Locale locale() {
        return new Locale(code);
    }

    /** Whether the language is written right to left. */
    public boolean isRightToLeft() {
        return this == URDU;
    }

    /** The language saved under {@code code}; English when there is none or it is not recognised. */
    public static AppLanguage fromCode(String code) {
        for (AppLanguage language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        return DEFAULT;
    }
}
