package com.setycz.chickens.client.screen;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.ChickenUiContext;
import com.setycz.chickens.world.blockentity.BreederBlockEntity;
import com.setycz.chickens.world.screen.BreederScreenHandler;
import com.setycz.chickens.world.screen.BreederGuiSlotNudge;
import com.setycz.chickens.world.screen.HenhouseScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BreederScreen extends HandledScreen<BreederScreenHandler> {
	private static final Identifier BG = ChickensFabricMod.id("textures/gui/breeder.png");
	private static final int TEX_SRC_SIZE = 256;
	private static final int TEX_H = 166;

	/** 仅在掩模为 {@code F} 的像素上叠色；空心轮廓由 breeder.png 提供 */
	private static final int HEART_PROGRESS_FILL = 0xD0E53935;

	/**
	 * 爱心内部可填充区域（每行等宽 11）；{@code F} 处按像素自下→上、同行左→右依次随进度点亮，避免「整行一块」偏一侧。
	 */
	private static final String[] HEART_FILL_MASK = {
		"  FFF   FFF  ",
		" FFFFF FFFFF " ,
		" FFFFFFFFFFF " ,
		" FFFFFFFFFFF " ,
		" FFFFFFFFFFF " ,
		"  FFFFFFFFF  ",
		"   FFFFFFF   ",
		"    FFFFF    ",
		"     FFF     ",
		"      F      ",
	};

	private static final List<int[]> HEART_FILL_ORDER = buildHeartFillOrder();

	private static List<int[]> buildHeartFillOrder() {
		int rows = HEART_FILL_MASK.length;
		int cols = HEART_FILL_MASK[0].length();
		List<int[]> list = new ArrayList<>();
		for (int py = 0; py < rows; py++) {
			String row = HEART_FILL_MASK[py];
			for (int px = 0; px < cols && px < row.length(); px++) {
				if (row.charAt(px) == 'F') {
					list.add(new int[] { px, py });
				}
			}
		}
		list.sort(Comparator.<int[]>comparingInt(a -> -a[1]).thenComparingInt(a -> a[0]));
		return List.copyOf(list);
	}

	public BreederScreen(BreederScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = BreederScreenHandler.BREEDER_BACKGROUND_WIDTH;
		this.backgroundHeight = TEX_H;
	}

	@Override
	protected void init() {
		super.init();
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = TEX_H - 94;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (width - backgroundWidth) / 2;
		int y = (height - backgroundHeight) / 2;
		context.drawTexture(BG, x, y, 0, 0, backgroundWidth, backgroundHeight, TEX_SRC_SIZE, TEX_SRC_SIZE);
		int p = handler.getBreedingProgress();
		float ratio = Math.min(1f, p / (float) BreederBlockEntity.PROGRESS_MAX);
		drawBreedingHeartProgressFill(context, x, y, ratio);
	}

	private static void drawBreedingHeartProgressFill(DrawContext context, int guiLeft, int guiTop, float ratio) {
		if (ratio <= 0f) {
			return;
		}
		int row1 = HenhouseScreenHandler.GRID_TOP;
		int cx = BreederGuiSlotNudge.breedingHeartCenterX() + BreederGuiSlotNudge.BREEDER_HEART_FILL_OFFSET_DX;
		int cy = BreederGuiSlotNudge.breedingHeartCenterY(row1) + BreederGuiSlotNudge.BREEDER_HEART_FILL_OFFSET_DY;
		int rows = HEART_FILL_MASK.length;
		int cols = HEART_FILL_MASK[0].length();
		int ox = guiLeft + cx - cols / 2;
		int oy = guiTop + cy - rows / 2;
		int total = HEART_FILL_ORDER.size();
		if (total == 0) {
			return;
		}
		int n = Math.min(total, Math.max(0, (int) Math.ceil(total * ratio - 1e-9)));
		if (n == 0) {
			return;
		}
		for (int i = 0; i < n; i++) {
			int[] c = HEART_FILL_ORDER.get(i);
			int px = c[0];
			int py = c[1];
			context.fill(ox + px, oy + py, ox + px + 1, oy + py + 1, HEART_PROGRESS_FILL);
		}
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
