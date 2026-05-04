package com.setycz.chickens.world.breeding;

import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 用两只收容鸡的 {@link ChickenSpawnEggItem#NBT_CAPTURE} 数据，在不上世界的情况下走
 * {@link ChickensChickenEntity#createChild}，与野外繁殖统计规则一致。
 */
public final class CapturedChickenBreeding {
	private CapturedChickenBreeding() {}

	public static @Nullable NbtCompound readCapture(ItemStack stack) {
		return ChickenSpawnEggItem.readCaptureCompound(stack);
	}

	/**
	 * @return 新收容鸡物品，若无法繁殖（幼体、无配方子代等）则 EMPTY
	 */
	public static ItemStack breedPocket(ServerWorld world, NbtCompound cap1, NbtCompound cap2) {
		if (cap1 == null || cap2 == null || cap1.isEmpty() || cap2.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ChickensChickenEntity p1 = ModEntities.CHICKENS_CHICKEN.create(world);
		ChickensChickenEntity p2 = ModEntities.CHICKENS_CHICKEN.create(world);
		if (p1 == null || p2 == null) {
			return ItemStack.EMPTY;
		}
		p1.readCustomDataFromNbt(cap1);
		p2.readCustomDataFromNbt(cap2);
		if (p1.isBaby() || p2.isBaby()) {
			discardQuiet(world, p1, p2, null);
			return ItemStack.EMPTY;
		}
		ChickensChickenEntity child = (ChickensChickenEntity) p1.createChild(world, p2);
		discardQuiet(world, p1, p2, null);
		if (child == null) {
			return ItemStack.EMPTY;
		}
		ItemStack pocket = new ItemStack(ModItems.CAPTURED_CHICKEN);
		CapturedChickenItem.packFromEntity(child, pocket);
		discardQuiet(world, null, null, child);
		return pocket;
	}

	private static void discardQuiet(World world, @Nullable ChickensChickenEntity a, @Nullable ChickensChickenEntity b, @Nullable ChickensChickenEntity c) {
		if (a != null) a.discard();
		if (b != null) b.discard();
		if (c != null) c.discard();
	}
}
