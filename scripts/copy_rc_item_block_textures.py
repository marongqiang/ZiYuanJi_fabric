#!/usr/bin/env python3
"""
Copy Resource Chickens textures/item/*.png and selected vanilla-aligned block textures into this mod.

  python scripts/copy_rc_item_block_textures.py
  python scripts/copy_rc_item_block_textures.py --dry-run

Source: ../Resource Chickens-1.20.1-2.4a/assets/resourcechickens/textures/
Targets:
  assets/chickens/textures/item/
  assets/chickens/textures/block/
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPO = ROOT.parent
RC_TEX = REPO / "Resource Chickens-1.20.1-2.4a/assets/resourcechickens/textures"
DEST_ITEM = ROOT / "src/main/resources/assets/chickens/textures/item"
DEST_BLOCK = ROOT / "src/main/resources/assets/chickens/textures/block"

# Vanilla-adjacent world blocks RC ships (hay/nest/eggs-on-ground)
EXTRA_BLOCK_FILES = frozenset({"hay_grass.png", "nest.png", "egg_block.png"})


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--dry-run", action="store_true")
	args = ap.parse_args()

	if not RC_TEX.is_dir():
		print(f"Missing {RC_TEX}", file=sys.stderr)
		return 1

	DEST_ITEM.mkdir(parents=True, exist_ok=True)
	DEST_BLOCK.mkdir(parents=True, exist_ok=True)

	n = 0
	src_item = RC_TEX / "item"
	if src_item.is_dir():
		for png in sorted(src_item.glob("*.png")):
			dest = DEST_ITEM / png.name
			print(f"item/{png.name} -> {dest.relative_to(ROOT)}")
			if not args.dry_run:
				shutil.copy2(png, dest)
			n += 1

	src_block = RC_TEX / "block"
	if src_block.is_dir():
		for png_name in sorted(EXTRA_BLOCK_FILES):
			png = src_block / png_name
			if not png.is_file():
				continue
			dest = DEST_BLOCK / png_name
			print(f"block/{png.name} -> {dest.relative_to(ROOT)}")
			if not args.dry_run:
				shutil.copy2(png, dest)
			n += 1

	print(f"Done. files={n} dry_run={args.dry_run}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
