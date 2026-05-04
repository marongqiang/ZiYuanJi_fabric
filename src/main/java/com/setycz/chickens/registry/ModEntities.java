package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

public final class ModEntities {
	private ModEntities() {}

	public static final EntityType<ChickensChickenEntity> CHICKENS_CHICKEN = Registry.register(
			Registries.ENTITY_TYPE,
			ChickensFabricMod.id("chickens_chicken"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ChickensChickenEntity::new)
					.dimensions(EntityDimensions.fixed(0.4F, 0.7F))
					.trackRangeBlocks(8)
					.trackedUpdateRate(3)
					.build()
	);

	public static void init() {
		// 注册默认属性，否则生成实体会在 LivingEntity 初始化时崩溃
		FabricDefaultAttributeRegistry.register(CHICKENS_CHICKEN, ChickenEntity.createChickenAttributes());
		SpawnRestriction.register(CHICKENS_CHICKEN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ChickenEntity::canMobSpawn);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.CREATURE, CHICKENS_CHICKEN, 10, 3, 5);
		BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(), SpawnGroup.CREATURE, CHICKENS_CHICKEN, 10, 3, 5);
	}
}

