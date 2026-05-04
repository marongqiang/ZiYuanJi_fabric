#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Copy Resource Chickens entity PNGs into this mod's assets/chickens/textures/entity/<id>_chicken.png.

Source:
    ../Resource Chickens-1.20.1-2.4a/assets/resourcechickens/textures/entity/vanilla/*.png

Run from fabric-1.20.1:
    python scripts/copy_resource_chicken_textures.py
    python scripts/copy_resource_chicken_textures.py --dry-run

Copies RC art whenever resolve_src finds a PNG (including pairs like smooth_stone→stone.png); skips only when unknown.
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

FABRIC_ROOT = Path(__file__).resolve().parent.parent
REPO_ROOT = FABRIC_ROOT.parent
RC_ENTITY_VANILLA = (
	REPO_ROOT
	/ "Resource Chickens-1.20.1-2.4a/assets/resourcechickens/textures/entity/vanilla"
)
RC_ENTITY_MODDED = (
	REPO_ROOT
	/ "Resource Chickens-1.20.1-2.4a/assets/resourcechickens/textures/entity/modded"
)
TARGET_ENTITY = FABRIC_ROOT / "src/main/resources/assets/chickens/textures/entity"
CHICKEN_JSON = FABRIC_ROOT / "src/main/resources/data/chickens/chickens"

DYE_IDS = frozenset({
	"black", "blue", "brown", "cyan", "gray", "green", "light_blue",
	"lime", "magenta", "orange", "pink", "purple", "red", "silver_dye",
	"white", "yellow",
})

# chickens id -> filename inside RC vanilla (without .png)
SPECIAL_RC_STEM: dict[str, str] = {
	"silver_dye": "dye_light_gray",
	"snowball": "snowman",
	"prismarine": "pcrystal",
}

# No 1:1 RC chicken file — pick closest vanilla art
FALLBACK_RC_STEM: dict[str, str] = {
	"smart": "chicken",
	"experience": "xp",
	"book": "leather",
	"rotten_flesh": "leather",
	"shulker_shell": "pshard",
	"string": "chicken",
	"ender": "enderman",
	"gunpowder": "creeper",
	"paper": "chicken",
	# RC 无单独 PNG：沿用同套件里最接近的皮
	"smooth_stone": "stone",
	"deepslate": "obsidian",
}


def resolve_src(cid: str) -> Path | None:
	if cid in SPECIAL_RC_STEM:
		p = RC_ENTITY_VANILLA / f"{SPECIAL_RC_STEM[cid]}.png"
		return p if p.is_file() else None
	if cid in FALLBACK_RC_STEM:
		p = RC_ENTITY_VANILLA / f"{FALLBACK_RC_STEM[cid]}.png"
		if p.is_file():
			return p
	if cid in DYE_IDS and cid != "silver_dye":
		p = RC_ENTITY_VANILLA / f"dye_{cid}.png"
		if p.is_file():
			return p
	for folder in (RC_ENTITY_VANILLA, RC_ENTITY_MODDED):
		p = folder / f"{cid}.png"
		if p.is_file():
			return p
	if cid.endswith("_log") or cid.endswith("_stem"):
		p = RC_ENTITY_VANILLA / "log.png"
		return p if p.is_file() else None
	return None


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--dry-run", action="store_true")
	args = ap.parse_args()

	if not RC_ENTITY_VANILLA.is_dir():
		print(f"Missing: {RC_ENTITY_VANILLA}", file=sys.stderr)
		return 1
	TARGET_ENTITY.mkdir(parents=True, exist_ok=True)

	copied = 0
	skipped = 0
	for jf in sorted(CHICKEN_JSON.glob("*.json")):
		cid = jf.stem
		with jf.open(encoding="utf-8") as f:
			data = json.load(f)
		src = resolve_src(cid)
		if src is None or not src.is_file():
			print(f"[skip] no RC png for chickens:{cid}")
			skipped += 1
			continue
		dest = TARGET_ENTITY / f"{cid}_chicken.png"
		print(f"{cid}: {src.name} -> {dest.relative_to(FABRIC_ROOT)}")
		if not args.dry_run:
			shutil.copy2(src, dest)
		copied += 1

	print(f"\nDone. copied={copied} skipped={skipped} dry_run={args.dry_run}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
