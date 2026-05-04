package com.setycz.chickens.world.screen;

/**
 * <strong>鸡舍</strong>机箱内槽位（见 {@link HenhouseScreenHandler}）；繁殖机请用 {@link BreederGuiSlotNudge}。**玩家物品栏**保持原版坐标。
 * <p>
 * <strong>定稿基准：</strong>下方常量即当前游戏内鸡舍界面（相对格线公式坐标的像素偏移）。以后若再调 UI，请直接说
 * 「相对<strong>现在</strong>界面」上 / 下 / 左 / 右几像素，在此类里对相应常量做加减即可，不必再记历史推导过程。
 * <p>
 * 当前数值：鸡 <strong>左 6、上 2</strong>；产出 <strong>右 5、上 3</strong>（与 {@link #CHICKEN_NUDGE_DX} 等一致）。
 * <p>
 * 原版在 18×18 格内画 16×16 物品。{@link #machineSx}/{@link #machineSy} 为洞心偏移（{@link #MACHINE_CELL_CENTER_DX}）；鸡格/产出格在格点上直接加 {@link #CHICKEN_NUDGE_DX} 等，不叠洞心。
 * 修改后须重新 {@code ./gradlew build} 换 jar 或 {@code runClient}。
 */
public final class ChickensGuiSlotNudge {
	private ChickensGuiSlotNudge() {}

	public static final int SLOT_DX = 0;
	public static final int SLOT_DY = 0;

	/** 机箱槽：相对网格公式坐标再平移，使图标更接近洞中心（不对就改 ±1） */
	public static final int MACHINE_CELL_CENTER_DX = 1;
	public static final int MACHINE_CELL_CENTER_DY = 1;

	public static int sx(int x) {
		return x + SLOT_DX;
	}

	public static int sy(int y) {
		return y + SLOT_DY;
	}

	public static int machineSx(int gridX) {
		return gridX + SLOT_DX + MACHINE_CELL_CENTER_DX;
	}

	public static int machineSy(int gridY) {
		return gridY + SLOT_DY + MACHINE_CELL_CENTER_DY;
	}

	/** 鸡槽：累计左 6、上 2 */
	public static final int CHICKEN_NUDGE_DX = -6;
	public static final int CHICKEN_NUDGE_DY = -2;

	/** 产出槽：4,-4 再右 1、下 1（y+1）→ 右 5、上 3 */
	public static final int OUTPUT_NUDGE_DX = 5;
	public static final int OUTPUT_NUDGE_DY = -3;

	public static int chickenSx(int gridX) {
		return sx(gridX) + CHICKEN_NUDGE_DX;
	}

	public static int chickenSy(int gridY) {
		return sy(gridY) + CHICKEN_NUDGE_DY;
	}

	public static int outputSx(int gridX) {
		return sx(gridX) + OUTPUT_NUDGE_DX;
	}

	public static int outputSy(int gridY) {
		return sy(gridY) + OUTPUT_NUDGE_DY;
	}
}
