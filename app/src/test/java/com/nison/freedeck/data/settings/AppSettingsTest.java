package com.nison.freedeck.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AppSettingsTest {

    private static final class MemoryStorage implements AppSettings.Storage {
        final Map<String, String> values = new HashMap<>();

        @Override
        public String read(String key) {
            return values.get(key);
        }

        @Override
        public void write(String key, String value) {
            values.put(key, value);
        }
    }

    @Test
    public void spectrumIsTheDefaultSound() {
        assertEquals(NavigationSound.SOUND_1, NavigationSound.DEFAULT);
        assertEquals(NavigationSound.SOUND_1, new AppSettings(new MemoryStorage()).navigationSound());
    }

    @Test
    public void aChosenSoundIsRemembered() {
        MemoryStorage storage = new MemoryStorage();
        new AppSettings(storage).setNavigationSound(NavigationSound.SOUND_7);
        assertEquals(NavigationSound.SOUND_7, new AppSettings(storage).navigationSound());
    }

    @Test
    public void everySoundSurvivesBeingSavedAndReloaded() {
        for (NavigationSound sound : NavigationSound.values()) {
            MemoryStorage storage = new MemoryStorage();
            new AppSettings(storage).setNavigationSound(sound);
            assertEquals(sound, new AppSettings(storage).navigationSound());
        }
    }

    @Test
    public void offIsRememberedToo() {
        MemoryStorage storage = new MemoryStorage();
        new AppSettings(storage).setNavigationSound(NavigationSound.OFF);
        assertEquals(NavigationSound.OFF, new AppSettings(storage).navigationSound());
        assertTrue(NavigationSound.OFF.isSilent());
    }

    @Test
    public void anUnknownSavedValueFallsBackToTheDefault() {
        MemoryStorage storage = new MemoryStorage();
        storage.write("navigation_sound", "a_sound_removed_in_a_later_version");
        assertEquals(NavigationSound.DEFAULT, new AppSettings(storage).navigationSound());
    }

    @Test
    public void fromIdOfNullIsTheDefault() {
        assertEquals(NavigationSound.DEFAULT, NavigationSound.fromId(null));
    }

    @Test
    public void thereAreTwentyFourSoundsAndOff() {
        assertEquals(25, NavigationSound.values().length);
        int silent = 0;
        for (NavigationSound sound : NavigationSound.values()) {
            if (sound.isSilent()) {
                silent++;
            }
        }
        assertEquals(1, silent);
    }

    @Test
    public void everySoundHasItsOwnIdAndItsOwnAudio() {
        Set<String> ids = new HashSet<>();
        Set<Integer> audio = new HashSet<>();
        for (NavigationSound sound : NavigationSound.values()) {
            assertTrue("duplicate id " + sound.id, ids.add(sound.id));
            if (!sound.isSilent()) {
                assertTrue("two sounds share one audio file: " + sound.id, audio.add(sound.audio));
            }
        }
    }

    @Test
    public void spectrumPlaysTheSoundTheUserAskedFor() {
        assertNotEquals(0, NavigationSound.SOUND_1.audio);
        assertEquals("sound_1", NavigationSound.SOUND_1.id);
    }

    @Test
    public void appLayoutDefaultsToFiveByThree() {
        assertEquals(AppLayout.COLUMNS_5_ROWS_3, new AppSettings(new MemoryStorage()).appLayout());
        assertEquals(5, AppLayout.DEFAULT.columns);
        assertEquals(3, AppLayout.DEFAULT.rows);
    }

    @Test
    public void appLayoutIsRememberedAndAnnounced() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        int[] calls = {0};
        settings.addAppLayoutListener(() -> calls[0]++);
        settings.setAppLayout(AppLayout.COLUMNS_3_ROWS_3);
        assertEquals(AppLayout.COLUMNS_3_ROWS_3, new AppSettings(storage).appLayout());
        assertEquals(1, calls[0]);
    }

    @Test
    public void unknownAppLayoutIsTheDefault() {
        assertEquals(AppLayout.DEFAULT, AppLayout.fromId("9x9"));
        assertEquals(AppLayout.DEFAULT, AppLayout.fromId(null));
    }

    @Test
    public void themeDefaultsToDark() {
        assertEquals(ThemeMode.DARK, new AppSettings(new MemoryStorage()).themeMode());
        assertEquals(ThemeMode.DARK, ThemeMode.DEFAULT);
    }

    @Test
    public void themeIsRemembered() {
        MemoryStorage storage = new MemoryStorage();
        new AppSettings(storage).setThemeMode(ThemeMode.LIGHT);
        assertEquals(ThemeMode.LIGHT, new AppSettings(storage).themeMode());
    }

    @Test
    public void unknownThemeIsTheDefault() {
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromId("sepia"));
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromId(null));
    }

    @Test
    public void eachThemeSelectsADifferentNightMode() {
        assertNotEquals(ThemeMode.DARK.uiNightMode, ThemeMode.LIGHT.uiNightMode);
    }

    @Test
    public void backgroundDefaultsToMountains() {
        assertEquals(AppBackground.SCENE_MOUNTAINS, new AppSettings(new MemoryStorage()).background());
        assertEquals(AppBackground.SCENE_MOUNTAINS, AppBackground.DEFAULT);
    }

    @Test
    public void screensaverBackgroundDefaultsToMountains() {
        assertEquals(AppBackground.SCENE_MOUNTAINS, new AppSettings(new MemoryStorage()).screensaverBackground());
    }

    @Test
    public void noneStaysChosenWhenPicked() {
        MemoryStorage storage = new MemoryStorage();
        new AppSettings(storage).setBackground(AppBackground.NONE);
        assertEquals(AppBackground.NONE, new AppSettings(storage).background());
    }

    @Test
    public void backgroundIsRememberedAndAnnounced() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        int[] calls = {0};
        settings.addBackgroundListener(() -> calls[0]++);
        settings.setBackground(AppBackground.OCEAN);
        assertEquals(AppBackground.OCEAN, new AppSettings(storage).background());
        assertEquals(1, calls[0]);
    }

    @Test
    public void unknownBackgroundIsTheDefault() {
        assertEquals(AppBackground.DEFAULT, AppBackground.fromId("plaid"));
        assertEquals(AppBackground.DEFAULT, AppBackground.fromId(null));
    }

    @Test
    public void everyBackgroundIsNoneOrASceneOrAGradient() {
        for (AppBackground background : AppBackground.values()) {
            if (background.isNone()) {
                assertEquals(AppBackground.NONE, background);
            } else if (background.isCustom()) {
                assertEquals(0, background.colors.length);
            } else if (background.isScene()) {
                assertNotEquals(0, background.thumbnail);
                assertEquals(0, background.colors.length);
            } else {
                assertEquals(2, background.colors.length);
            }
        }
    }

    @Test
    public void backgroundIdsAreUnique() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (AppBackground background : AppBackground.values()) {
            assertTrue(background.id, ids.add(background.id));
        }
    }

    @Test
    public void clockDefaultsToTwelveHour() {
        assertEquals(ClockFormat.HOURS_12, new AppSettings(new MemoryStorage()).clockFormat());
        assertEquals(ClockFormat.DEFAULT, ClockFormat.fromId("martian"));
    }

    @Test
    public void clockFormatIsRememberedAndAnnounced() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        int[] calls = {0};
        settings.addNavbarListener(() -> calls[0]++);
        settings.setClockFormat(ClockFormat.HOURS_24);
        assertEquals(ClockFormat.HOURS_24, new AppSettings(storage).clockFormat());
        assertEquals(1, calls[0]);
    }

    @Test
    public void byDefaultOnlyGreetingTimeAndDateAreShown() {
        AppSettings settings = new AppSettings(new MemoryStorage());
        assertTrue(settings.isNavbarItemShown(NavbarItem.GREETING));
        assertTrue(settings.isNavbarItemShown(NavbarItem.TIME));
        assertTrue(settings.isNavbarItemShown(NavbarItem.DATE));
        assertFalse(settings.isNavbarItemShown(NavbarItem.LOCATION));
        assertFalse(settings.isNavbarItemShown(NavbarItem.WEATHER));
    }

    @Test
    public void defaultOrderPutsGreetingAndTimeOnTheFirstLineAndDateUnderThem() {
        java.util.List<NavbarItem> order = new AppSettings(new MemoryStorage()).navbarTextOrder();
        assertEquals(java.util.Arrays.asList(NavbarItem.GREETING, NavbarItem.TIME, NavbarItem.DATE,
                NavbarItem.LOCATION, NavbarItem.WEATHER), order);
        assertEquals(2, NavbarItem.FIRST_LINE_COUNT);
    }

    @Test
    public void switchingOnAnItemThatIsOffByDefaultIsRemembered() {
        MemoryStorage storage = new MemoryStorage();
        new AppSettings(storage).setNavbarItemShown(NavbarItem.LOCATION, true);
        assertTrue(new AppSettings(storage).isNavbarItemShown(NavbarItem.LOCATION));
        new AppSettings(storage).setNavbarItemShown(NavbarItem.LOCATION, false);
        assertFalse(new AppSettings(storage).isNavbarItemShown(NavbarItem.LOCATION));
    }

    @Test
    public void hidingANavbarItemIsRememberedAndOnlyAffectsThatItem() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        int[] calls = {0};
        settings.addNavbarListener(() -> calls[0]++);
        settings.setNavbarItemShown(NavbarItem.DATE, false);
        AppSettings again = new AppSettings(storage);
        assertFalse(again.isNavbarItemShown(NavbarItem.DATE));
        assertTrue(again.isNavbarItemShown(NavbarItem.TIME));
        assertEquals(1, calls[0]);
        settings.setNavbarItemShown(NavbarItem.DATE, true);
        assertTrue(new AppSettings(storage).isNavbarItemShown(NavbarItem.DATE));
    }

    @Test
    public void eachModeKeepsItsOwnFontColour() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        assertEquals(null, settings.fontColor(ThemeMode.DARK));
        settings.setFontColor(ThemeMode.DARK, 0xFF6DE3F2);
        settings.setFontColor(ThemeMode.LIGHT, 0xFF14284B);
        assertEquals(Integer.valueOf(0xFF6DE3F2), new AppSettings(storage).fontColor(ThemeMode.DARK));
        assertEquals(Integer.valueOf(0xFF14284B), new AppSettings(storage).fontColor(ThemeMode.LIGHT));
        settings.setFontColor(ThemeMode.DARK, null);
        assertEquals(null, new AppSettings(storage).fontColor(ThemeMode.DARK));
    }

    @Test
    public void denyingTheDefaultLauncherPromptIsRemembered() {
        MemoryStorage storage = new MemoryStorage();
        assertFalse(new AppSettings(storage).isDefaultLauncherPromptDenied());
        new AppSettings(storage).setDefaultLauncherPromptDenied(true);
        assertTrue(new AppSettings(storage).isDefaultLauncherPromptDenied());
        // Turning the launcher setting on and off again does not bring the prompt back.
        new AppSettings(storage).setDefaultLauncher(true);
        new AppSettings(storage).setDefaultLauncher(false);
        assertTrue(new AppSettings(storage).isDefaultLauncherPromptDenied());
    }

    @Test
    public void englishIsTheDefaultLanguageAndAChoiceIsRemembered() {
        MemoryStorage storage = new MemoryStorage();
        assertEquals(AppLanguage.ENGLISH, new AppSettings(storage).language());
        new AppSettings(storage).setLanguage(AppLanguage.TAMIL);
        assertEquals(AppLanguage.TAMIL, new AppSettings(storage).language());
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("klingon"));
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null));
    }

    @Test
    public void thereAreFiveCustomBackgroundsNamedByNumber() {
        int customs = 0;
        for (AppBackground background : AppBackground.values()) {
            if (background.isCustom()) {
                customs++;
                assertEquals(background, AppBackground.forSlot(background.slot()));
            }
        }
        assertEquals(CustomBackground.SLOTS, customs);
        assertEquals(AppBackground.CUSTOM_1, AppBackground.fromId("custom_1"));
        assertEquals(AppBackground.CUSTOM_5, AppBackground.fromId("custom_5"));
    }

    @Test
    public void theSingleUploadFromBeforeBecomesCustomOne() {
        assertEquals(AppBackground.CUSTOM_1, AppBackground.fromId("custom"));
    }

    @Test
    public void screensaverRandomHasTwoModesAndOldRandomMeansSystem() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        assertEquals(BackgroundRandom.OFF, settings.screensaverRandom());
        settings.setScreensaverRandom(BackgroundRandom.CUSTOM);
        assertEquals(BackgroundRandom.CUSTOM, new AppSettings(storage).screensaverRandom());
        settings.setScreensaverRandom(BackgroundRandom.SYSTEM);
        assertEquals(BackgroundRandom.SYSTEM, new AppSettings(storage).screensaverRandom());
        storage.write("screensaver_background", "random");
        assertEquals(BackgroundRandom.SYSTEM, new AppSettings(storage).screensaverRandom());
    }

    @Test
    public void choosingAFixedBackgroundTurnsRandomOff() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        settings.setScreensaverRandom(BackgroundRandom.SYSTEM);
        settings.setScreensaverBackground(AppBackground.OCEAN);
        assertEquals(BackgroundRandom.OFF, settings.screensaverRandom());
        assertEquals(AppBackground.OCEAN, settings.screensaverBackground());
    }

    @Test
    public void deletingAPictureInUseGoesBackToTheDefault() {
        MemoryStorage storage = new MemoryStorage();
        AppSettings settings = new AppSettings(storage);
        settings.setBackground(AppBackground.CUSTOM_2);
        settings.setScreensaverBackground(AppBackground.CUSTOM_2);
        settings.customBackgroundDeleted(AppBackground.CUSTOM_2, true);
        assertEquals(AppBackground.DEFAULT, settings.background());
        assertEquals(AppBackground.DEFAULT, settings.screensaverBackground());
    }

    @Test
    public void deletingADifferentPictureChangesNothing() {
        AppSettings settings = new AppSettings(new MemoryStorage());
        settings.setBackground(AppBackground.CUSTOM_1);
        settings.setScreensaverBackground(AppBackground.OCEAN);
        settings.customBackgroundDeleted(AppBackground.CUSTOM_3, true);
        assertEquals(AppBackground.CUSTOM_1, settings.background());
        assertEquals(AppBackground.OCEAN, settings.screensaverBackground());
    }

    @Test
    public void randomCustomWithNoPicturesLeftIsTurnedOff() {
        AppSettings settings = new AppSettings(new MemoryStorage());
        settings.setBackgroundRandom(BackgroundRandom.CUSTOM);
        settings.setScreensaverRandom(BackgroundRandom.CUSTOM);
        settings.customBackgroundDeleted(AppBackground.CUSTOM_1, false);
        assertEquals(BackgroundRandom.OFF, settings.backgroundRandom());
        assertEquals(BackgroundRandom.OFF, settings.screensaverRandom());
    }

    @Test
    public void randomCustomKeepsGoingWhilePicturesRemain() {
        AppSettings settings = new AppSettings(new MemoryStorage());
        settings.setScreensaverRandom(BackgroundRandom.CUSTOM);
        settings.customBackgroundDeleted(AppBackground.CUSTOM_1, true);
        assertEquals(BackgroundRandom.CUSTOM, settings.screensaverRandom());
    }

    @Test
    public void deletingAnnouncesTheChangeToBothLists() {
        AppSettings settings = new AppSettings(new MemoryStorage());
        int[] calls = {0, 0, 0};
        settings.addCustomBackgroundListener(() -> calls[0]++);
        settings.addBackgroundListener(() -> calls[1]++);
        settings.addScreensaverListener(() -> calls[2]++);
        settings.customBackgroundDeleted(AppBackground.CUSTOM_4, true);
        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
        assertEquals(1, calls[2]);
    }
}
