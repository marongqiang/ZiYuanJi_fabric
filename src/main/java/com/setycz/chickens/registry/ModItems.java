package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.ChickenCatcherItem;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import com.setycz.chickens.world.item.ColoredEggItem;
import com.setycz.chickens.world.item.FixedChickenSpawnEggItem;
import com.setycz.chickens.world.item.LiquidEggItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class ModItems {
	private ModItems() {}

	/** 圆石 / 石头 / 平滑石鸡使用专用刷怪蛋物品，与通用 {@link #SPAWN_EGG} 区分 */
	public static final Set<Identifier> CHICKEN_TYPES_WITH_FIXED_SPAWN_EGG = Set.of(
			ChickensFabricMod.id("cobblestone"),
			ChickensFabricMod.id("stone"),
			ChickensFabricMod.id("smooth_stone")
	);

	/** 创造栏 / EMI 下蛋配方：石链用专用蛋，其余用带 {@code Type} 的通用蛋 */
	public static ItemStack spawnEggStackForChickenType(Identifier typeId) {
		if (typeId.equals(ChickensFabricMod.id("cobblestone"))) {
			return new ItemStack(COBBLESTONE_CHICKEN_SPAWN_EGG);
		}
		if (typeId.equals(ChickensFabricMod.id("stone"))) {
			return new ItemStack(STONE_CHICKEN_SPAWN_EGG);
		}
		if (typeId.equals(ChickensFabricMod.id("smooth_stone"))) {
			return new ItemStack(SMOOTH_STONE_CHICKEN_SPAWN_EGG);
		}
		ItemStack stack = new ItemStack(SPAWN_EGG);
		ChickenSpawnEggItem.writeType(stack, typeId);
		return stack;
	}

	public static final Item CHICKEN_CATCHER = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("chicken_catcher"),
			new ChickenCatcherItem(new FabricItemSettings().maxCount(1).maxDamage(238).recipeRemainder(Items.BUCKET))
	);

	/**
	 * 「物品鸡」：与 MoreChickens {@code ChickenItem} 相同堆叠上限 1；数据在 {@link ChickenSpawnEggItem#NBT_CAPTURE}；
	 * 击鸡收成时先以掉落物形式掉进世界再上拾取。
	 */
	public static final Item CAPTURED_CHICKEN = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("captured_chicken"),
			new CapturedChickenItem(new FabricItemSettings().maxCount(1))
	);

	public static final Item SPAWN_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("spawn_egg"),
			new ChickenSpawnEggItem(new FabricItemSettings().maxCount(64))
	);

	public static final Item COBBLESTONE_CHICKEN_SPAWN_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("cobblestone_chicken_spawn_egg"),
			new FixedChickenSpawnEggItem(ChickensFabricMod.id("cobblestone"), new FabricItemSettings().maxCount(64))
	);

	public static final Item STONE_CHICKEN_SPAWN_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("stone_chicken_spawn_egg"),
			new FixedChickenSpawnEggItem(ChickensFabricMod.id("stone"), new FabricItemSettings().maxCount(64))
	);

	public static final Item SMOOTH_STONE_CHICKEN_SPAWN_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("smooth_stone_chicken_spawn_egg"),
			new FixedChickenSpawnEggItem(ChickensFabricMod.id("smooth_stone"), new FabricItemSettings().maxCount(64))
	);

	public static final Item LIQUID_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("liquid_egg"),
			new LiquidEggItem(new FabricItemSettings().maxCount(64))
	);

	public static final Item COLORED_EGG = Registry.register(
			Registries.ITEM,
			ChickensFabricMod.id("colored_egg"),
			new ColoredEggItem(new FabricItemSettings().maxCount(64))
	);

	public static void init() {
		// classload triggers registrations
	}
}

