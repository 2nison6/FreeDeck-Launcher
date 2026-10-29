"""Splits a UI-sound compilation (many short sounds separated by silence) into one WAV file per sound.

Usage: python3 -I tools/extract_sounds.py <compilation.mp3> <output-dir>

Each sound is found by looking for the quiet gaps between sounds, cut with a few milliseconds of margin, faded at
both ends so it never clicks, converted to mono, and scaled so its loudest point is the same for every sound
(otherwise the louder ones would drown out the quieter ones when you switch between them).
Files are named sfx_01.wav, sfx_02.wav, ... in the order they appear in the compilation.
Needs ffmpeg on the PATH.
"""
import os
import re
import subprocess
import sys

GAP_THRESHOLD_DB = -45     # quieter than this counts as silence
MIN_GAP_S = 0.06           # ...if it lasts at least this long
MIN_SOUND_S = 0.02         # shorter blips are noise, not sounds
LEAD_IN_S = 0.015          # a little silence kept before the sound, so its start isn't clipped
TAIL_S = 0.03              # and after, so its decay isn't clipped
FADE_IN_S = 0.004
FADE_OUT_S = 0.02
SAMPLE_RATE = 32000
TARGET_PEAK_DB = -3.0


def run(args):
    return subprocess.run(args, capture_output=True, text=True, check=True)


def duration(path):
    out = run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=nw=1:nk=1", path])
    return float(out.stdout.strip())


def find_sounds(path, total):
    log = subprocess.run(
        ["ffmpeg", "-hide_banner", "-nostats", "-i", path,
         "-af", f"silencedetect=noise={GAP_THRESHOLD_DB}dB:d={MIN_GAP_S}", "-f", "null", "-"],
        capture_output=True, text=True).stderr
    events = sorted(
        [(float(t), "start") for t in re.findall(r"silence_start: ([\d.]+)", log)]
        + [(float(t), "end") for t in re.findall(r"silence_end: ([\d.]+)", log)])
    sounds, sound_start = [], None
    for time, kind in events:
        if kind == "end":
            sound_start = time
        else:
            if sound_start is not None and time - sound_start > MIN_SOUND_S:
                sounds.append((sound_start, time))
            sound_start = None
    if sound_start is not None and total - sound_start > MIN_SOUND_S:
        sounds.append((sound_start, total))
    return sounds


def peak_db(path):
    log = subprocess.run(
        ["ffmpeg", "-hide_banner", "-nostats", "-i", path, "-af", "volumedetect", "-f", "null", "-"],
        capture_output=True, text=True).stderr
    return float(re.search(r"max_volume: (-?[\d.]+) dB", log).group(1))


def main(source, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    total = duration(source)
    sounds = find_sounds(source, total)
    for index, (begin, end) in enumerate(sounds, 1):
        start = max(0.0, begin - LEAD_IN_S)
        length = min(total, end + TAIL_S) - start
        target = os.path.join(out_dir, f"sfx_{index:02d}.wav")
        draft = target + ".draft.wav"
        # Cut, mix down to mono and fade first; the loudness is measured on that result, because mixing down
        # and fading both lower the peak, and only the final sound matters.
        run(["ffmpeg", "-y", "-v", "error", "-ss", f"{start:.4f}", "-t", f"{length:.4f}", "-i", source,
             "-af", (f"pan=mono|c0=0.5*c0+0.5*c1,"
                     f"afade=t=in:d={FADE_IN_S},afade=t=out:st={length - FADE_OUT_S:.4f}:d={FADE_OUT_S}"),
             "-ar", str(SAMPLE_RATE), "-c:a", "pcm_s16le", draft])
        gain = TARGET_PEAK_DB - peak_db(draft)
        run(["ffmpeg", "-y", "-v", "error", "-i", draft, "-af", f"volume={gain:.2f}dB",
             "-c:a", "pcm_s16le", target])
        os.remove(draft)
        print(f"sfx_{index:02d}.wav  {start:7.3f}s  {length:5.2f}s long  gain {gain:+5.1f} dB")
    print(f"{len(sounds)} sounds -> {out_dir}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
