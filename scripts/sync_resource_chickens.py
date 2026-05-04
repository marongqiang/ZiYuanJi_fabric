#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sync breeding / egg colors / spawn flags from Resource Chickens 1.20.1 configs
into this mod's data/chickens/chickens/*.json.

Source folder (repo root):
    Resource Chickens-1.20.1-2.4a/data/resourcechickens/configs/vanilla/*.json

Usage (from fabric-1.20.1):
    python scripts/sync_resource_chickens.py
    python scripts/sync_resource_chickens.py --apply

Policies (this port-specific):
    - Egg colors always taken from RC when a matching chicken json exists.
    - Parents: only updated when RC provides at least one parent AND every parent maps to an
      existing chickens:* json in this mod (skip if e.g. resourcechickens:spider).
    - If RC leaves parents blank, existing parent1/parent2 in THIS mod are NOT stripped
      (keeps forks like lapiz = blue+diamond).
    - spawn_type from RC unless the chicken id is in NO_RC_SPAWN (smart-only dyes & logs).

Textures: unpacked reference in this workspace has NO .png files. Pull from the original .jar:

    jar xf "ResourceChicken...jar" assets/resourcechickens/textures
Then manually align names or symlink into assets/chickens/textures/entity/.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

FABRIC_ROOT = Path(__file__).resolve().parent.parent
REPO_ROOT = FABRIC_ROOT.parent
RC_VANILLA = REPO_ROOT / "Resource Chickens-1.20.1-2.4a/data/resourcechickens/configs/vanilla"
CHICKEN_JSON_DIR = FABRIC_ROOT / "src/main/resources/data/chickens/chickens"

STEM_TO_ID: dict[str, str] = {
	"snowman": "snowball",
	"dye_light_gray": "silver_dye",
	"pcrystal": "prismarine",
}

# These are obtained via smart chicken + stacks in this fork; ignore RC spawn for them.
NO_RC_SPAWN: frozenset[str] = frozenset({
	"smart",
	"red", "white", "blue", "green", "black", "yellow",
	"log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
	"mangrove_log", "cherry_log", "crimson_stem", "warped_stem",
})


def rc_stem_to_chickens_id(stem: str) -> str:
	if stem in STEM_TO_ID:
		return STEM_TO_ID[stem]
	if stem.startswith("dye_"):
		return stem[len("dye_") :]
	return stem


def parse_parent(ref: str) -> str | None:
	if not ref or not ref.strip():
		return None
	m = re.match(r"^resourcechickens:([a-z0-9_]+)$", ref.strip())
	if not m:
		return None
	return rc_stem_to_chickens_id(m.group(1))


def chickens_json_exists(cid: str) -> bool:
	return (CHICKEN_JSON_DIR / f"{cid}.json").is_file()


def rc_spawn_to_ours(rc: dict) -> str:
	parents = bool((rc.get("parentA") or "").strip()) or bool((rc.get("parentB") or "").strip())
	if not rc.get("spawnNaturally", False):
		return "NONE"
	if parents:
		return "NONE"
	st = int(rc.get("spawnType", 1))
	if st == 0:
		return "HELL"
	return "NORMAL"


def int_from_rc_color(s: str) -> int:
	return int(str(s).strip(), 10)


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--apply", action="store_true")
	args = ap.parse_args()

	if not RC_VANILLA.is_dir():
		print(f"Missing Resource Chickens configs: {RC_VANILLA}", file=sys.stderr)
		return 1
	if not CHICKEN_JSON_DIR.is_dir():
		print(f"Missing target dir: {CHICKEN_JSON_DIR}", file=sys.stderr)
		return 1

	updated = 0
	unchanged = 0

	for rc_path in sorted(RC_VANILLA.glob("*.json")):
		stem = rc_path.stem
		target_id = rc_stem_to_chickens_id(stem)
		if chickens_json_exists(stem):
			target_id = stem
		out_path = CHICKEN_JSON_DIR / f"{target_id}.json"
		if not out_path.is_file():
			print(f"[skip] no fabric chicken for RC {rc_path.name} -> {target_id}")
			continue

		with rc_path.open(encoding="utf-8") as f:
			rc = json.load(f)
		with out_path.open(encoding="utf-8") as f:
			ch = json.load(f)

		try:
			bg = int_from_rc_color(rc.get("eggColorBackground", "16777215"))
			fg = int_from_rc_color(rc.get("eggColorForeground", "16776960"))
		except ValueError as e:
			print(f"[warn] bad colors in {rc_path.name}: {e}")
			continue

		changes: list[str] = []

		ra = (rc.get("parentA") or "").strip()
		rb = (rc.get("parentB") or "").strip()
		if ra or rb:
			pa = parse_parent(ra) if ra else None
			pb = parse_parent(rb) if rb else None
			ok = True
			missing_reason = ""
			for label, stem_id in (("parentA", pa), ("parentB", pb)):
				if stem_id is not None and not chickens_json_exists(stem_id):
					ok = False
					missing_reason = f"{label} -> {stem_id} (no {stem_id}.json)"
					break
			if ok:
				n1 = f"chickens:{pa}" if pa else None
				n2 = f"chickens:{pb}" if pb else None
				if ch.get("parent1") != n1 or ch.get("parent2") != n2:
					changes.append(f"parents -> {n1}, {n2}")
				if pa:
					ch["parent1"] = n1
				else:
					ch.pop("parent1", None)
				if pb:
					ch["parent2"] = n2
				else:
					ch.pop("parent2", None)
			else:
				print(f"[warn] {out_path.name}: skip RC parents ({missing_reason}), keep yours")

		spawn = rc_spawn_to_ours(rc)
		if target_id not in NO_RC_SPAWN:
			if ch.get("spawn_type") != spawn:
				changes.append(f"spawn_type {ch.get('spawn_type')} -> {spawn}")
			ch["spawn_type"] = spawn

		if ch.get("bg_color") != bg or ch.get("fg_color") != fg:
			changes.append(f"colors bg {ch.get('bg_color')}->{bg} fg {ch.get('fg_color')}->{fg}")
		ch["bg_color"] = bg
		ch["fg_color"] = fg

		if changes:
			print(f"{out_path.name}: " + "; ".join(changes))
			updated += 1
			if args.apply:
				with out_path.open("w", encoding="utf-8", newline="\n") as f:
					json.dump(ch, f, indent=2, ensure_ascii=False)
					f.write("\n")
		else:
			unchanged += 1

	dry = not args.apply
	print(f"\nDone. updated_lines={updated} unchanged={unchanged} dry_run={dry}")
	if dry and updated:
		print("Re-run with --apply to write files.", file=sys.stderr)
	return 0


if __name__ == "__main__":
	sys.exit(main())
