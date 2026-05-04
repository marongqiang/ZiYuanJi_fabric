package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
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

						// Stone chain spawn eggs (furnace upgrade path)
						entries.add(new ItemStack(ModItems.COBBLESTONE_CHICKEN_SPAWN_EGG));
						entries.add(new ItemStack(ModItems.STONE_CHICKEN_SPAWN_EGG));
						entries.add(new ItemStack(ModItems.SMOOTH_STONE_CHICKEN_SPAWN_EGG));

						// Spawn eggs for all loaded chicken types
						for (var type : ChickenTypes.all()) {
							ItemStack egg = new ItemStack(ModItems.SPAWN_EGG);
							ChickenSpawnEggItem.writeType(egg, type.id());
							entries.add(egg);
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

