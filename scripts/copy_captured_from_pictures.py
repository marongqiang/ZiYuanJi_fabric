# -*- coding: utf-8 -*-
"""Copy 修改后鸡图片 -> assets/.../captured/<id>.png. Run: python scripts/copy_captured_from_pictures.py"""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = Path(r"C:\Users\31305\Pictures\修改后鸡图片")
DST = ROOT / "src/main/resources/assets/chickens/textures/item/captured"
DATA = ROOT / "src/main/resources/data/chickens/chickens"

# 中文文件名 -> 鸡 id（与 json  basename 一致；海晶灯鸡仍为 prismarine）
MAPPING: dict[str, str] = {
	"下界之星.png": "nether_star.png",
	"下界疣鸡.png": "netherwart.png",
	"不死图腾.png": "totem.png",
	"丛林原木.png": "jungle_log.png",
	"书.png": "book.png",
	"云杉原木.png": "spruce_log.png",
	"品红染料鸡.png": "magenta.png",
	"圆石.png": "cobblestone.png",
	"岩浆膏.png": "magma.png",
	"平滑石.png": "smooth_stone.png",
	"恶魂之泪鸡.png": "ghast.png",
	"末影珍珠鸡.png": "ender.png",
	"棕色染料鸡.png": "brown.png",
	"樱花原木.png": "cherry_log.png",
	"橙色染料鸡.png": "orange.png",
	"橡木原木鸡.png": "log.png",
	"水鸡.png": "water.png",
	"沙子鸡.png": "sand.png",
	"海晶灯.png": "prismarine.png",
	"淡灰色染料.png": "silver_dye.png",
	"淡蓝色染料.png": "light_blue.png",
	"深板岩.png": "deepslate.png",
	"深色橡木原木.png": "dark_oak_log.png",
	"潜影贝.png": "shulker_shell.png",
	"火药鸡.png": "gunpowder.png",
	"灰色染料.png": "gray.png",
	"烈焰粉鸡.png": "blaze.png",
	"煤炭鸡.png": "coal.png",
	"燧石鸡.png": "flint.png",
	"玻璃鸡.png": "glass.png",
	"白桦原木.png": "birch_log.png",
	"白色燃料鸡.png": "white.png",
	"皮革鸡.png": "leather.png",
	"石头.png": "stone.png",
	"石英鸡.png": "quartz.png",
	"粉色染料鸡.png": "pink.png",
	"粘液球鸡.png": "slime.png",
	"紫色染料.png": "purple.png",
	"红树原木.png": "mangrove_log.png",
	"红石鸡.png": "redstone.png",
	"红色鸡.png": "red.png",
	"纸鸡.png": "paper.png",
	"经验鸡.png": "experience.png",
	"绯红菌柄.png": "crimson_stem.png",
	"绿宝石.png": "emerald.png",
	"绿色染料鸡.png": "green.png",
	"腐肉鸡.png": "rotten_flesh.png",
	"荧石鸡.png": "glowstone.png",
	"蓝色染料.png": "blue.png",
	"诡异菌柄.png": "warped_stem.png",
	"远古残骸.png": "netherite.png",
	"金合欢原木.png": "acacia_log.png",
	"钻石鸡.png": "diamond.png",
	"铁鸡.png": "iron.png",
	"雪球鸡.png": "snowball.png",
	"线鸡.png": "string.png",
	"青色染料鸡.png": "cyan.png",
	"青金石.png": "lapis.png",
	"鞘翅.png": "elytra.png",
	"骨粉.png": "bone.png",
	"黄绿鸡.png": "lime.png",
	"黄色鸡.png": "yellow.png",
	"黄金鸡.png": "gold.png",
	"黏土鸡.png": "clay.png",
	"黑曜石鸡.png": "obsidian.png",
	"黑色染料鸡.png": "black.png",
}

SKIP = {"原始图.png"}


def main() -> None:
	if not SRC.is_dir():
		raise SystemExit(f"Missing source folder: {SRC}")
	DST.mkdir(parents=True, exist_ok=True)
	copied = 0
	for cn, out in MAPPING.items():
		s = SRC / cn
		if not s.is_file():
			continue
		shutil.copy2(s, DST / out)
		copied += 1
	print(f"Copied {copied} files -> {DST}")

	ids = sorted(p.stem for p in DATA.glob("*.json"))
	pngs = {p.stem for p in DST.glob("*.png")}
	missing = [i for i in ids if i not in pngs]
	print(f"Chicken types without captured/*.png: {len(missing)}")
	for m in missing:
		print(m)


if __name__ == "__main__":
	main()
