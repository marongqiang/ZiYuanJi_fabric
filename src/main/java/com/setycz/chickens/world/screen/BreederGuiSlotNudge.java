package com.setycz.chickens.world.screen;

/**
 * 繁殖机机箱区（第一行鸡+蛋、种子行、进度条）。<strong>玩家物品栏</strong>不改，仍在 {@link BreederScreenHandler} 里用原版坐标。
 * <p>
 * 分两层：① {@link #CHICKEN_NUDGE_DX} 等与鸡舍同类「格内图标」微调；② {@link #ROW1_LAYOUT_DX} 整行布局；种子行再叠 {@link #SEED_ROW_EXTRA_DX} / {@link #SEED_ROW_EXTRA_DY}。
 * 繁殖进度：贴图 {@code breeder.png} 自带空心爱心；红色进度在客户端用与爱心内部一致的掩模像素绘制（非整块矩形），掩模相对 {@link #breedingHeartCenterX()} / {@link #breedingHeartCenterY(int)} 居中，可用 {@link #BREEDER_HEART_FILL_OFFSET_DX} / {@link #BREEDER_HEART_FILL_OFFSET_DY} 微调。
 * 以后请相对<strong>当前</strong>界面说上下左右几像素，只改本类常量即可。
 */
public final class BreederGuiSlotNudge {
	private BreederGuiSlotNudge() {}

	/** 与鸡舍 / {@link BreederScreenHandler} 第一行「两格 + 箭头」中箭头宽度一致（22 像素） */
	private static final int BREEDER_ROW1_ARROW = 22;

	private static int breederRow1GridX(int slotIndex) {
		int pair = slotIndex / 2;
		int inPair = slotIndex % 2;
		return HenhouseScreenHandler.CHICKEN_GRID_X + pair * (36 + BREEDER_ROW1_ARROW) + inPair * 18;
	}

	/** 与鸡舍定稿一致的格内微调（相对格线公式） */
	public static final int CHICKEN_NUDGE_DX = -6;
	public static final int CHICKEN_NUDGE_DY = -2;

	/** 刷怪蛋槽 4–5 */
	public static final int OUTPUT_NUDGE_DX = -2;
	public static final int OUTPUT_NUDGE_DY = -3;

	/** 第一行（收容鸡 + 蛋）与进度条：在 ① 基础上整体左 10、下 2 */
	public static final int ROW1_LAYOUT_DX = -10;
	public static final int ROW1_LAYOUT_DY = 2;

	/** 小麦种子行：在 machine+ROW1 基础上微调 */
	public static final int SEED_ROW_EXTRA_DX = -3;

	public static final int SEED_ROW_EXTRA_DY = 10;

	/** 槽 0–1 收容鸡：在通用鸡坐标上再平移 */
	public static final int CHICKEN_FIRST_PAIR_EXTRA_DX = 4;
	public static final int CHICKEN_FIRST_PAIR_EXTRA_DY = -1;

	/** 槽 2–3 收容鸡：仅垂直微调（相对当前再上 1） */
	public static final int CHICKEN_SECOND_PAIR_EXTRA_DY = -1;

	/** 进度条在 machine 基准上再微调（一般保持 0） */
	public static final int PROGRESS_BAR_DX = 0;
	public static final int PROGRESS_BAR_DY = 0;

	public static int chickenSx(int gridX) {
		return ChickensGuiSlotNudge.sx(gridX) + CHICKEN_NUDGE_DX + ROW1_LAYOUT_DX;
	}

	public static int chickenSy(int gridY) {
		return ChickensGuiSlotNudge.sy(gridY) + CHICKEN_NUDGE_DY + ROW1_LAYOUT_DY;
	}

	/** 槽 0–1 用此 sx（前两格额外平移） */
	public static int chickenSxForSlotIndex(int chickenSlotIndex, int gridX) {
		int x = chickenSx(gridX);
		if (chickenSlotIndex <= 1) {
			x += CHICKEN_FIRST_PAIR_EXTRA_DX;
		}
		return x;
	}

	/** 槽 0–1、2–3 用此 sy（第二对仅叠 {@link #CHICKEN_SECOND_PAIR_EXTRA_DY}） */
	public static int chickenSyForSlotIndex(int chickenSlotIndex, int gridY) {
		int y = chickenSy(gridY);
		if (chickenSlotIndex <= 1) {
			y += CHICKEN_FIRST_PAIR_EXTRA_DY;
		} else if (chickenSlotIndex <= 3) {
			y += CHICKEN_SECOND_PAIR_EXTRA_DY;
		}
		return y;
	}

	public static int outputSx(int gridX) {
		return ChickensGuiSlotNudge.sx(gridX) + OUTPUT_NUDGE_DX + ROW1_LAYOUT_DX;
	}

	public static int outputSy(int gridY) {
		return ChickensGuiSlotNudge.sy(gridY) + OUTPUT_NUDGE_DY + ROW1_LAYOUT_DY;
	}

	public static int seedSx(int gridX) {
		return ChickensGuiSlotNudge.machineSx(gridX) + ROW1_LAYOUT_DX + SEED_ROW_EXTRA_DX;
	}

	public static int seedSy(int gridY) {
		return ChickensGuiSlotNudge.machineSy(gridY) + ROW1_LAYOUT_DY + SEED_ROW_EXTRA_DY;
	}

	public static int progressBarSx(int gridX) {
		return ChickensGuiSlotNudge.machineSx(gridX) + ROW1_LAYOUT_DX + PROGRESS_BAR_DX;
	}

	public static int progressBarSy(int gridY) {
		return ChickensGuiSlotNudge.machineSy(gridY) + ROW1_LAYOUT_DY + PROGRESS_BAR_DY;
	}

	/** 第一、二对收容鸡之间箭头区域水平中心（相对繁殖箱 GUI 纹理左上） */
	public static int breedingHeartCenterX() {
		int gx1 = breederRow1GridX(1);
		int gx2 = breederRow1GridX(2);
		int rightPair0 = chickenSxForSlotIndex(1, gx1) + 18;
		int leftPair1 = chickenSxForSlotIndex(2, gx2);
		return (rightPair0 + leftPair1) / 2;
	}

	/** 与收容鸡第一行垂直对齐（相对 GUI 左上） */
	public static int breedingHeartCenterY(int row1GridY) {
		int y0 = chickenSyForSlotIndex(0, row1GridY);
		int y2 = chickenSyForSlotIndex(2, row1GridY);
		return (y0 + y2) / 2 + 9;
	}

	/** 填充掩模相对几何中心的微调（相对当前：左为负 dx，下为正 dy） */
	public static final int BREEDER_HEART_FILL_OFFSET_DX = -2;
	public static final int BREEDER_HEART_FILL_OFFSET_DY = -1;
}
