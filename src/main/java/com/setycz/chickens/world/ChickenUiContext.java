package com.setycz.chickens.world;

/**
 * 客户端在绘制鸡舍/繁殖箱 {@link net.minecraft.client.gui.screen.ingame.HandledScreen} 时置位，
 * 用于收容鸡物品条与 tooltip 仅在「当前 GUI 帧」使用详细冷却展示，避免其它界面误用。
 */
public final class ChickenUiContext {
	private static final ThreadLocal<Boolean> RENDERING_CAPTURED_CHICKEN_TOOLTIPS = ThreadLocal.withInitial(() -> false);

	private ChickenUiContext() {}

	public static void setRenderingCapturedChickenTooltips(boolean rendering) {
		RENDERING_CAPTURED_CHICKEN_TOOLTIPS.set(rendering);
	}

	public static boolean isRenderingCapturedChickenTooltips() {
		return Boolean.TRUE.equals(RENDERING_CAPTURED_CHICKEN_TOOLTIPS.get());
	}
}
