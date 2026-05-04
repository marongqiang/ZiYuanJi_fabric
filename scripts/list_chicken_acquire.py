#!/usr/bin/env python3
"""Print spawn_type and breeding parents from datapack chicken JSONs."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent / "src/main/resources/data/chickens/chickens"
rows = []
for p in sorted(root.glob("*.json")):
	d = json.loads(p.read_text(encoding="utf-8"))
	rows.append(
		{
			"id": p.stem,
			"spawn": d.get("spawn_type", "NONE"),
			"p1": d.get("parent1"),
			"p2": d.get("parent2"),
		}
	)

natural = [r for r in rows if r["spawn"] != "NONE"]
breed = [r for r in rows if r["p1"] and r["p2"]]
manual = [
	r
	for r in rows
	if r["spawn"] == "NONE" and not (r["p1"] and r["p2"])
]


def strip_ns(s):
	return (s or "").replace("chickens:", "") if s else ""


print("# natural_spawn (world generation)")
for r in sorted(natural, key=lambda x: (x["spawn"], x["id"])):
	print(f"{r['id']}\t{r['spawn']}")

print("\n# breed_only (has parent1+parent2 in datapack)")
for r in sorted(breed, key=lambda x: x["id"]):
	print(f"{r['id']}\t{strip_ns(r['p1'])} + {strip_ns(r['p2'])}")

print("\n# no_spawn_no_parents_datapack (smart / specials — see mods)")
for r in sorted(manual, key=lambda x: x["id"]):
	print(r["id"])
