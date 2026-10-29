package com.nison.freedeck.ui.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.NavigationSound;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sound played as focus moves between app cards: whichever one is chosen in the settings.
 * Every sound is loaded up front, so changing the choice takes effect at once. Call {@link #release} when done.
 */
public class FocusSound {

    private static final long MIN_GAP_MS = 90L;
    private static final float VOLUME = 0.8f;

    private final PlayThrottle throttle = new PlayThrottle(MIN_GAP_MS);
    private final AppSettings settings;
    private final SoundPool pool;
    private final Map<NavigationSound, Integer> samples = new EnumMap<>(NavigationSound.class);
    /** Filled from SoundPool's own thread as each sound finishes loading. */
    private final Set<Integer> loaded = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private int activeStream;

    public FocusSound(Context context, AppSettings settings) {
        this.settings = settings;
        pool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                loaded.add(sampleId);
            }
        });
        for (NavigationSound sound : NavigationSound.values()) {
            if (!sound.isSilent()) {
                samples.put(sound, pool.load(context, sound.audio, 1));
            }
        }
    }

    /** Plays the chosen sound, unless it is "Off", still loading, or one played a moment ago. */
    public void play() {
        NavigationSound sound = settings.navigationSound();
        if (isPlayable(sound) && throttle.tryPlay(SystemClock.uptimeMillis())) {
            start(sound);
        }
    }

    /** Plays a given sound right away, e.g. while the user is choosing one. */
    public void preview(NavigationSound sound) {
        if (isPlayable(sound)) {
            start(sound);
        }
    }

    public void release() {
        loaded.clear();
        pool.release();
    }

    private boolean isPlayable(NavigationSound sound) {
        Integer sample = samples.get(sound);
        return sample != null && loaded.contains(sample);
    }

    /** The previous sound is cut off first, so moving quickly doesn't stack up their tails. */
    private void start(NavigationSound sound) {
        pool.stop(activeStream);
        activeStream = pool.play(samples.get(sound), VOLUME, VOLUME, 1, 0, 1f);
    }
}
