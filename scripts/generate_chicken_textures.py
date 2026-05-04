#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Batch-generate chickens mod entity PNGs by tinting a grayscale chicken silhouette.

Requirements:
    pip install pillow

Recommended base (better than built-in silhouette):
    Copy from Minecraft jar: assets/minecraft/textures/entity/chicken.png
    Resize if needed so width is 64; convert to grayscale and save as:
        scripts/chicken_base.png

Run from fabric-1.20.1:
    python scripts/generate_chicken_textures.py
    python scripts/generate_chicken_textures.py --base path/to/grayscale.png
    python scripts/generate_chicken_textures.py --fix-json

Textures are written to:
    src/main/resources/assets/chickens/textures/entity/<id>_chicken.png
one file per datapack chicken JSON basename (matches each "鸡"条目).

Skip --fix-json if you intentionally keep minecraft:textures/entity/chicken.png for some entries;
use --fix-json to set texture to chickens:textures/entity/<id>_chicken.png for every json except
those whose texture already uses minecraft: (skipped by default when fixing).
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
	from PIL import Image, ImageDraw, ImageOps
except ImportError:
	print("Install Pillow:  pip install pillow", file=sys.stderr)
	sys.exit(1)


FABRIC_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JSON_DIR = FABRIC_ROOT / "src/main/resources/data/chickens/chickens"
DEFAULT_OUT_ENTITY = FABRIC_ROOT / "src/main/resources/assets/chickens/textures/entity"
DEFAULT_SYNTH_BASE = Path(__file__).resolve().parent / "chicken_base.png"


def rgb_int_to_tuple(c: int) -> tuple[int, int, int]:
	c &= 0xFFFFFF
	return (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF


def make_synthetic_base(path: Path, size: tuple[int, int] = (64, 32)) -> None:
	w, h = size
	im = Image.new("L", (w, h), 0)
	dr = ImageDraw.Draw(im)
	# Rough chicken side-view mask: dark=legs/outline, bright=body (for ImageOps.colorize)
	body = (int(w * 0.18), int(h * 0.38), int(w * 0.72), int(h * 0.92))
	head = (int(w * 0.62), int(h * 0.28), int(w * 0.92), int(h * 0.72))
	beak = (int(w * 0.84), int(h * 0.52), int(w * 0.98), int(h * 0.62))
	comb = (int(w * 0.72), int(h * 0.18), int(w * 0.88), int(h * 0.34))
	l_leg = (int(w * 0.38), int(h * 0.88), int(w * 0.46), int(h * 1.0))
	r_leg = (int(w * 0.50), int(h * 0.88), int(w * 0.58), int(h * 1.0))

	for rect in (body, head, beak):
		dr.rectangle(rect, fill=220)
	for rect in (comb,):
		dr.rectangle(rect, fill=235)
	for rect in (l_leg, r_leg):
		dr.rectangle(rect, fill=120)
	im.save(path)


def load_base_grayscale(path: Path | None) -> Image.Image:
	if path is not None and path.is_file():
		im = Image.open(path).convert("RGBA")
	else:
		if not DEFAULT_SYNTH_BASE.is_file():
			make_synthetic_base(DEFAULT_SYNTH_BASE)
		im = Image.open(DEFAULT_SYNTH_BASE).convert("RGBA")
	w, h = im.size
	# Typical chicken entity sheet is fairly wide — keep proportions, cap width 64 like vanilla-ish
	scale = min(1.0, 64 / max(w, 1))
	if scale != 1.0:
		im = im.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.Resampling.LANCZOS)
	gray = im.convert("RGB").convert("L")
	return gray


def tint_chicken(base_l: Image.Image, bg_rgb: tuple[int, int, int], fg_rgb: tuple[int, int, int]) -> Image.Image:
	"""Map luminance so dark areas pick bg-ish, highlights pick fg-ish."""
	shade_bg = tuple(max(8, min(255, int(c * 0.65))) for c in bg_rgb)
	high_fg = tuple(min(255, int(c + (255 - c) * 0.35)) if c > 120 else min(255, c + 55) for c in fg_rgb)
	return ImageOps.colorize(base_l, shade_bg, high_fg)


def main() -> int:
	ap = argparse.ArgumentParser(description="Generate tinted chicken entity textures from datapack JSON.")
	ap.add_argument("--json-dir", type=Path, default=DEFAULT_JSON_DIR)
	ap.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_ENTITY)
	ap.add_argument(
		"--base",
		type=Path,
		default=None,
		help="Grayscale PNG (recommended: grayscale vanilla chicken). Default: synthetic scripts/chicken_base.png",
	)
	ap.add_argument(
		"--fix-json",
		action="store_true",
		help="Rewrite texture in each json to chickens:textures/entity/<id>_chicken.png unless it was minecraft:",
	)
	ap.add_argument(
		"--include-minecraft-texture-rows",
		action="store_true",
		help="With --fix-json, also overwrite rows that pointed at minecraft textures.",
	)
	args = ap.parse_args()

	if not args.json_dir.is_dir():
		print(f"Missing json dir: {args.json_dir}", file=sys.stderr)
		return 1

	args.out_dir.mkdir(parents=True, exist_ok=True)
	base_l = load_base_grayscale(args.base)

	files = sorted(args.json_dir.glob("*.json"))
	if not files:
		print(f"No json in {args.json_dir}", file=sys.stderr)
		return 1

	for jf in files:
		with jf.open(encoding="utf-8") as f:
			data = json.load(f)
		chicken_id = jf.stem
		bg = int(data.get("bg_color", 0xFFFFFF))
		fg = int(data.get("fg_color", 0xFFFF00))
		im = tint_chicken(base_l, rgb_int_to_tuple(bg), rgb_int_to_tuple(fg))
		out_png = args.out_dir / f"{chicken_id}_chicken.png"
		im.save(out_png)
		print(f"Wrote {out_png.relative_to(FABRIC_ROOT)}")

		if args.fix_json:
			tex_key = data.get("texture", "")
			if tex_key.startswith("minecraft:") and not args.include_minecraft_texture_rows:
				print(f"  skip json patch (minecraft texture): {jf.name}")
				continue
			new_tex = f"chickens:textures/entity/{chicken_id}_chicken.png"
			if data.get("texture") != new_tex:
				data["texture"] = new_tex
				with jf.open("w", encoding="utf-8", newline="\n") as f:
					json.dump(data, f, indent=2, ensure_ascii=False)
					f.write("\n")
				print(f"  updated texture in {jf.name}")

	print("Done.")
	return 0


if __name__ == "__main__":
	sys.exit(main())
