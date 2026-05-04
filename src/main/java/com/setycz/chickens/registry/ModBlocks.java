package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.block.BreederBlock;
import com.setycz.chickens.world.block.HenhouseBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
	private ModBlocks() {}

	public static final Block HENHOUSE = Registry.register(
			Registries.BLOCK,
			ChickensFabricMod.id("henhouse"),
			new HenhouseBlock()
	);

	public static final Block BREEDER = Registry.register(
			Registries.BLOCK,
			ChickensFabricMod.id("breeder"),
			new BreederBlock()
	);

	static {
		Registry.register(
				Registries.ITEM,
				ChickensFabricMod.id("henhouse"),
				new BlockItem(HENHOUSE, new FabricItemSettings())
		);
		Registry.register(
				Registries.ITEM,
				ChickensFabricMod.id("breeder"),
				new BlockItem(BREEDER, new FabricItemSettings())
		);
	}

	public static void init() {
		// classload triggers registrations
	}
}

