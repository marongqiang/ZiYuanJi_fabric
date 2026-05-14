package com.setycz.chickens.world.screen;

import com.setycz.chickens.registry.ModScreenHandlers;
import com.setycz.chickens.world.blockentity.BreederBlockEntity;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * GUI：第一行 6 格为「两格 + 箭头 + 两格 + 箭头 + 两格」（网格公式与鸡舍 {@link HenhouseScreenHandler} 一致；像素用 {@link BreederGuiSlotNudge}）。
 * 第二行 5 格小麦种子；第一行 0–3 收容鸡、4–5 刷怪蛋。下方玩家物品栏为原版坐标，不经 {@link BreederGuiSlotNudge}。
 */
public final class BreederScreenHandler extends ScreenHandler {
	public static final int BREEDER_BACKGROUND_WIDTH = 176;
	/** 与鸡舍第一列一致，保证与 breeder.png 上槽位对齐 */
	private static final int ROW1_Y = HenhouseScreenHandler.GRID_TOP;
	private static final int ROW1_PAIR_X0 = HenhouseScreenHandler.CHICKEN_GRID_X;
	private static final int ROW1_ARROW = 22;
	/** 第一行槽底 + 与贴图第二行之间的间距 */
	private static final int SEED_ROW_Y = ROW1_Y + 22;
	private static final int BREEDER_SLOTS = BreederBlockEntity.INVENTORY_SIZE;
	private final Inventory breederInventory;
	private final PropertyDelegate propertyDelegate;

	public BreederScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(BREEDER_SLOTS), new ArrayPropertyDelegate(1));
	}

	public BreederScreenHandler(int syncId, PlayerInventory playerInventory, BreederBlockEntity breeder) {
		this(syncId, playerInventory, breeder, breeder.propertyDelegate);
	}

	private BreederScreenHandler(int syncId, PlayerInventory playerInventory, Inventory breederInventory, PropertyDelegate propertyDelegate) {
		super(ModScreenHandlers.BREEDER, syncId);
		this.breederInventory = breederInventory;
		this.propertyDelegate = propertyDelegate;
		addProperties(propertyDelegate);

		for (int i = 0; i < 6; i++) {
			int gx = row1SlotX(i);
			int x = i <= 3 ? BreederGuiSlotNudge.chickenSxForSlotIndex(i, gx) : BreederGuiSlotNudge.outputSx(gx);
			int y = i <= 3 ? BreederGuiSlotNudge.chickenSyForSlotIndex(i, ROW1_Y) : BreederGuiSlotNudge.outputSy(ROW1_Y);
			if (i <= 3) {
				this.addSlot(new CapturedChickenSlot(breederInventory, BreederBlockEntity.SLOT_CHICKEN_1 + i, x, y));
			} else {
				this.addSlot(new OutputSlot(breederInventory, BreederBlockEntity.SLOT_EGG_FIRST + (i - 4), x, y));
			}
		}
		for (int i = 0; i < BreederBlockEntity.SEED_SLOT_COUNT; i++) {
			this.addSlot(new SeedSlot(breederInventory, BreederBlockEntity.SLOT_SEEDS_FIRST + i,
					BreederGuiSlotNudge.seedSx(ROW1_PAIR_X0 + i * 18),
					BreederGuiSlotNudge.seedSy(SEED_ROW_Y)));
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
						8 + col * 18,
						HenhouseScreenHandler.PLAYER_MAIN_INV_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, HenhouseScreenHandler.PLAYER_HOTBAR_Y));
		}
	}

	public int getBreedingProgress() {
		return propertyDelegate.get(0);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return breederInventory.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

		ItemStack original = slot.getStack();
		newStack = original.copy();

		int playerStart = BREEDER_SLOTS;
		if (slotIndex < BREEDER_SLOTS) {
			if (!this.insertItem(original, playerStart, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (original.isIn(BreederBlockEntity.BREEDER_FUEL)) {
				if (!this.insertItem(original, BreederBlockEntity.SLOT_SEEDS_FIRST, BreederBlockEntity.SLOT_SEEDS_LAST + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (original.getItem() instanceof CapturedChickenItem && isValidCaptureForGui(original)) {
				if (!this.insertItem(original, BreederBlockEntity.SLOT_CHICKEN_1, BreederBlockEntity.SLOT_NEXT_LAST + 1, false)) {
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

	private static final class SeedSlot extends Slot {
		SeedSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return stack.isIn(BreederBlockEntity.BREEDER_FUEL);
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

	private static boolean isValidCaptureForGui(ItemStack stack) {
		var cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		return cap != null && !cap.isEmpty();
	}

	/** 鸡栏 0–3：可放入/取出有效收容鸡 */
	private static final class CapturedChickenSlot extends Slot {
		CapturedChickenSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return stack.getItem() instanceof CapturedChickenItem && isValidCaptureForGui(stack);
		}
	}

	/** 第一行第 i 格（0–5）的 x：两格一组，组间留箭头宽度 {@link HenhouseScreenHandler#ARROW_GAP}。 */
	private static int row1SlotX(int i) {
		int pair = i / 2;
		int inPair = i % 2;
		return ROW1_PAIR_X0 + pair * (36 + ROW1_ARROW) + inPair * 18;
	}
}
