package com.nison.freedeck.data.settings;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** The sounds that can play as focus moves between app cards, plus "Off". The first one is the default. */
public enum NavigationSound {
    SOUND_1("sound_1", R.string.sound_1, R.raw.sfx_16),
    SOUND_2("sound_2", R.string.sound_2, R.raw.sfx_01),
    SOUND_3("sound_3", R.string.sound_3, R.raw.sfx_02),
    SOUND_4("sound_4", R.string.sound_4, R.raw.sfx_03),
    SOUND_5("sound_5", R.string.sound_5, R.raw.sfx_04),
    SOUND_6("sound_6", R.string.sound_6, R.raw.sfx_05),
    SOUND_7("sound_7", R.string.sound_7, R.raw.sfx_06),
    SOUND_8("sound_8", R.string.sound_8, R.raw.sfx_07),
    SOUND_9("sound_9", R.string.sound_9, R.raw.sfx_08),
    SOUND_10("sound_10", R.string.sound_10, R.raw.sfx_09),
    SOUND_11("sound_11", R.string.sound_11, R.raw.sfx_10),
    SOUND_12("sound_12", R.string.sound_12, R.raw.sfx_11),
    SOUND_13("sound_13", R.string.sound_13, R.raw.sfx_12),
    SOUND_14("sound_14", R.string.sound_14, R.raw.sfx_13),
    SOUND_15("sound_15", R.string.sound_15, R.raw.sfx_14),
    SOUND_16("sound_16", R.string.sound_16, R.raw.sfx_15),
    SOUND_17("sound_17", R.string.sound_17, R.raw.sfx_17),
    SOUND_18("sound_18", R.string.sound_18, R.raw.sfx_18),
    SOUND_19("sound_19", R.string.sound_19, R.raw.sfx_19),
    SOUND_20("sound_20", R.string.sound_20, R.raw.sfx_20),
    SOUND_21("sound_21", R.string.sound_21, R.raw.sfx_21),
    SOUND_22("sound_22", R.string.sound_22, R.raw.sfx_22),
    SOUND_23("sound_23", R.string.sound_23, R.raw.sfx_23),
    SOUND_24("sound_24", R.string.sound_24, R.raw.sfx_24),
    /** No audio file (0 is not a valid resource id), so nothing plays. */
    OFF("off", R.string.sound_off, 0);

    public static final NavigationSound DEFAULT = SOUND_1;

    /** Stable key for saving the choice; never changes even if the display name does. */
    public final String id;
    @StringRes
    public final int label;
    @RawRes
    public final int audio;

    NavigationSound(String id, @StringRes int label, @RawRes int audio) {
        this.id = id;
        this.label = label;
        this.audio = audio;
    }

    public boolean isSilent() {
        return audio == 0;
    }

    /** The sound saved under {@code id}; the default when there is none or it is not recognised. */
    public static NavigationSound fromId(String id) {
        for (NavigationSound sound : values()) {
            if (sound.id.equals(id)) {
                return sound;
            }
        }
        return DEFAULT;
    }
}
