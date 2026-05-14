package com.setycz.chickens.world.blockentity;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.world.ChickenAttributeTicks;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.breeding.CapturedChickenBreeding;
import com.setycz.chickens.world.inventory.ImplementedInventory;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import com.setycz.chickens.world.screen.BreederScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI：第一行 0–3 为收容鸡（未满时选属性最高的一对繁殖，子代放入空位）；任一只在交配冷却内则不繁殖。交配冷却结束后若种子与空位满足则当 tick 即尝试繁殖（对齐原版「冷却好就能配」）。爱心进度仅反映该对两只的交配冷却完成度（取慢的一侧）。
 * 0–3 全满且非四只均为满属性（Growth/Gain/Strength 等级均为 10）时，选属性最低的两只转为刷怪蛋放入 4–5；四只均为满属性时不淘汰、不消耗繁殖计时。
 * 三项属性含义：Growth 幼体长成速度、Gain 下蛋间隔、Strength 繁殖交配冷却（与 {@link ChickenAttributeTicks} 一致）。繁殖时扣 2 份燃料。
 */
public final class BreederBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {
	public static final int SLOT_CHICKEN_1 = 0;
	public static final int SLOT_CHICKEN_2 = 1;
	/** 收容鸡行其余两格（与 0–1 规则相同） */
	public static final int SLOT_NEXT_FIRST = 2;
	public static final int SLOT_NEXT_LAST = 3;
	/** 淘汰产出：刷怪蛋 */
	public static final int SLOT_EGG_FIRST = 4;
	public static final int SLOT_EGG_LAST = 5;
	public static final int SLOT_SEEDS_FIRST = 6;
	public static final int SLOT_SEEDS_LAST = 10;
	public static final int SEED_SLOT_COUNT = SLOT_SEEDS_LAST - SLOT_SEEDS_FIRST + 1;
	public static final int INVENTORY_SIZE = SLOT_SEEDS_LAST + 1;


	public static final TagKey<Item> BREEDER_FUEL = TagKey.of(RegistryKeys.ITEM, ChickensFabricMod.id("breeder_fuel"));
	public static final int PROGRESS_MAX = 1000;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

	public final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return index == 0 ? getProgressScaled() : 0;
		}

		@Override
		public void set(int index, int value) {
			/* 客户端占位；进度由服务端每 tick 更新 */
		}

		@Override
		public int size() {
			return 1;
		}
	};

	public BreederBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BREEDER, pos, state);
	}

	public static void tickServer(World world, BlockPos pos, BlockState state, BreederBlockEntity be) {
		if (!(world instanceof ServerWorld serverWorld)) return;

		if (be.tickChickenRowBreedCooldowns()) {
			be.markDirty();
		}

		int before = be.propertyDelegate.get(0);

		// 四鸡满且能放入蛋：立刻淘汰
		if (be.chickenRowFull() && be.canCullAccumulate()) {
			be.tryCullToSpawnEggs();
		}

		if (be.tryMergeEggOutputSlots()) {
			be.markDirty();
		}

		if (be.canAccumulateTime()) {
			be.tryConsumeBreed(serverWorld);
		}

		if (before != be.propertyDelegate.get(0)) {
			be.markDirty();
		}
	}

	private int getProgressScaled() {
		BreedPair best = pickBestBreedPair(false);
		if (best == null) {
			return 0;
		}
		NbtCompound capA = ChickenSpawnEggItem.readCaptureCompound(getStack(best.slotA));
		NbtCompound capB = ChickenSpawnEggItem.readCaptureCompound(getStack(best.slotB));
		if (capA == null || capB == null) {
			return 0;
		}
		int remA = breedCooldownRemaining(capA);
		int remB = breedCooldownRemaining(capB);
		int totalA = ChickenAttributeTicks.breedCooldownTicks(captureStat10(capA, "Strength"));
		int totalB = ChickenAttributeTicks.breedCooldownTicks(captureStat10(capB, "Strength"));
		if (remA > 0 || remB > 0) {
			float pa = totalA <= 0 ? 1f : (totalA - remA) / (float) totalA;
			float pb = totalB <= 0 ? 1f : (totalB - remB) / (float) totalB;
			float merged = Math.min(pa, pb);
			return Math.min(PROGRESS_MAX, (int) (merged * PROGRESS_MAX));
		}
		// 双方交配冷却已结束：满格（繁殖由 tick 立即尝试，不再叠第二层计时条）
		return PROGRESS_MAX;
	}

	/**
	 * 「未满四鸡」且种子、空位、可繁殖配对（含冷却已清）齐备时每 tick 尝试 {@link #tryConsumeBreed}。
	 */
	private boolean canAccumulateTime() {
		if (chickenRowFull()) {
			return false;
		}
		return canBreedAccumulate();
	}

	/** 槽 4–5 为两格；原版也不会把两格同种刷怪蛋自动并一格，这里每 tick 尝试合并到左格。 */
	private boolean tryMergeEggOutputSlots() {
		ItemStack left = getStack(SLOT_EGG_FIRST);
		ItemStack right = getStack(SLOT_EGG_LAST);
		if (left.isEmpty() || right.isEmpty()) {
			return false;
		}
		if (!ItemStack.canCombine(left, right)) {
			return false;
		}
		int room = left.getMaxCount() - left.getCount();
		if (room <= 0) {
			return false;
		}
		int move = Math.min(room, right.getCount());
		if (move <= 0) {
			return false;
		}
		left.increment(move);
		right.decrement(move);
		setStack(SLOT_EGG_FIRST, left);
		setStack(SLOT_EGG_LAST, right.isEmpty() ? ItemStack.EMPTY : right);
		return true;
	}

	/** 槽 0–3 均为有效成体收容鸡。 */
	private boolean chickenRowFull() {
		return countValidChickensInRow() == 4;
	}

	private int countValidChickensInRow() {
		int n = 0;
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			if (isValidCaptured(getStack(s))) {
				n++;
			}
		}
		return n;
	}

	private boolean hasEmptyChickenSlot() {
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			if (getStack(s).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private boolean canCullAccumulate() {
		if (countValidChickensInRow() != 4) {
			return false;
		}
		if (allFourChickensAtFullStats()) {
			return false;
		}
		int[] low = pickLowestTwoSlots();
		if (low == null) {
			return false;
		}
		ItemStack e0 = makeSpawnEggFromCapture(ChickenSpawnEggItem.readCaptureCompound(getStack(low[0])));
		ItemStack e1 = makeSpawnEggFromCapture(ChickenSpawnEggItem.readCaptureCompound(getStack(low[1])));
		if (e0.isEmpty() || e1.isEmpty()) {
			return false;
		}
		return canFitEggStacks(e0, e1);
	}

	private boolean canBreedAccumulate() {
		if (countBreederFuel() < 2) {
			return false;
		}
		if (countValidChickensInRow() < 2 || !hasEmptyChickenSlot()) {
			return false;
		}
		return pickBestBreedPair(true) != null;
	}

	private static boolean isBabyCapture(NbtCompound cap) {
		return cap.contains("Age") && cap.getInt("Age") < 0;
	}

	private static boolean isValidCaptured(ItemStack stack) {
		if (!(stack.getItem() instanceof CapturedChickenItem)) return false;
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || cap.isEmpty() || isBabyCapture(cap)) return false;
		return true;
	}

	/** 收容格内 {@code BreedCooldown} 每 tick 减 1；有变化返回 true */
	private boolean tickChickenRowBreedCooldowns() {
		boolean any = false;
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			ItemStack st = getStack(s);
			if (!isValidCaptured(st)) continue;
			NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(st);
			if (cap == null || !cap.contains("BreedCooldown") || cap.getInt("BreedCooldown") <= 0) {
				continue;
			}
			cap.putInt("BreedCooldown", cap.getInt("BreedCooldown") - 1);
			ChickenSpawnEggItem.writeCapture(st, cap);
			setStack(s, st);
			any = true;
		}
		return any;
	}

	private static int breedCooldownRemaining(NbtCompound cap) {
		return cap.contains("BreedCooldown") ? Math.max(0, cap.getInt("BreedCooldown")) : 0;
	}

	/** 与鸡舍产蛋逻辑一致：单项 1–10 */
	private static int clampStat10(int v) {
		if (v < 1) return 1;
		if (v > 10) return 10;
		return v;
	}

	private static int captureStat10(NbtCompound cap, String key) {
		int v = cap.contains(key) ? cap.getInt(key) : 1;
		return clampStat10(v);
	}

	private static boolean captureAtFullStats(NbtCompound cap) {
		return captureStat10(cap, "Growth") == 10
				&& captureStat10(cap, "Gain") == 10
				&& captureStat10(cap, "Strength") == 10;
	}

	/** 四格均为有效成体且每只 Growth/Gain/Strength（按 1–10 钳制）均为 10 时返回 true。 */
	private boolean allFourChickensAtFullStats() {
		if (!chickenRowFull()) {
			return false;
		}
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			ItemStack st = getStack(s);
			if (!isValidCaptured(st)) {
				return false;
			}
			NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(st);
			if (cap == null || !captureAtFullStats(cap)) {
				return false;
			}
		}
		return true;
	}

	private static int statSum(NbtCompound cap) {
		int g = cap.contains("Growth") ? cap.getInt("Growth") : 0;
		int ga = cap.contains("Gain") ? cap.getInt("Gain") : 0;
		int st = cap.contains("Strength") ? cap.getInt("Strength") : 0;
		return g + ga + st;
	}

	private static ItemStack makeSpawnEggFromCapture(@Nullable NbtCompound cap) {
		if (cap == null || !cap.contains("Type")) {
			return ItemStack.EMPTY;
		}
		Identifier typeId;
		try {
			typeId = new Identifier(cap.getString("Type"));
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
		if (ChickenTypes.get(typeId) == null) {
			return ItemStack.EMPTY;
		}
		ItemStack egg = new ItemStack(ModItems.SPAWN_EGG);
		ChickenSpawnEggItem.writeType(egg, typeId);
		// 不写 ChickensCapture：与原版刷怪蛋一致，同类型可堆叠至 64；放置时按类型生成默认属性（非栏内个体数据）
		ChickenSpawnEggItem.writeCapture(egg, null);
		return egg;
	}

	private record SlotCap(int slot, NbtCompound cap, Identifier typeId, int score) {}

	private @Nullable int[] pickLowestTwoSlots() {
		List<SlotCap> list = new ArrayList<>(4);
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			ItemStack st = getStack(s);
			if (!isValidCaptured(st)) {
				return null;
			}
			NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(st);
			Identifier id = typeIdFromCapture(st);
			if (cap == null || id == null) {
				return null;
			}
			list.add(new SlotCap(s, cap, id, statSum(cap)));
		}
		list.sort(Comparator.comparingInt(SlotCap::score).thenComparingInt(SlotCap::slot));
		return new int[]{list.get(0).slot, list.get(1).slot};
	}

	/**
	 * @param requireZeroCooldown {@code true} 与繁殖执行一致：双方交配冷却均为 0；{@code false} 用于进度条，冷却中仍返回将最先繁殖的那一对
	 */
	private @Nullable BreedPair pickBestBreedPair(boolean requireZeroCooldown) {
		List<SlotCap> list = new ArrayList<>();
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			ItemStack st = getStack(s);
			if (!isValidCaptured(st)) {
				continue;
			}
			NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(st);
			Identifier id = typeIdFromCapture(st);
			if (cap == null || id == null || ChickenTypes.get(id) == null) {
				continue;
			}
			list.add(new SlotCap(s, cap, id, statSum(cap)));
		}
		if (list.size() < 2) {
			return null;
		}
		int bestPairSum = -1;
		int bestHi = -1;
		BreedPair best = null;
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				SlotCap a = list.get(i);
				SlotCap b = list.get(j);
				if (!ChickenTypes.hasBreedOutcome(a.typeId, b.typeId)) {
					continue;
				}
				if (requireZeroCooldown && (breedCooldownRemaining(a.cap) > 0 || breedCooldownRemaining(b.cap) > 0)) {
					continue;
				}
				int sum = a.score + b.score;
				int hi = Math.max(a.score, b.score);
				if (sum > bestPairSum || (sum == bestPairSum && hi > bestHi)) {
					bestPairSum = sum;
					bestHi = hi;
					best = new BreedPair(a.slot, b.slot);
				}
			}
		}
		return best;
	}

	private record BreedPair(int slotA, int slotB) {}

	private static @Nullable Identifier typeIdFromCapture(ItemStack stack) {
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || !cap.contains("Type")) return null;
		try {
			return new Identifier(cap.getString("Type"));
		} catch (Exception ignored) {
			return null;
		}
	}

	private boolean canFitEggStacks(ItemStack egg1, ItemStack egg2) {
		DefaultedList<ItemStack> pair = DefaultedList.ofSize(2, ItemStack.EMPTY);
		pair.set(0, getStack(SLOT_EGG_FIRST).copy());
		pair.set(1, getStack(SLOT_EGG_LAST).copy());
		if (!absorbIntoTwoSlotPair(pair, egg1.copy())) {
			return false;
		}
		return absorbIntoTwoSlotPair(pair, egg2.copy());
	}

	/** 将 {@code incoming} 合并进两格副本（索引 0、1）；全部并入则返回 true。 */
	private static boolean absorbIntoTwoSlotPair(DefaultedList<ItemStack> pair, ItemStack incoming) {
		if (incoming.isEmpty()) {
			return true;
		}
		for (int i = 0; i < 2; i++) {
			ItemStack slot = pair.get(i);
			if (slot.isEmpty()) {
				pair.set(i, incoming.copy());
				return true;
			}
			if (ItemStack.canCombine(slot, incoming)) {
				int room = slot.getMaxCount() - slot.getCount();
				if (room <= 0) {
					continue;
				}
				int move = Math.min(room, incoming.getCount());
				ItemStack nw = slot.copy();
				nw.increment(move);
				incoming.decrement(move);
				pair.set(i, nw);
				if (incoming.isEmpty()) {
					return true;
				}
			}
		}
		for (int i = 0; i < 2; i++) {
			if (pair.get(i).isEmpty()) {
				pair.set(i, incoming.copy());
				return true;
			}
		}
		return false;
	}

	/**
	 * 计时结束：四鸡满则淘汰为蛋（不扣种子）；否则繁殖（扣 2 种子，子代进 0–3 空位）。
	 */
	private boolean tryConsumeBreed(ServerWorld serverWorld) {
		if (chickenRowFull()) {
			return tryCullToSpawnEggs();
		}
		return tryBreedIntoEmptyRowSlot(serverWorld);
	}

	private boolean tryCullToSpawnEggs() {
		if (allFourChickensAtFullStats()) {
			return false;
		}
		int[] low = pickLowestTwoSlots();
		if (low == null) {
			return false;
		}
		NbtCompound c0 = ChickenSpawnEggItem.readCaptureCompound(getStack(low[0]));
		NbtCompound c1 = ChickenSpawnEggItem.readCaptureCompound(getStack(low[1]));
		ItemStack e0 = makeSpawnEggFromCapture(c0);
		ItemStack e1 = makeSpawnEggFromCapture(c1);
		if (e0.isEmpty() || e1.isEmpty() || !canFitEggStacks(e0, e1)) {
			return false;
		}
		setStack(low[0], ItemStack.EMPTY);
		setStack(low[1], ItemStack.EMPTY);
		if (!insertEggOutput(e0) || !insertEggOutput(e1)) {
			return false;
		}
		world.playSound(null, pos, SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.95F);
		markDirty();
		return true;
	}

	private boolean tryBreedIntoEmptyRowSlot(ServerWorld serverWorld) {
		if (countBreederFuel() < 2 || !hasEmptyChickenSlot()) {
			return false;
		}
		BreedPair pair = pickBestBreedPair(true);
		if (pair == null) {
			return false;
		}
		NbtCompound cap1 = ChickenSpawnEggItem.readCaptureCompound(getStack(pair.slotA));
		NbtCompound cap2 = ChickenSpawnEggItem.readCaptureCompound(getStack(pair.slotB));
		ItemStack out = CapturedChickenBreeding.breedPocket(serverWorld, cap1, cap2);
		if (out.isEmpty() || !insertCapturedIntoChickenRow(out)) {
			return false;
		}
		applyBreedCooldownToSlot(pair.slotA);
		applyBreedCooldownToSlot(pair.slotB);
		consumeBreederFuel(2);
		world.playSound(null, pos, SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.95F);
		markDirty();
		return true;
	}

	/** 子代收容鸡只进 0–3 中的空格（堆叠上限 1）。 */
	private void applyBreedCooldownToSlot(int slot) {
		ItemStack st = getStack(slot);
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(st);
		if (cap == null) {
			return;
		}
		int cd = ChickenAttributeTicks.breedCooldownTicks(captureStat10(cap, "Strength"));
		cap.putInt("BreedCooldown", cd);
		ChickenSpawnEggItem.writeCapture(st, cap);
		setStack(slot, st);
	}

	private boolean insertCapturedIntoChickenRow(ItemStack stack) {
		for (int s = SLOT_CHICKEN_1; s <= SLOT_NEXT_LAST; s++) {
			if (getStack(s).isEmpty()) {
				setStack(s, stack.copy());
				return true;
			}
		}
		return false;
	}

	private boolean insertEggOutput(ItemStack stack) {
		for (int s = SLOT_EGG_FIRST; s <= SLOT_EGG_LAST; s++) {
			ItemStack slot = getStack(s);
			if (slot.isEmpty()) {
				setStack(s, stack.copy());
				return true;
			}
			if (ItemStack.canCombine(slot, stack)) {
				int room = slot.getMaxCount() - slot.getCount();
				if (room <= 0) {
					continue;
				}
				int move = Math.min(room, stack.getCount());
				slot.increment(move);
				stack.decrement(move);
				setStack(s, slot);
				if (stack.isEmpty()) {
					return true;
				}
			}
		}
		return stack.isEmpty();
	}

	private int countBreederFuel() {
		int n = 0;
		for (int s = SLOT_SEEDS_FIRST; s <= SLOT_SEEDS_LAST; s++) {
			ItemStack st = getStack(s);
			if (st.isIn(BREEDER_FUEL)) {
				n += st.getCount();
			}
		}
		return n;
	}

	/** 按槽位顺序从 {@link #SLOT_SEEDS_FIRST} 起扣除燃料。 */
	private void consumeBreederFuel(int amount) {
		int rem = amount;
		for (int s = SLOT_SEEDS_FIRST; s <= SLOT_SEEDS_LAST && rem > 0; s++) {
			ItemStack st = getStack(s);
			if (!st.isIn(BREEDER_FUEL)) continue;
			int take = Math.min(rem, st.getCount());
			st.decrement(take);
			rem -= take;
			setStack(s, st.isEmpty() ? ItemStack.EMPTY : st);
		}
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		return new BreederScreenHandler(syncId, playerInventory, this);
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("screen.chickens.breeder");
	}

	@Override
	public DefaultedList<ItemStack> getItems() {
		return items;
	}

	@Override
	public void markDirty() {
		super.markDirty();
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		return switch (side) {
			case UP -> seedSlotIndices();
			case DOWN -> new int[]{SLOT_CHICKEN_1, SLOT_CHICKEN_2, SLOT_NEXT_FIRST, SLOT_NEXT_LAST, SLOT_EGG_FIRST, SLOT_EGG_LAST};
			default -> new int[0];
		};
	}

	private static int[] seedSlotIndices() {
		int[] a = new int[SEED_SLOT_COUNT];
		for (int i = 0; i < SEED_SLOT_COUNT; i++) {
			a[i] = SLOT_SEEDS_FIRST + i;
		}
		return a;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
		return dir == Direction.UP
				&& slot >= SLOT_SEEDS_FIRST
				&& slot <= SLOT_SEEDS_LAST
				&& stack.isIn(BREEDER_FUEL);
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction dir) {
		return dir == Direction.DOWN && slot >= SLOT_CHICKEN_1 && slot <= SLOT_EGG_LAST;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		writeInventory(nbt);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		readInventory(nbt);
	}
}
