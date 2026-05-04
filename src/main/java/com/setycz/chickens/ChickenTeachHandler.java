package com.setycz.chickens;

import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;

public final class ChickenTeachHandler {
	private static final int DYES_REQUIRED = 10;
	private static final int WATER_BOTTLES_REQUIRED = 3;
	private static final int TOTEMS_REQUIRED = 3;
	private static final int STARS_REQUIRED = 3;

	private ChickenTeachHandler() {}

	public static void init() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack held = player.getStackInHand(hand);
			if (held.isOf(Items.BOOK)) {
				if (world.isClient) {
					return entity.getType() == EntityType.CHICKEN ? ActionResult.SUCCESS : ActionResult.PASS;
				}
				if (!(entity instanceof ChickenEntity chicken) || entity instanceof ChickensChickenEntity) {
					return ActionResult.PASS;
				}

				ChickensChickenEntity smartChicken = ModEntities.CHICKENS_CHICKEN.create((ServerWorld) world);
				if (smartChicken == null) {
					return ActionResult.FAIL;
				}

				smartChicken.refreshPositionAndAngles(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getYaw(), chicken.getPitch());
				smartChicken.setChickenTypeId(ChickenTypes.smartId());
				smartChicken.setBaby(chicken.isBaby());
				if (chicken.hasCustomName()) {
					smartChicken.setCustomName(chicken.getCustomName());
				}

				entity.discard();
				world.spawnEntity(smartChicken);
				smartChicken.playSpawnEffects();
				return ActionResult.SUCCESS;
			}

			Identifier targetType = dyeToChickenType(held.getItem());
			if (targetType == null) {
				// water bottle training: 3x water bottles -> water chicken
				if (held.isOf(Items.POTION)
						&& PotionUtil.getPotion(held) == Potions.WATER
						&& entity instanceof ChickensChickenEntity chicken
						&& chicken.getChickenTypeId().equals(ChickenTypes.smartId())) {
					if (world.isClient) {
						return ActionResult.SUCCESS;
					}
					// Consume one water bottle (return empty bottle), track count on the chicken
					if (!player.isCreative()) {
						held.decrement(1);
						ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
						if (held.isEmpty()) {
							player.setStackInHand(hand, bottle);
						} else if (!player.getInventory().insertStack(bottle)) {
							player.dropItem(bottle, false);
						}
					}

					int taught = chicken.getWaterTeach() + 1;
					chicken.setWaterTeach(taught);
					if (taught >= WATER_BOTTLES_REQUIRED) {
						chicken.setWaterTeach(0);
						chicken.setChickenTypeId(ChickensFabricMod.id("water"));
						chicken.playSpawnEffects();
					}
					return ActionResult.SUCCESS;
				}

				// totem training: 3x totem_of_undying -> totem chicken
				if (held.isOf(Items.TOTEM_OF_UNDYING)
						&& entity instanceof ChickensChickenEntity chicken
						&& chicken.getChickenTypeId().equals(ChickenTypes.smartId())) {
					if (world.isClient) {
						return ActionResult.SUCCESS;
					}
					if (!player.isCreative()) {
						held.decrement(1);
					}
					int taught = chicken.getTotemTeach() + 1;
					chicken.setTotemTeach(taught);
					if (taught >= TOTEMS_REQUIRED) {
						chicken.setTotemTeach(0);
						chicken.setChickenTypeId(ChickensFabricMod.id("totem"));
						chicken.playSpawnEffects();
					}
					return ActionResult.SUCCESS;
				}

				// nether star training: 3x nether_star -> nether star chicken
				if (held.isOf(Items.NETHER_STAR)
						&& entity instanceof ChickensChickenEntity chicken
						&& chicken.getChickenTypeId().equals(ChickenTypes.smartId())) {
					if (world.isClient) {
						return ActionResult.SUCCESS;
					}
					if (!player.isCreative()) {
						held.decrement(1);
					}
					int taught = chicken.getStarTeach() + 1;
					chicken.setStarTeach(taught);
					if (taught >= STARS_REQUIRED) {
						chicken.setStarTeach(0);
						chicken.setChickenTypeId(ChickensFabricMod.id("nether_star"));
						chicken.playSpawnEffects();
					}
					return ActionResult.SUCCESS;
				}
				return ActionResult.PASS;
			}
			if (!(entity instanceof ChickensChickenEntity chicken) || !chicken.getChickenTypeId().equals(ChickenTypes.smartId())) {
				return ActionResult.PASS;
			}
			if (world.isClient) {
				return ActionResult.SUCCESS;
			}
			if (held.getCount() < DYES_REQUIRED) {
				return ActionResult.FAIL;
			}

			if (!player.isCreative()) {
				held.decrement(DYES_REQUIRED);
			}
			chicken.setChickenTypeId(targetType);
			chicken.playSpawnEffects();
			return ActionResult.SUCCESS;
		});
	}

	private static @Nullable Identifier dyeToChickenType(Item item) {
		if (item == Items.RED_DYE) return ChickensFabricMod.id("red");
		if (item == Items.WHITE_DYE) return ChickensFabricMod.id("white");
		if (item == Items.BLUE_DYE) return ChickensFabricMod.id("blue");
		if (item == Items.GREEN_DYE) return ChickensFabricMod.id("green");
		if (item == Items.BLACK_DYE) return ChickensFabricMod.id("black");
		if (item == Items.YELLOW_DYE) return ChickensFabricMod.id("yellow");
		if (item == Items.SNOWBALL) return ChickensFabricMod.id("snowball");
		if (item == Items.OAK_LOG) return ChickensFabricMod.id("log");
		if (item == Items.SPRUCE_LOG) return ChickensFabricMod.id("spruce_log");
		if (item == Items.BIRCH_LOG) return ChickensFabricMod.id("birch_log");
		if (item == Items.JUNGLE_LOG) return ChickensFabricMod.id("jungle_log");
		if (item == Items.ACACIA_LOG) return ChickensFabricMod.id("acacia_log");
		if (item == Items.DARK_OAK_LOG) return ChickensFabricMod.id("dark_oak_log");
		if (item == Items.MANGROVE_LOG) return ChickensFabricMod.id("mangrove_log");
		if (item == Items.CHERRY_LOG) return ChickensFabricMod.id("cherry_log");
		if (item == Items.CRIMSON_STEM) return ChickensFabricMod.id("crimson_stem");
		if (item == Items.WARPED_STEM) return ChickensFabricMod.id("warped_stem");
		return null;
	}
}

