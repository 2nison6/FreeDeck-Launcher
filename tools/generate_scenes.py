#!/usr/bin/env python3
"""Paints the launcher's scene backgrounds: layered, illustrated landscapes, one JPEG (plus a small thumbnail) each.

Everything is drawn from shapes and gradients in plain Python, so the pictures are original and need no downloads.
Each scene is painted at 1280x720; ffmpeg scales it to 1920x1080 and adds a little noise so smooth skies do not band.

Usage: python3 tools/generate_scenes.py     (needs ffmpeg on the PATH; writes into app/src/main/res/drawable-nodpi)
"""
import math
import os
import random
import subprocess
import tempfile

W, H = 1280, 720
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5))


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def byte(v):
    return max(0, min(255, int(v)))


class Canvas:
    def __init__(self):
        self.px = bytearray(W * H * 3)

    # ---- skies and light
    @staticmethod
    def _ramp(stops, t):
        if t <= stops[0][0]:
            return stops[0][1]
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            if t <= t1:
                return lerp(c0, c1, (t - t0) / (t1 - t0))
        return stops[-1][1]

    def sky(self, stops, y0=0, y1=H):
        stops = [(t, hexc(c)) for t, c in stops]
        for y in range(y0, y1):
            col = self._ramp(stops, (y - y0) / max(1, y1 - y0 - 1))
            self.px[y * W * 3:(y + 1) * W * 3] = bytes(byte(v) for v in col) * W

    def glow(self, cx, cy, radius, color, strength):
        col = hexc(color)
        sigma2 = 2 * (radius / 2.0) ** 2
        px = self.px
        reach = radius * 2  # the falloff is below 0.1% by here, so the edge of the box is never visible
        for y in range(max(0, int(cy - reach)), min(H, int(cy + reach))):
            dy2 = (y - cy) ** 2
            base = y * W * 3
            for x in range(max(0, int(cx - reach)), min(W, int(cx + reach))):
                w = strength * math.exp(-((x - cx) ** 2 + dy2) / sigma2)
                if w < 0.004:
                    continue
                i = base + x * 3
                px[i] = int(px[i] + (col[0] - px[i]) * w)
                px[i + 1] = int(px[i + 1] + (col[1] - px[i + 1]) * w)
                px[i + 2] = int(px[i + 2] + (col[2] - px[i + 2]) * w)

    def haze(self, y0, y1, color, strength):
        col = hexc(color)
        row_col = bytes(byte(v) for v in col) * W
        for y in range(max(0, y0), min(H, y1)):
            w = strength * math.sin(math.pi * (y - y0) / max(1, y1 - y0))
            a, b = y * W * 3, (y + 1) * W * 3
            self.px[a:b] = bytes(int(v + (c - v) * w) for v, c in zip(self.px[a:b], row_col))

    def stars(self, count, y_max, seed):
        rnd = random.Random(seed)
        for _ in range(count):
            x, y = rnd.randrange(W), rnd.randrange(y_max)
            level = rnd.choice([90, 120, 160, 200, 255])
            tint = rnd.choice([(1, 1, 1), (1, .95, .85), (.85, .92, 1)])
            size = 2 if level > 190 and rnd.random() < .5 else 1
            for dy in range(size):
                for dx in range(size):
                    i = ((y + dy) * W + (x + dx)) * 3
                    if i + 2 < len(self.px):
                        for c in range(3):
                            self.px[i + c] = max(self.px[i + c], int(level * tint[c]))

    # ---- shapes
    def rect(self, x0, y0, x1, y1, color):
        x0, x1, y0, y1 = max(0, x0), min(W, x1), max(0, y0), min(H, y1)
        if x1 <= x0:
            return
        row = bytes(byte(v) for v in hexc(color)) * (x1 - x0)
        for y in range(y0, y1):
            self.px[(y * W + x0) * 3:(y * W + x1) * 3] = row

    def disc(self, cx, cy, r, color, clip_y=H):
        for y in range(max(0, cy - r), min(clip_y, cy + r + 1)):
            half = int(math.sqrt(max(0, r * r - (y - cy) ** 2)))
            self.rect(cx - half, y, cx + half + 1, y + 1, color)

    def ridge(self, heights, top, bottom, depth=0.55):
        """Fills everything under a skyline, shaded from `top` at the edge to `bottom` lower down."""
        top, bottom = hexc(top), hexc(bottom)
        channels = [bytes(byte(top[c] + (bottom[c] - top[c]) * min(1.0, k / (H * depth))) for k in range(H))
                    for c in range(3)]
        stride = W * 3
        for x in range(W):
            h = max(0, min(H - 1, heights[x]))
            for c in range(3):
                self.px[(h * W + x) * 3 + c::stride] = channels[c][:H - h]

    def pine(self, cx, base, height, width, color):
        top = base - height
        tiers = 5
        for y in range(top, base):
            t = (y - top) / height
            tier = int(t * tiers)
            half = int(((tier + 1 + (t * tiers - tier)) / (tiers + 1)) * width / 2)
            self.rect(cx - half, y, cx + half + 1, y + 1, color)
        self.rect(cx - 2, base, cx + 3, base + 6, color)

    def save(self, path):
        with open(path, "wb") as f:
            f.write(b"P6\n%d %d\n255\n" % (W, H))
            f.write(self.px)


def ridged(base, amp, freqs, seed, sharp=True):
    """A skyline: the height of an edge at each column, from a few waves added together."""
    rnd = random.Random(seed)
    comps = [(f, rnd.uniform(0, 2 * math.pi), 1.0 / (i + 1)) for i, f in enumerate(freqs)]
    norm = sum(a for _, _, a in comps)
    heights = []
    for x in range(W):
        n = 0.0
        for f, phase, a in comps:
            s = math.sin(2 * math.pi * f * x / W + phase)
            n += a * ((1 - abs(s)) if sharp else (s + 1) / 2)
        heights.append(int(base - amp * n / norm))
    return heights


# ------------------------------------------------------------------ scenes

def mountains():
    c = Canvas()
    c.sky([(0, '#1b1245'), (.45, '#6a2f78'), (.75, '#e0607a'), (1, '#ffb36b')])
    c.glow(900, 440, 300, '#ffd29a', .85)
    c.disc(900, 430, 38, '#fff0cf', 480)
    c.ridge(ridged(450, 170, (2, 5, 11), 1), '#9a6a98', '#6b4684')
    c.haze(380, 560, '#e9a0a0', .30)
    c.ridge(ridged(520, 160, (3, 7, 13), 2), '#6a4a86', '#43336a')
    c.haze(460, 620, '#a070a8', .28)
    c.ridge(ridged(590, 130, (2, 6, 12), 3), '#3a2d62', '#241e48')
    c.ridge(ridged(660, 90, (4, 9), 4), '#1b1738', '#100e26')
    return c


def night_sky():
    c = Canvas()
    c.sky([(0, '#04061a'), (.55, '#0d1b3e'), (1, '#27456e')])
    c.stars(620, 420, 11)
    c.glow(980, 170, 200, '#9db4ff', .45)
    c.disc(980, 170, 44, '#f6f2df')
    c.disc(968, 160, 6, '#e3dfc8')
    c.disc(992, 184, 9, '#e8e4cf')
    c.disc(990, 156, 5, '#e3dfc8')  # a few craters
    c.glow(300, 640, 330, '#3b6ea5', .35)
    c.ridge(ridged(590, 80, (1.3, 2.7, 5), 5, sharp=False), '#13264a', '#0a1430')
    c.ridge(ridged(660, 60, (1.8, 3.4), 6, sharp=False), '#0a1430', '#050a1c')
    return c


def city():
    c = Canvas()
    c.sky([(0, '#0b1030'), (.5, '#2b2a6b'), (.78, '#c0507a'), (1, '#ff9a6b')])
    c.stars(150, 280, 21)
    c.glow(640, 620, 420, '#ff8a65', .55)
    rnd = random.Random(7)
    x = -10
    while x < W:
        w, top = rnd.randint(40, 90), rnd.randint(300, 450)
        c.rect(x, top, x + w, H, '#3b3170')
        x += w + rnd.randint(0, 6)
    c.haze(360, 720, '#c0507a', .30)
    x = -10
    while x < W:
        w, top = rnd.randint(50, 110), rnd.randint(410, 600)
        c.rect(x, top, x + w, H, '#15122d')
        if rnd.random() < .3:
            c.rect(x + w // 2 - 1, top - 40, x + w // 2 + 2, top, '#15122d')  # an aerial
        for wy in range(top + 14, H - 10, 18):
            for wx in range(x + 8, x + w - 10, 14):
                if rnd.random() < .38:
                    c.rect(wx, wy, wx + 6, wy + 9, rnd.choice(['#ffd27a', '#ffe9a8', '#ffb86b']))
        x += w + rnd.randint(2, 10)
    return c


def sunset_sea():
    c = Canvas()
    horizon = 430
    c.sky([(0, '#1a1040'), (.5, '#8a3a7a'), (.8, '#ff6f61'), (1, '#ffc371')], 0, horizon)
    c.glow(640, horizon, 330, '#ffb07a', .9)
    c.disc(640, horizon - 2, 58, '#fff1c1', horizon)
    c.sky([(0, '#d6566a'), (.35, '#6d2f6a'), (1, '#0b1033')], horizon, H)
    rnd = random.Random(3)
    for y in range(horizon + 2, H, 2):
        d = (y - horizon) / (H - horizon)
        half = int(40 + d * 190)
        for _ in range(int(3 + d * 7)):
            x0 = 640 + rnd.randint(-half, half)
            length = rnd.randint(8, int(24 + d * 70))
            col = '#ffd9a0' if rnd.random() < .5 else '#ffb27a'
            row = bytes(byte(v * (1 - d * .55)) for v in hexc(col)) * length
            a = (y * W + max(0, x0)) * 3
            span = min(length, W - max(0, x0))
            if span > 0:
                cur = c.px[a:a + span * 3]
                c.px[a:a + span * 3] = bytes(max(p, q) for p, q in zip(cur, row[:span * 3]))
    return c


def pine_forest():
    c = Canvas()
    c.sky([(0, '#6aa7a0'), (.6, '#b8dccd'), (1, '#f2f0d0')])
    c.glow(900, 250, 320, '#fff6cf', .85)
    rnd = random.Random(9)
    layers = [(470, (90, 130), 46, '#8fbfb4', 0.0), (545, (130, 200), 60, '#558f84', .30),
              (640, (200, 300), 80, '#27574f', .30), (745, (300, 430), 110, '#0f302f', 0.0)]
    for base, (h0, h1), width, color, haze in layers:
        c.rect(0, base, W, H, color)
        x = -20
        while x < W + 20:
            c.pine(x, base, rnd.randint(h0, h1), width + rnd.randint(-10, 14), color)
            x += rnd.randint(int(width * .45), int(width * .85))
        if haze:
            c.haze(base - 140, base + 40, '#dcefe4', haze)
    return c


def desert():
    c = Canvas()
    c.sky([(0, '#2a1b4d'), (.5, '#d9658b'), (.8, '#ffb26b'), (1, '#ffd89b')])
    c.glow(400, 390, 360, '#ffe3b0', .9)
    c.disc(400, 380, 72, '#fff2d6', 520)
    c.ridge(ridged(470, 50, (.8, 1.7), 31, sharp=False), '#f0a06a', '#c46f4e')
    c.ridge(ridged(545, 70, (1.1, 2.3), 32, sharp=False), '#cf714f', '#94495a')
    c.ridge(ridged(625, 75, (.9, 2.1), 33, sharp=False), '#80405a', '#4e2a56')
    c.ridge(ridged(695, 55, (1.4, 2.9), 34, sharp=False), '#3c2048', '#27163a')
    return c


def northern_lights():
    c = Canvas()
    c.sky([(0, '#02040d'), (.6, '#07162e'), (1, '#10304a')])
    c.stars(450, 400, 41)
    px = c.px
    ribbons = [(230, 70, 1.3, .5, 0.85, ((70, 255, 170), (60, 200, 255), (170, 100, 255))),
               (165, 50, 1.9, 2.0, 0.55, ((120, 255, 200), (90, 130, 255), (200, 90, 255)))]
    for center, wobble, freq, phase, strength, palette in ribbons:
        for x in range(W):
            u = x / W
            cy = center + wobble * math.sin(2 * math.pi * freq * u + phase) + 25 * math.sin(2 * math.pi * 3.1 * u)
            col = lerp(palette[0], palette[1], u * 2) if u < .5 else lerp(palette[1], palette[2], (u - .5) * 2)
            for y in range(int(cy - 170), int(cy + 70)):
                if not 0 <= y < H:
                    continue
                dy = y - cy
                sigma = 26.0 if dy > 0 else 80.0
                w = strength * math.exp(-(dy * dy) / (2 * sigma * sigma))
                if w < .01:
                    continue
                i = (y * W + x) * 3
                for k in range(3):
                    px[i + k] = min(255, int(px[i + k] + col[k] * w))
    c.ridge(ridged(610, 55, (1.2, 2.6, 4.5), 42, sharp=False), '#a9c0d8', '#4d6a88')
    c.ridge(ridged(675, 38, (1.7, 3.3), 43, sharp=False), '#0b1a2a', '#050b14')
    return c


SCENES = {"mountains": mountains, "night_sky": night_sky, "city": city, "sunset_sea": sunset_sea,
          "pine_forest": pine_forest, "desert": desert, "northern_lights": northern_lights}


def encode(ppm, out, width, height, noise):
    filters = f"scale={width}:{height}:flags=bicubic" + (",noise=alls=4:allf=u" if noise else "")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ppm, "-vf", filters, "-q:v", "3", out], check=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for name, scene in SCENES.items():
            ppm = os.path.join(tmp, name + ".ppm")
            scene().save(ppm)
            encode(ppm, os.path.join(OUT, f"scene_{name}.jpg"), 1920, 1080, True)
            encode(ppm, os.path.join(OUT, f"scene_{name}_thumb.jpg"), 320, 180, False)
            print("wrote", name, flush=True)


if __name__ == "__main__":
    main()
