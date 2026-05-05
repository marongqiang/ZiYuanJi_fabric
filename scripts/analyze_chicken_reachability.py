#!/usr/bin/env python3
"""Analyze which chicken types are reachable from world gen + breeding (+ optional smart/teach)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/data/chickens/chickens"

WOOD = {
	"log",
	"spruce_log",
	"birch_log",
	"jungle_log",
	"acacia_log",
	"dark_oak_log",
	"mangrove_log",
	"cherry_log",
	"crimson_stem",
	"warped_stem",
}

# ChickenTeachHandler: book+vanilla -> smart; smart + items -> these types
TEACH_FROM_SMART = {
	"red",
	"white",
	"blue",
	"green",
	"black",
	"yellow",
	"snowball",
	"log",
	"spruce_log",
	"birch_log",
	"jungle_log",
	"acacia_log",
	"dark_oak_log",
	"mangrove_log",
	"cherry_log",
	"crimson_stem",
	"warped_stem",
	"water",
	"totem",
	"nether_star",
}


def load_types():
	types = {}
	for p in ROOT.glob("*.json"):
		d = json.loads(p.read_text(encoding="utf-8"))
		stem = p.stem
		p1 = d.get("parent1")
		p2 = d.get("parent2")
		types[stem] = {
			"spawn": d.get("spawn_type", "NONE"),
			"p1": (p1 or "").replace("chickens:", "") or None,
			"p2": (p2 or "").replace("chickens:", "") or None,
		}
	return types


def can_spawn_naturally_java(types: dict, tid: str) -> bool:
	"""Match ChickenType.canSpawnNaturally(): isBase && spawn != NONE; isBase = p1 null OR p2 null."""
	t = types[tid]
	if t["spawn"] == "NONE":
		return False
	return t["p1"] is None or t["p2"] is None


def strict_datapack_base_spawn(types: dict, tid: str) -> bool:
	"""Both parents absent in JSON and spawn != NONE (clean world-gen chickens)."""
	t = types[tid]
	return t["spawn"] != "NONE" and t["p1"] is None and t["p2"] is None


def matches_parent(expected: str, actual: str) -> bool:
	return expected == actual or (expected in WOOD and actual in WOOD)


def is_child_of(types: dict, child_id: str, p1: str, p2: str) -> bool:
	t = types[child_id]
	a, b = t["p1"], t["p2"]
	if a is None or b is None:
		return False
	return (matches_parent(a, p1) and matches_parent(b, p2)) or (
		matches_parent(a, p2) and matches_parent(b, p1)
	)


def children_and_parents(types: dict, p1: str, p2: str) -> list[str]:
	result = []
	if p1 in types:
		result.append(p1)
	if p2 in types:
		result.append(p2)
	for tid in types:
		if is_child_of(types, tid, p1, p2):
			result.append(tid)
	seen: set[str] = set()
	out: list[str] = []
	for x in result:
		if x not in seen:
			seen.add(x)
			out.append(x)
	return out


def bfs_reach(types: dict, seeds: set[str], add_smelting_chain: bool) -> set[str]:
	r = set(seeds)
	if add_smelting_chain and "cobblestone" in r:
		r.add("stone")
		r.add("smooth_stone")
	changed = True
	while changed:
		changed = False
		for a in list(r):
			for b in list(r):
				for c in children_and_parents(types, a, b):
					if c not in r:
						r.add(c)
						changed = True
		if add_smelting_chain and "cobblestone" in r:
			if "stone" not in r or "smooth_stone" not in r:
				if "stone" not in r:
					r.add("stone")
					changed = True
				if "smooth_stone" not in r:
					r.add("smooth_stone")
					changed = True
	return r


def reach_with_smart_teach_iterative(types: dict, seeds: set[str]) -> set[str]:
	"""When 'smart' is in the set, all TEACH_FROM_SMART types become obtainable (book + use)."""
	r = set(seeds)
	changed = True
	while changed:
		changed = False
		if "smart" in r:
			for tid in TEACH_FROM_SMART:
				if tid not in r:
					r.add(tid)
					changed = True
		nr = bfs_reach(types, r, add_smelting_chain=True)
		if nr != r:
			r = nr
			changed = True
	return r


def main():
	types = load_types()
	all_ids = set(types)

	strict_natural = {tid for tid in all_ids if strict_datapack_base_spawn(types, tid)}
	print("Strict datapack natural (no parents, spawn != NONE):", sorted(strict_natural))

	java_natural = {tid for tid in all_ids if can_spawn_naturally_java(types, tid)}
	print("Java canSpawnNaturally (spawn != NONE AND (no p1 OR no p2)):", sorted(java_natural))

	misleading = sorted(tid for tid in all_ids if types[tid]["spawn"] != "NONE" and not strict_datapack_base_spawn(types, tid))
	print("spawn_type set but NOT strict base (extra parents or single field):", misleading)

	# Game often keeps default white when candidate list empty (nether/cold)
	seeds_a = set(strict_natural) | {"white"}
	reach_a = bfs_reach(types, seeds_a, add_smelting_chain=True)
	print("\n=== From strict_natural + white + smelting + breeding closure ===")
	print("Unreachable:", sorted(all_ids - reach_a))

	# Book on vanilla chicken -> smart only; then teach unlocks dye/log/water/totem/star chickens
	seeds_c = set(strict_natural) | {"white", "smart"}
	reach_c = reach_with_smart_teach_iterative(types, seeds_c)
	print("\n=== strict_natural + white + smart (book) + iterative teach + breeding ===")
	print("Unreachable:", sorted(all_ids - reach_c))


if __name__ == "__main__":
	main()
