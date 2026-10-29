"""Renders the dot-matrix world map used by the Wi-Fi page.

Usage: python3 -I tools/generate_world_map.py <ne_110m_land.geojson> <output.png>

Land outlines: Natural Earth (public domain), https://www.naturalearthdata.com
The projection (Mercator, 16:9, bottom at LAT_BOTTOM) must match MapProjection.java.
"""
import json
import math
import struct
import sys
import zlib

LON_MIN, LON_MAX = -180.0, 180.0
LAT_BOTTOM = -56.0                 # just below Tierra del Fuego; Antarctica is left out
WIDTH_PX, HEIGHT_PX = 1920, 1080   # 16:9, so the map fills a TV screen
CELL_PX = 8                        # one dot per 8x8 px cell
DOT_RADIUS_PX = 2.6


def mercator_y(latitude_deg):
    return math.log(math.tan(math.pi / 4 + math.radians(latitude_deg) / 2))


def latitude_from_y(y):
    return math.degrees(2 * math.atan(math.exp(y)) - math.pi / 2)


Y_BOTTOM = mercator_y(LAT_BOTTOM)
Y_TOP = Y_BOTTOM + 2 * math.pi / (WIDTH_PX / HEIGHT_PX)   # the top follows from the 16:9 shape


def load_polygons(path):
    """Returns [(bounding box, rings)] for every land polygon."""
    with open(path) as source:
        features = json.load(source)["features"]
    polygons = []
    for feature in features:
        geometry = feature["geometry"]
        parts = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
        for rings in parts:
            lons = [point[0] for point in rings[0]]
            lats = [point[1] for point in rings[0]]
            polygons.append(((min(lons), min(lats), max(lons), max(lats)), rings))
    return polygons


def inside_ring(lon, lat, ring):
    inside = False
    j = len(ring) - 1
    for i in range(len(ring)):
        xi, yi = ring[i][0], ring[i][1]
        xj, yj = ring[j][0], ring[j][1]
        if (yi > lat) != (yj > lat) and lon < (xj - xi) * (lat - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def is_land(lon, lat, polygons):
    for (min_lon, min_lat, max_lon, max_lat), rings in polygons:
        if not (min_lon <= lon <= max_lon and min_lat <= lat <= max_lat):
            continue
        # Even-odd across the outer ring and any holes.
        if sum(inside_ring(lon, lat, ring) for ring in rings) % 2 == 1:
            return True
    return False


def dot_pattern():
    """Anti-aliased alpha values for one CELL_PX x CELL_PX dot, as rows."""
    centre = CELL_PX / 2.0
    rows = []
    for y in range(CELL_PX):
        row = []
        for x in range(CELL_PX):
            distance = math.hypot(x + 0.5 - centre, y + 0.5 - centre)
            row.append(int(round(255 * min(1.0, max(0.0, DOT_RADIUS_PX + 0.5 - distance)))))
        rows.append(row)
    return rows


def write_png(path, width, height, row_bytes):
    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + row for row in row_bytes)  # filter type 0 on every scanline
    header = struct.pack(">IIBBBBB", width, height, 8, 4, 0, 0, 0)  # 8-bit grayscale + alpha
    with open(path, "wb") as out:
        out.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main(land_path, output_path):
    polygons = load_polygons(land_path)
    columns = WIDTH_PX // CELL_PX
    rows = HEIGHT_PX // CELL_PX

    land = []
    for r in range(rows):
        lat = latitude_from_y(Y_TOP - (r + 0.5) / rows * (Y_TOP - Y_BOTTOM))
        land.append([is_land(LON_MIN + (c + 0.5) / columns * (LON_MAX - LON_MIN), lat, polygons)
                     for c in range(columns)])

    blank = bytes([255, 0]) * CELL_PX
    dot_rows = [b"".join(bytes([255, alpha]) for alpha in row) for row in dot_pattern()]
    scanlines = []
    for r in range(rows):
        for y in range(CELL_PX):
            scanlines.append(b"".join(dot_rows[y] if land[r][c] else blank for c in range(columns)))

    write_png(output_path, columns * CELL_PX, rows * CELL_PX, scanlines)
    print(f"{columns}x{rows} cells, {sum(map(sum, land))} land dots -> {columns * CELL_PX}x{rows * CELL_PX}px, "
          f"top latitude {latitude_from_y(Y_TOP):.2f}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
