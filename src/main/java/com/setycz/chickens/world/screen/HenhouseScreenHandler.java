package com.setycz.chickens.world.screen;

import com.setycz.chickens.registry.ModScreenHandlers;
import com.setycz.chickens.world.blockentity.HenhouseBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class HenhouseScreenHandler extends ScreenHandler {
	/** 与 {@link com.setycz.chickens.client.screen.HenhouseScreen}、{@code rebuild_henhouse_gui_png.py} 一致 */
	/** 须低于 breeder 玩家在纹理中的分区：底行机箱槽止于 y=GRID_TOP+53，留出 y≥72 原图分隔带 */
	public static final int GRID_TOP = 18;
	public static final int CHICKEN_GRID_X = 26;
	private static final int GRID_COLUMNS = 3;
	private static final int ARROW_GAP = 22;
	public static final int OUTPUT_GRID_X = CHICKEN_GRID_X + GRID_COLUMNS * 18 + ARROW_GAP;

	/**
	 * 玩家区与原版 176×166 单格容器（如潜影盒界面）一致：左列 x=8，主背包首行 y=84，快捷栏 y=142。
	 * 勿对玩家槽使用 {@link ChickensGuiSlotNudge}，避免与「物品栏」格线、悬停框、原版绘制错位。
	 */
	public static final int PLAYER_MAIN_INV_Y = 84;
	public static final int PLAYER_HOTBAR_Y = 142;

	private final Inventory inventory;

	public HenhouseScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ModScreenHandlers.HENHOUSE, syncId);
		this.inventory = inventory;

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int slotIndex = HenhouseBlockEntity.CHICKEN_FIRST + row * 3 + col;
				this.addSlot(new ChickenSlot(inventory, slotIndex,
						ChickensGuiSlotNudge.chickenSx(CHICKEN_GRID_X + col * 18),
						ChickensGuiSlotNudge.chickenSy(GRID_TOP + row * 18)));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int slotIndex = HenhouseBlockEntity.OUTPUT_FIRST + row * 3 + col;
				this.addSlot(new OutputSlot(inventory, slotIndex,
						ChickensGuiSlotNudge.outputSx(OUTPUT_GRID_X + col * 18),
						ChickensGuiSlotNudge.outputSy(GRID_TOP + row * 18)));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_MAIN_INV_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_HOTBAR_Y));
		}

		if (inventory instanceof HenhouseBlockEntity henhouse) {
			henhouse.addGuiSyncViewer();
		}
	}

	public HenhouseScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(HenhouseBlockEntity.TOTAL_SLOTS));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		if (inventory instanceof HenhouseBlockEntity henhouse) {
			henhouse.removeGuiSyncViewer();
		}
		super.onClosed(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

		ItemStack original = slot.getStack();
		newStack = original.copy();

		int henhouseSlots = HenhouseBlockEntity.TOTAL_SLOTS;
		if (slotIndex < henhouseSlots) {
			if (!this.insertItem(original, henhouseSlots, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (HenhouseBlockEntity.acceptsChickenStack(original)) {
				if (!this.insertItem(original, HenhouseBlockEntity.CHICKEN_FIRST, HenhouseBlockEntity.OUTPUT_FIRST, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
		}

		if (original.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		return newStack;
	}

	private static final class ChickenSlot extends Slot {
		ChickenSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return HenhouseBlockEntity.acceptsChickenStack(stack);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}
	}

	private static final class OutputSlot extends Slot {
		OutputSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false;
		}
	}
}
