package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.blockentity.BreederBlockEntity;
import com.setycz.chickens.world.blockentity.HenhouseBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
	private ModBlockEntities() {}

	public static final BlockEntityType<HenhouseBlockEntity> HENHOUSE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			ChickensFabricMod.id("henhouse"),
			FabricBlockEntityTypeBuilder.create(HenhouseBlockEntity::new, ModBlocks.HENHOUSE).build()
	);

	public static final BlockEntityType<BreederBlockEntity> BREEDER = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			ChickensFabricMod.id("breeder"),
			FabricBlockEntityTypeBuilder.create(BreederBlockEntity::new, ModBlocks.BREEDER).build()
	);

	public static void init() {
		// classload triggers registrations
	}
}

