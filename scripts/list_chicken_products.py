#!/usr/bin/env python3
"""Print each chicken id -> lay item (+ optional drop_item from datapack)."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHICKENS = ROOT / "src/main/resources/data/chickens/chickens"
LANG = ROOT / "src/main/resources/assets/chickens/lang/zh_cn.json"

CN_ITEM: dict[str, str] = {
	"minecraft:cobblestone": "圆石",
	"minecraft:stone": "石头",
	"minecraft:smooth_stone": "平滑石",
	"minecraft:deepslate": "深板岩",
	"minecraft:flint": "燧石",
	"minecraft:quartz": "下界石英",
	"minecraft:sand": "沙子",
	"minecraft:string": "线",
	"minecraft:glowstone_dust": "荧石粉",
	"minecraft:gunpowder": "火药",
	"minecraft:redstone": "红石粉",
	"minecraft:glass": "玻璃",
	"minecraft:iron_ingot": "铁锭",
	"minecraft:coal": "煤炭",
	"minecraft:gold_ingot": "金锭",
	"minecraft:gold_nugget": "金粒",
	"minecraft:snowball": "雪球",
	"minecraft:clay_ball": "黏土球",
	"minecraft:leather": "皮革",
	"minecraft:nether_wart": "下界疣",
	"minecraft:diamond": "钻石",
	"minecraft:blaze_rod": "烈焰棒",
	"minecraft:slime_ball": "黏液球",
	"minecraft:ender_pearl": "末影珍珠",
	"minecraft:ghast_tear": "恶魂之泪",
	"minecraft:emerald": "绿宝石",
	"minecraft:lapis_lazuli": "青金石",
	"minecraft:bone": "骨头",
	"minecraft:rotten_flesh": "腐肉",
	"minecraft:obsidian": "黑曜石",
	"minecraft:experience_bottle": "附魔之瓶",
	"minecraft:paper": "纸",
	"minecraft:book": "书",
	"minecraft:prismarine_shard": "海晶碎片",
	"minecraft:shulker_shell": "潜影壳",
	"minecraft:magma_cream": "岩浆膏",
	"minecraft:white_dye": "白色染料",
	"minecraft:orange_dye": "橙色染料",
	"minecraft:magenta_dye": "品红染料",
	"minecraft:light_blue_dye": "淡蓝色染料",
	"minecraft:yellow_dye": "黄色染料",
	"minecraft:lime_dye": "黄绿色染料",
	"minecraft:pink_dye": "粉红色染料",
	"minecraft:gray_dye": "灰色染料",
	"minecraft:light_gray_dye": "淡灰色染料",
	"minecraft:cyan_dye": "青色染料",
	"minecraft:purple_dye": "紫色染料",
	"minecraft:blue_dye": "蓝色染料",
	"minecraft:brown_dye": "棕色染料",
	"minecraft:green_dye": "绿色染料",
	"minecraft:red_dye": "红色染料",
	"minecraft:black_dye": "黑色染料",
	"minecraft:oak_log": "橡木原木",
	"minecraft:spruce_log": "云杉原木",
	"minecraft:birch_log": "白桦原木",
	"minecraft:jungle_log": "丛林木原木",
	"minecraft:acacia_log": "金合欢原木",
	"minecraft:dark_oak_log": "深色橡木原木",
	"minecraft:mangrove_log": "红树原木",
	"minecraft:cherry_log": "樱花原木",
	"minecraft:crimson_stem": "绯红菌柄",
	"minecraft:warped_stem": "诡异菌柄",
	"minecraft:feather": "羽毛",
	"minecraft:netherite_scrap": "下界合金碎片",
	"minecraft:elytra": "鞘翅",
	"minecraft:totem_of_undying": "不死图腾",
	"minecraft:nether_star": "下界之星",
	"minecraft:spider_eye": "蜘蛛眼",
	"chickens:liquid_egg": "模组·液体蛋",
	"chickens:colored_egg": "模组·彩色蛋（类型由 NBT 决定）",
}


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("-o", "--out", help="写入 UTF-8 文件路径（不设则 stdout）")
	args = ap.parse_args()

	lang = json.loads(LANG.read_text(encoding="utf-8"))

	buf: list[str] = []

	def zh(cid: str) -> str:
		return lang.get("entity.chickens." + cid + ".name", cid)

	def ilab(fid: str) -> str:
		return CN_ITEM.get(fid, fid)

	rows = []
	for p in sorted(CHICKENS.glob("*.json")):
		d = json.loads(p.read_text(encoding="utf-8"))
		cid = p.stem
		lay = d.get("lay_item", "")
		cnt = d.get("lay_count", 1)
		lnbt = (d.get("lay_nbt") or "").strip()
		drop = d.get("drop_item")
		dcnt = d.get("drop_count", 1)
		dnbt = (d.get("drop_nbt") or "").strip()
		rows.append((zh(cid), cid, lay, cnt, lnbt, drop, dcnt, dnbt))

	rows.sort(key=lambda x: (x[0], x[1]))
	def emit(s: str) -> None:
		buf.append(s)

	emit("| 中文名（内部 ID） | 周期性产出（产蛋，`lay_item`） | 备注 |")
	emit("| --- | --- | --- |")
	for name, cid, lay, cnt, lnbt, drop, dcnt, dnbt in rows:
		col1 = f"{name} (`{cid}`)"
		col2 = f"{ilab(lay)}（`{lay}`）×{cnt}"
		notes = []
		if lnbt:
			notes.append(f"产蛋 NBT：`{lnbt}`")
		if drop:
			s = f"数据条目另有 `drop_item`：{ilab(drop)}（`{drop}`）×{dcnt}"
			if dnbt:
				s += f"，NBT `{dnbt}`"
			s += "（是否与击杀掉落一致取决于实体战利表是否使用该字段）"
			notes.append(s)
		col3 = "；".join(notes) if notes else "—"
		emit(f"| {col1} | {col2} | {col3} |")
	emit("")
	emit(f"共 **{len(rows)}** 种。")
	emit("")
	emit(
		"说明：表中为数据包定义的 **`lay_item` / `lay_count` / `lay_nbt`**（模组鸡的周期性「产蛋」产出）；"
		" **`gain`** 较高时同一次产蛋可能尝试 **多份同样的 `lay_item`**。"
	)
	emit(
		"另：`drop_item` 仅少量 JSON 有写；是否与击杀掉落一致取决于实体战利表是否使用该字段。"
	)

	text = "\n".join(buf) + "\n"
	if args.out:
		Path(args.out).parent.mkdir(parents=True, exist_ok=True)
		Path(args.out).write_text(text, encoding="utf-8")
	else:
		sys.stdout.write(text)
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
