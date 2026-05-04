#!/usr/bin/env python3
"""
将鸡舍 GUI 底板恢复为与 breeder 相同（便于在外部改图后再覆盖）。

运行：python scripts/rebuild_henhouse_gui_png.py
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/resources/assets/chickens/textures/gui/breeder.png"
OUT = ROOT / "src/main/resources/assets/chickens/textures/gui/henhouse.png"


def main() -> None:
	if not SRC.is_file():
		raise FileNotFoundError(SRC)
	shutil.copy2(SRC, OUT)
	print(f"Restored {OUT.relative_to(ROOT)} <= {SRC.name}")


if __name__ == "__main__":
	main()
