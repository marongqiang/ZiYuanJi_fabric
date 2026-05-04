package com.setycz.chickens.world.blockentity;

import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.world.ChickenAttributeTicks;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.inventory.ImplementedInventory;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import com.setycz.chickens.world.screen.HenhouseScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 左侧 3×3（{@value #CHICKEN_SLOT_COUNT}）格收容鸡 → 右侧 3×3 产出区；GUI 中为两格网加中央箭头样式。
 * 顶/侧面漏斗可放入收容鸡，底面漏斗只抽取产出。产蛋间隔由栏内 {@code layTicks} 与收容数据 Gain 决定（计时初始化不读收容里过期的 EggTime）；仅当有玩家打开本鸡舍界面时，才在每 tick 把 {@code layTicks} 写回收容 NBT 的 {@code EggTime}（减轻大量鸡舍时的写包）；从收容槽取出物品或区块落盘前仍会刷写一次，避免物品 NBT 与栏内计时脱节。收容数据内交配冷却每 tick 递减。
 */
public final class HenhouseBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {
	public static final int CHICKEN_FIRST = 0;
	public static final int CHICKEN_SLOT_COUNT = 9;
	public static final int OUTPUT_FIRST = CHICKEN_FIRST + CHICKEN_SLOT_COUNT;
	public static final int OUTPUT_SLOT_COUNT = 9;
	public static final int TOTAL_SLOTS = OUTPUT_FIRST + OUTPUT_SLOT_COUNT;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
	private final int[] layTicks = new int[CHICKEN_SLOT_COUNT];
	/** 有玩家打开本鸡舍 GUI 时 &gt;0；仅此时把栏内产蛋进度写回物品 NBT，减轻大量鸡舍时的同步开销 */
	private int guiSyncViewers;

	public HenhouseBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HENHOUSE, pos, state);
	}

	public void addGuiSyncViewer() {
		guiSyncViewers++;
	}

	public void removeGuiSyncViewer() {
		if (guiSyncViewers > 0) {
			guiSyncViewers--;
		}
	}

	public static void tickServer(World world, BlockPos pos, BlockState state, HenhouseBlockEntity be) {
		if (world.isClient) return;
		Random random = world.random;
		boolean dirty = false;
		for (int i = 0; i < CHICKEN_SLOT_COUNT; i++) {
			if (be.tickChickenSlot(random, i)) {
				dirty = true;
			}
		}
		if (dirty) {
			be.markDirty();
		}
	}

	private boolean tickChickenSlot(Random random, int idx) {
		ItemStack stack = items.get(CHICKEN_FIRST + idx);
		if (stack.isEmpty()) {
			if (layTicks[idx] != 0) {
				layTicks[idx] = 0;
				return true;
			}
			return false;
		}
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || cap.isEmpty() || isBabyCapture(cap)) {
			return false;
		}
		boolean changed = false;
		if (tickBreedCooldownInStack(stack, cap)) {
			changed = true;
		}
		// 仅用栏内 layTicks + Gain 计时；不用收容 NBT 里的 EggTime（否则收进/挪动槽位时会把很久以前的 EggTime 又读一遍，间隔被拉得极长，像不产蛋）
		if (layTicks[idx] <= 0) {
			layTicks[idx] = computeLayCooldown(random, cap);
			changed = true;
		}
		layTicks[idx]--;
		if (layTicks[idx] <= 0) {
			ItemStack leftover = layOutputsForCapture(cap);
			if (!leftover.isEmpty()) {
				// 产出区满或无法合并：不消耗这次产蛋；约每秒再试一次，避免每 tick 扫槽
				layTicks[idx] = 20;
			} else {
				layTicks[idx] = computeLayCooldown(random, cap);
			}
			changed = true;
		}
		if (guiSyncViewers > 0 && syncEggTimeToCaptureForSlot(idx, stack)) {
			changed = true;
		}
		return changed;
	}

	private void flushEggTimeFromLayTicksForChickenSlot(int slot) {
		if (slot < CHICKEN_FIRST || slot >= OUTPUT_FIRST) {
			return;
		}
		int idx = slot - CHICKEN_FIRST;
		syncEggTimeToCaptureForSlot(idx, items.get(CHICKEN_FIRST + idx));
	}

	private void flushEggTimeFromLayTicksForAllChickenSlots() {
		for (int i = 0; i < CHICKEN_SLOT_COUNT; i++) {
			syncEggTimeToCaptureForSlot(i, items.get(CHICKEN_FIRST + i));
		}
	}

	/** 将 {@link #layTicks} 剩余 tick 写入收容 {@code EggTime}；有变化才写回物品。 */
	private boolean syncEggTimeToCaptureForSlot(int idx, ItemStack stack) {
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || cap.isEmpty() || isBabyCapture(cap)) {
			return false;
		}
		int remain = Math.max(0, layTicks[idx]);
		int prev = cap.contains("EggTime") ? cap.getInt("EggTime") : -1;
		if (prev == remain) {
			return false;
		}
		cap.putInt("EggTime", remain);
		ChickenSpawnEggItem.writeCapture(stack, cap);
		return true;
	}

	/** @return 未能放入产出区的部分；空表示全部并入 */
	private ItemStack layOutputsForCapture(NbtCompound cap) {
		net.minecraft.util.Identifier rid = readTypeId(cap);
		if (rid == null) return ItemStack.EMPTY;
		ChickenType type = ChickenTypes.getOrDefault(rid);
		ItemStack product = type.layItem().copy();
		if (product.isEmpty()) return ItemStack.EMPTY;

		return insertIntoOutputs(product.copy());
	}

	/** 收容物品上 {@code BreedCooldown} 每世界 tick 减 1 */
	private static boolean tickBreedCooldownInStack(ItemStack stack, NbtCompound cap) {
		if (!cap.contains("BreedCooldown")) {
			return false;
		}
		int cd = cap.getInt("BreedCooldown");
		if (cd <= 0) {
			return false;
		}
		cap.putInt("BreedCooldown", cd - 1);
		ChickenSpawnEggItem.writeCapture(stack, cap);
		return true;
	}

	private static @Nullable net.minecraft.util.Identifier readTypeId(NbtCompound cap) {
		if (!cap.contains("Type")) return null;
		try {
			return new net.minecraft.util.Identifier(cap.getString("Type"));
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean isBabyCapture(NbtCompound cap) {
		return cap.contains("Age") && cap.getInt("Age") < 0;
	}

	private static int clampStat(int v) {
		if (v < 1) return 1;
		if (v > 10) return 10;
		return v;
	}

	private static int computeLayCooldown(Random random, NbtCompound cap) {
		int gain = clampStat(cap.contains("Gain") ? cap.getInt("Gain") : 1);
		return ChickenAttributeTicks.nextLayCooldownTicks(random, gain);
	}

	/** 附近实体鸡产蛋时仍可优先送入本鸡舍产出区 */
	public static ItemStack pushItemStack(ItemStack input, World world, Vec3d pos, int radius) {
		if (input.isEmpty()) return ItemStack.EMPTY;
		List<HenhouseBlockEntity> henhouses = findHenhouses(world, pos, radius);
		ItemStack rest = input;
		for (HenhouseBlockEntity henhouse : henhouses) {
			rest = henhouse.insertIntoOutputs(rest);
			if (rest.isEmpty()) break;
		}
		return rest;
	}

	private static List<HenhouseBlockEntity> findHenhouses(World world, Vec3d pos, int radius) {
		BlockPos center = BlockPos.ofFloored(pos);
		BlockPos min = center.add(-radius, -radius, -radius);
		BlockPos max = center.add(radius, radius, radius);
		List<HenhouseBlockEntity> result = new ArrayList<>();
		BlockPos.iterate(min, max).forEach(p -> {
			if (world.getBlockEntity(p) instanceof HenhouseBlockEntity be) {
				result.add(be);
			}
		});
		result.sort(Comparator.comparingDouble(be -> be.getPos().getSquaredDistance(pos.x, pos.y, pos.z)));
		return result;
	}

	private ItemStack insertIntoOutputs(ItemStack stack) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		ItemStack remaining = stack.copy();
		// 先并入已有未满堆叠，再找空槽；否则先遇到空槽会占新格，后面同种物品无法合并（漏斗/产蛋同理）
		for (int s = OUTPUT_FIRST; s < TOTAL_SLOTS; s++) {
			ItemStack slot = items.get(s);
			if (slot.isEmpty()) continue;
			if (ItemStack.canCombine(slot, remaining)) {
				int room = slot.getMaxCount() - slot.getCount();
				if (room <= 0) continue;
				int move = Math.min(room, remaining.getCount());
				ItemStack nw = slot.copy();
				nw.increment(move);
				remaining.decrement(move);
				items.set(s, nw);
				markDirty();
				if (remaining.isEmpty()) return ItemStack.EMPTY;
			}
		}
		for (int s = OUTPUT_FIRST; s < TOTAL_SLOTS; s++) {
			ItemStack slot = items.get(s);
			if (!slot.isEmpty()) continue;
			int put = Math.min(remaining.getMaxCount(), remaining.getCount());
			ItemStack placed = remaining.split(put);
			items.set(s, placed);
			markDirty();
			if (remaining.isEmpty()) return ItemStack.EMPTY;
		}
		return remaining;
	}

	public static boolean acceptsChickenStack(ItemStack stack) {
		if (!stack.isOf(ModItems.CAPTURED_CHICKEN)) return false;
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		return cap != null && !cap.isEmpty();
	}

	@Override
	public DefaultedList<ItemStack> getItems() {
		return items;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		getItems().set(slot, stack);
		if (!stack.isEmpty() && stack.getCount() > getMaxCountPerStack()) {
			stack.setCount(getMaxCountPerStack());
		}
		if (slot >= CHICKEN_FIRST && slot < OUTPUT_FIRST) {
			layTicks[slot - CHICKEN_FIRST] = 0;
		}
		markDirty();
	}

	@Override
	public ItemStack removeStack(int slot, int count) {
		if (guiSyncViewers <= 0 && slot >= CHICKEN_FIRST && slot < OUTPUT_FIRST) {
			flushEggTimeFromLayTicksForChickenSlot(slot);
		}
		ItemStack result = Inventories.splitStack(getItems(), slot, count);
		if (!result.isEmpty()) {
			markDirty();
		}
		return result;
	}

	@Override
	public ItemStack removeStack(int slot) {
		if (guiSyncViewers <= 0 && slot >= CHICKEN_FIRST && slot < OUTPUT_FIRST) {
			flushEggTimeFromLayTicksForChickenSlot(slot);
		}
		ItemStack removed = Inventories.removeStack(getItems(), slot);
		if (!removed.isEmpty()) {
			markDirty();
		}
		return removed;
	}

	@Override
	public void markDirty() {
		super.markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), 3);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		if (guiSyncViewers <= 0) {
			flushEggTimeFromLayTicksForAllChickenSlots();
		}
		writeInventory(nbt);
		nbt.putIntArray("HenhouseLayTicks", layTicks);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		readInventory(nbt);
		int[] arr = nbt.getIntArray("HenhouseLayTicks");
		for (int i = 0; i < CHICKEN_SLOT_COUNT; i++) {
			layTicks[i] = i < arr.length ? arr[i] : 0;
		}
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("block.chickens.henhouse");
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		return new HenhouseScreenHandler(syncId, playerInventory, this);
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		if (slot >= CHICKEN_FIRST && slot < OUTPUT_FIRST) {
			return acceptsChickenStack(stack);
		}
		return false;
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		int[] chickens = new int[CHICKEN_SLOT_COUNT];
		for (int i = 0; i < CHICKEN_SLOT_COUNT; i++) chickens[i] = CHICKEN_FIRST + i;
		int[] outs = new int[OUTPUT_SLOT_COUNT];
		for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) outs[i] = OUTPUT_FIRST + i;

		return switch (side) {
			case DOWN -> outs;
			case UP -> chickens;
			default -> chickens;
		};
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
		if (slot >= CHICKEN_FIRST && slot < OUTPUT_FIRST) {
			return acceptsChickenStack(stack);
		}
		return false;
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		return dir == Direction.DOWN && slot >= OUTPUT_FIRST && slot < TOTAL_SLOTS;
	}

}
