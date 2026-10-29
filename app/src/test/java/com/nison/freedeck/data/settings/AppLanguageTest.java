package com.nison.freedeck.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class AppLanguageTest {

    @Test
    public void thirteenLanguagesWithEnglishFirstAndAsTheDefault() {
        assertEquals(13, AppLanguage.values().length);
        assertEquals(AppLanguage.ENGLISH, AppLanguage.values()[0]);
        assertEquals(AppLanguage.ENGLISH, AppLanguage.DEFAULT);
    }

    @Test
    public void codesAndNamesAreUniqueAndFilledIn() {
        Set<String> codes = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (AppLanguage language : AppLanguage.values()) {
            assertTrue(language.code, codes.add(language.code));
            assertTrue(language.nativeName, names.add(language.nativeName));
            assertFalse(language.nativeName.isEmpty());
            assertEquals(language, AppLanguage.fromCode(language.code));
        }
    }

    @Test
    public void onlyUrduIsWrittenRightToLeft() {
        for (AppLanguage language : AppLanguage.values()) {
            assertEquals(language.code, language == AppLanguage.URDU, language.isRightToLeft());
        }
    }
}
