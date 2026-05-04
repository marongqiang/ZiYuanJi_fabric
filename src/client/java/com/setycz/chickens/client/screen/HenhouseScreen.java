package com.setycz.chickens.client.screen;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.ChickenUiContext;
import com.setycz.chickens.world.screen.HenhouseScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HenhouseScreen extends HandledScreen<HenhouseScreenHandler> {
	private static final Identifier BG = ChickensFabricMod.id("textures/gui/henhouse.png");
	private static final int TEX_H = 166;
	private static final int EXTRA_H = 22;
	/** breeder.png 底板 y=164 一线的深灰延伸，避免再接 #C6 亮条看起来像「整块白条」 */
	private static final int EXTRA_PANEL_FILL = 0xFF555555;

	public HenhouseScreen(HenhouseScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = TEX_H + EXTRA_H;
	}

	@Override
	protected void init() {
		super.init();
		// 原版惯例：可视底板高 166 时 titleY = 72。若误用加长后的 backgroundHeight(188−94→94)，会画进背包首行槽内。
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = TEX_H - 94;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (width - backgroundWidth) / 2;
		int y = (height - backgroundHeight) / 2;
		// breeder/henhouse 资源为 256×256（仅左上 176×166 可视）；写明尺寸避免别处误导出成 176 宽时被 256 取样拉花
		context.drawTexture(BG, x, y, 0, 0, backgroundWidth, TEX_H, 256, 256);
		context.fill(x, y + TEX_H, x + backgroundWidth, y + backgroundHeight, EXTRA_PANEL_FILL);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ChickenUiContext.setRenderingCapturedChickenTooltips(true);
		try {
			renderBackground(context);
			super.render(context, mouseX, mouseY, delta);
			drawMouseoverTooltip(context, mouseX, mouseY);
		} finally {
			ChickenUiContext.setRenderingCapturedChickenTooltips(false);
		}
	}
}
