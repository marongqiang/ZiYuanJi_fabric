package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.LiquidEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
	private ModItemGroups() {}

	public static final ItemGroup CHICKENS = Registry.register(
			Registries.ITEM_GROUP,
			ChickensFabricMod.id("chickens"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup.chickens.chickens"))
					.icon(() -> new ItemStack(ModItems.CHICKEN_CATCHER))
					.entries((context, entries) -> {
						entries.add(new ItemStack(ModItems.CHICKEN_CATCHER));

						// 石链：专用蛋与收容展示紧挨成对（若只把蛋放栏首、收容仍走无序循环，收容会排到很后页，像「没有」）
						entries.add(new ItemStack(ModItems.COBBLESTONE_CHICKEN_SPAWN_EGG));
						entries.add(CapturedChickenItem.createDisplayStack(ChickensFabricMod.id("cobblestone")));
						entries.add(new ItemStack(ModItems.STONE_CHICKEN_SPAWN_EGG));
						entries.add(CapturedChickenItem.createDisplayStack(ChickensFabricMod.id("stone")));
						entries.add(new ItemStack(ModItems.SMOOTH_STONE_CHICKEN_SPAWN_EGG));
						entries.add(CapturedChickenItem.createDisplayStack(ChickensFabricMod.id("smooth_stone")));

						// 其余鸡：刷怪蛋 + 收容展示；石链三种已在上面成对列出，此处跳过以免重复
						for (var type : ChickenTypes.all()) {
							if (ModItems.CHICKEN_TYPES_WITH_FIXED_SPAWN_EGG.contains(type.id())) {
								continue;
							}
							entries.add(ModItems.spawnEggStackForChickenType(type.id()));
							entries.add(CapturedChickenItem.createDisplayStack(type.id()));
						}

						ItemStack waterEgg = new ItemStack(ModItems.LIQUID_EGG);
						LiquidEggItem.writeLiquid(waterEgg, net.minecraft.util.Identifier.of("minecraft", "water"));
						entries.add(waterEgg);

						ItemStack lavaEgg = new ItemStack(ModItems.LIQUID_EGG);
						LiquidEggItem.writeLiquid(lavaEgg, net.minecraft.util.Identifier.of("minecraft", "lava"));
						entries.add(lavaEgg);

						entries.add(new ItemStack(ModBlocks.HENHOUSE));
						entries.add(new ItemStack(ModBlocks.BREEDER));

						// convenience items for testing
						entries.add(new ItemStack(Items.HAY_BLOCK));
					})
					.build()
	);

	public static void init() {
		// classload triggers registrations
	}
}

