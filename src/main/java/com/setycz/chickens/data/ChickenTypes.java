package com.setycz.chickens.data;

import com.setycz.chickens.ChickensFabricMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public final class ChickenTypes {
	private ChickenTypes() {}

	private static final Map<Identifier, ChickenType> TYPES = new HashMap<>();
	private static final Random RAND = new Random();
	private static final Set<Identifier> WOOD_CHICKEN_IDS = Set.of(
			ChickensFabricMod.id("log"),
			ChickensFabricMod.id("spruce_log"),
			ChickensFabricMod.id("birch_log"),
			ChickensFabricMod.id("jungle_log"),
			ChickensFabricMod.id("acacia_log"),
			ChickensFabricMod.id("dark_oak_log"),
			ChickensFabricMod.id("mangrove_log"),
			ChickensFabricMod.id("cherry_log"),
			ChickensFabricMod.id("crimson_stem"),
			ChickensFabricMod.id("warped_stem")
	);

	public static void setAll(Map<Identifier, ChickenType> newTypes) {
		TYPES.clear();
		TYPES.putAll(newTypes);
		CapturedChickenModelIndex.rebuild(TYPES.values());
		ChickensFabricMod.LOGGER.info("已加载鸡类型：{}", TYPES.size());
	}

	public static Collection<ChickenType> all() {
		return Collections.unmodifiableCollection(TYPES.values());
	}

	public static ChickenType getOrDefault(Identifier id) {
		ChickenType t = TYPES.get(id);
		if (t != null) return t;
		return fallback(id);
	}

	public static @Nullable ChickenType get(Identifier id) {
		return TYPES.get(id);
	}

	public static Identifier defaultId() {
		return ChickensFabricMod.id("white");
	}

	public static Identifier smartId() {
		return ChickensFabricMod.id("smart");
	}

	private static ChickenType fallback(Identifier id) {
		return new ChickenType(
				id,
				"entity.chickens.chickens_chicken",
				new Identifier("minecraft", "textures/entity/chicken.png"),
				0xFFFFFF,
				0xFFFF00,
				new ItemStack(Items.EGG),
				new ItemStack(Items.EGG),
				1.0f,
				SpawnType.NONE,
				null,
				null
		);
	}

	public static int tierOf(Identifier id) {
		ChickenType t = TYPES.get(id);
		if (t == null || t.isBase()) return 1;
		return 1 + Math.max(tierOf(t.parent1()), tierOf(t.parent2()));
	}

	public static float childChancePercent(Identifier child) {
		int tier = tierOf(child);
		if (tier <= 1) return 0;
		ChickenType t = TYPES.get(child);
		if (t == null || t.parent1() == null || t.parent2() == null) return 0;

		List<Identifier> possible = childrenAndParents(t.parent1(), t.parent2());
		int maxChance = maxTier(possible) + 1;
		int maxDice = diceMax(possible, maxChance);
		return ((maxChance - tier) * 100.0f) / Math.max(1, maxDice);
	}

	/** 双亲组合是否可能存在繁殖结果（不参与随机骰子，仅供繁殖箱等提前判断）。 */
	public static boolean hasBreedOutcome(Identifier parent1, Identifier parent2) {
		return !childrenAndParents(parent1, parent2).isEmpty();
	}

	public static @Nullable Identifier randomChild(Identifier parent1, Identifier parent2) {
		List<Identifier> possible = childrenAndParents(parent1, parent2);
		if (possible.isEmpty()) return null;

		int maxChance = maxTier(possible) + 1;
		int maxDice = diceMax(possible, maxChance);
		int dice = RAND.nextInt(maxDice);

		int current = 0;
		for (Identifier id : possible) {
			current += maxChance - tierOf(id);
			if (dice < current) return id;
		}
		return null;
	}

	private static List<Identifier> childrenAndParents(Identifier parent1, Identifier parent2) {
		List<Identifier> result = new ArrayList<>();
		if (TYPES.containsKey(parent1)) result.add(parent1);
		if (TYPES.containsKey(parent2)) result.add(parent2);
		for (ChickenType t : TYPES.values()) {
			if (isChildOf(t, parent1, parent2)) result.add(t.id());
		}
		return result;
	}

	private static boolean isChildOf(ChickenType type, Identifier parent1, Identifier parent2) {
		if (type.parent1() == null || type.parent2() == null) {
			return false;
		}
		return (matchesParent(type.parent1(), parent1) && matchesParent(type.parent2(), parent2))
				|| (matchesParent(type.parent1(), parent2) && matchesParent(type.parent2(), parent1));
	}

	private static boolean matchesParent(Identifier expected, Identifier actual) {
		if (expected.equals(actual)) {
			return true;
		}
		return WOOD_CHICKEN_IDS.contains(expected) && WOOD_CHICKEN_IDS.contains(actual);
	}

	private static int maxTier(List<Identifier> ids) {
		int max = 0;
		for (Identifier id : ids) max = Math.max(max, tierOf(id));
		return max;
	}

	private static int diceMax(List<Identifier> ids, int maxChance) {
		int sum = 0;
		for (Identifier id : ids) sum += maxChance - tierOf(id);
		return Math.max(1, sum);
	}

	public static List<ChickenType> getSpawnCandidates(SpawnType spawnType) {
		List<ChickenType> result = new ArrayList<>();
		for (ChickenType type : TYPES.values()) {
			if (type.canSpawnNaturally() && type.spawnType() == spawnType) {
				result.add(type);
			}
		}
		result.sort(Comparator.comparing(type -> type.id().toString()));
		return result;
	}

	public static boolean isDyeChicken(ChickenType type) {
		Item item = type.layItem().getItem();
		Identifier itemId = Registries.ITEM.getId(item);
		return itemId != null && itemId.getPath().endsWith("_dye");
	}

	public static List<ChickenType> getDyeChickens() {
		List<ChickenType> result = new ArrayList<>();
		for (ChickenType type : TYPES.values()) {
			if (isDyeChicken(type)) {
				result.add(type);
			}
		}
		result.sort(Comparator.comparing(type -> type.id().toString()));
		return result;
	}

	public static @Nullable ChickenType findByLayItem(Item item) {
		for (ChickenType type : TYPES.values()) {
			if (type.layItem().getItem() == item) {
				return type;
			}
		}
		return null;
	}
}

