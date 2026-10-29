#!/usr/bin/env python3
"""Draws the mod-list logo (src/main/resources/immersive_spellcraft_logo.png): a sigil ring with three stations'
motifs: the scribe's quill and its inked stroke, the forge's school focus notch, and the press's die over a glyph plate.
Original art, rendered at 4x and downsampled for smooth edges."""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "immersive_spellcraft_logo.png")
SIZE = 256
S = 4
W = SIZE * S
C = W / 2

VIOLET = (0xB5, 0x7B, 0xFF)
INK = (0x2B, 0x1D, 0x3A)
PARCHMENT = (0xE9, 0xDF, 0xC6)
BRASS = (0xD9, 0xA4, 0x4E)
MOLTEN = (0xFF, 0x9A, 0x3C)
GREEN = (0x00, 0x9E, 0x73)


def ring(d, r, width, color):
    d.ellipse([C - r, C - r, C + r, C + r], outline=color, width=width)


def main():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([C - 470, C - 470, C + 470, C + 470], fill=VIOLET + (90,))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(60)))
    d = ImageDraw.Draw(img)

    # Parchment disc and the sigil ring.
    d.ellipse([C - 430, C - 430, C + 430, C + 430], fill=PARCHMENT + (255,))
    ring(d, 430, 26, INK + (255,))
    ring(d, 372, 10, VIOLET + (255,))
    for i in range(12):
        a = math.pi * 2 * i / 12
        x0, y0 = C + math.cos(a) * 386, C + math.sin(a) * 386
        x1, y1 = C + math.cos(a) * 414, C + math.sin(a) * 414
        d.line([x0, y0, x1, y1], fill=INK + (255,), width=12)

    # The forge's focus notch on the ring and its green mark.
    a = -math.pi / 2
    d.polygon([(C + math.cos(a) * 470, C + math.sin(a) * 470), (C + math.cos(a - 0.09) * 360, C + math.sin(a - 0.09) * 360),
               (C + math.cos(a + 0.09) * 360, C + math.sin(a + 0.09) * 360)], fill=GREEN + (255,))

    # An inked glyph stroke: a quadratic curve through three junctions.
    pts = [(C - 230, C + 150), (C - 40, C - 170), (C + 210, C + 40)]
    def bez(t):
        u = 1 - t
        return (u * u * pts[0][0] + 2 * u * t * pts[1][0] + t * t * pts[2][0], u * u * pts[0][1] + 2 * u * t * pts[1][1] + t * t * pts[2][1])
    curve = [bez(i / 60) for i in range(61)]
    d.line(curve, fill=INK + (255,), width=34, joint="curve")
    for p in (pts[0], pts[2]):
        d.ellipse([p[0] - 30, p[1] - 30, p[0] + 30, p[1] + 30], fill=VIOLET + (255,), outline=INK + (255,), width=10)

    # The quill lying along the stroke's end.
    qx, qy = pts[2]
    d.polygon([(qx, qy), (qx + 150, qy - 250), (qx + 210, qy - 230), (qx + 40, qy - 10)], fill=(250, 246, 236, 255), outline=INK + (255,))
    d.line([qx + 8, qy - 6, qx + 175, qy - 238], fill=INK + (255,), width=8)

    # The press's die over a small glowing plate.
    d.rounded_rectangle([C - 120, C + 210, C + 120, C + 300], radius=18, fill=MOLTEN + (255,), outline=INK + (255,), width=14)
    d.rounded_rectangle([C - 70, C + 120, C + 70, C + 190], radius=10, fill=BRASS + (255,), outline=INK + (255,), width=12)
    d.rectangle([C - 18, C + 60, C + 18, C + 120], fill=BRASS + (255,), outline=INK + (255,), width=10)

    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
