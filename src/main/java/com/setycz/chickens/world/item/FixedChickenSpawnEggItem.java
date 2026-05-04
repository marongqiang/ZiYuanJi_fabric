package com.setycz.chickens.world.item;

import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class FixedChickenSpawnEggItem extends Item {
	private final Identifier typeId;

	public FixedChickenSpawnEggItem(Identifier typeId, Settings settings) {
		super(settings);
		this.typeId = typeId;
	}

	@Override
	public Text getName(ItemStack stack) {
		return Text.translatable(
				"item.chickens.spawn_egg.named",
				Text.translatable(ChickenTypes.getOrDefault(typeId).nameKey())
		);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		BlockPos pos = context.getBlockPos().offset(context.getSide());

		ChickensChickenEntity entity = ModEntities.CHICKENS_CHICKEN.create(serverWorld);
		if (entity == null) return ActionResult.FAIL;

		entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, serverWorld.random.nextFloat() * 360.0F, 0.0F);
		entity.initialize(serverWorld, serverWorld.getLocalDifficulty(pos), SpawnReason.SPAWN_EGG, null, null);
		ItemStack clicked = context.getStack();
		NbtCompound capture = ChickenSpawnEggItem.readCaptureCompound(clicked);
		if (capture != null && !capture.isEmpty()) {
			entity.readCustomDataFromNbt(capture);
		} else {
			entity.setChickenTypeId(typeId);
		}
		serverWorld.spawnEntity(entity);

		if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
			context.getStack().decrement(1);
		}

		return ActionResult.SUCCESS;
	}
}

