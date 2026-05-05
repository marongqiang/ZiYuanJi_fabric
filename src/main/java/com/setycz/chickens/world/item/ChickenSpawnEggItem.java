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

import org.jetbrains.annotations.Nullable;

public final class ChickenSpawnEggItem extends Item {
	public static final String NBT_CHICKEN_TYPE = "Type";
	/** 击鸡棒等写入的全套实体模组数据（含原版鸡成长等），与地面放置时恢复的鸡一致 */
	public static final String NBT_CAPTURE = "ChickensCapture";

	public ChickenSpawnEggItem(Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		Identifier typeId = readType(stack);
		return Text.translatable(
				"item.chickens.spawn_egg.named",
				Text.translatable(ChickenTypes.getOrDefault(typeId).nameKey())
		);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		return spawnChickenAt(context, readType(context.getStack()));
	}

	/** 与对方块使用刷怪蛋相同：生成模组鸡；用于 {@link ChickenSpawnEggItem} 与 {@link ColoredEggItem}。 */
	public static ActionResult spawnChickenAt(ItemUsageContext context, Identifier typeId) {
		World world = context.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		BlockPos pos = context.getBlockPos().offset(context.getSide());

		ItemStack stack = context.getStack();

		ChickensChickenEntity entity = ModEntities.CHICKENS_CHICKEN.create(serverWorld);
		if (entity == null) return ActionResult.FAIL;

		entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, serverWorld.random.nextFloat() * 360.0F, 0.0F);
		entity.initialize(serverWorld, serverWorld.getLocalDifficulty(pos), SpawnReason.SPAWN_EGG, null, null);

		NbtCompound capture = readCaptureCompound(stack);
		if (capture != null && !capture.isEmpty()) {
			entity.readCustomDataFromNbt(capture);
		} else {
			entity.setChickenTypeId(typeId);
		}

		serverWorld.spawnEntity(entity);

		if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}

	public static Identifier readType(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		if (nbt != null && nbt.contains(NBT_CHICKEN_TYPE)) {
			try {
				return new Identifier(nbt.getString(NBT_CHICKEN_TYPE));
			} catch (Exception ignored) {
			}
		}
		return ChickenTypes.defaultId();
	}

	public static void writeType(ItemStack stack, Identifier type) {
		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.putString(NBT_CHICKEN_TYPE, type.toString());
		nbt.remove("CustomModelData");
	}

	public static void writeCapture(ItemStack stack, NbtCompound capture) {
		if (capture == null || capture.isEmpty()) {
			stack.getOrCreateNbt().remove(NBT_CAPTURE);
			return;
		}
		stack.getOrCreateNbt().put(NBT_CAPTURE, capture.copy());
	}

	public static @Nullable NbtCompound readCaptureCompound(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		if (nbt == null || !nbt.contains(NBT_CAPTURE)) {
			return null;
		}
		NbtCompound c = nbt.getCompound(NBT_CAPTURE);
		return c.isEmpty() ? null : c;
	}

	/**
	 * 用于击鸡棒等：把鸡的自定义数据写成可放回刷怪蛋的 tag（含 {@link ChickensChickenEntity#writeCustomDataToNbt} 链子保存的成长/产蛋进度等）。
	 */
	public static void writeCaptureFromChicken(ChickensChickenEntity chicken, ItemStack spawnEggStack) {
		writeType(spawnEggStack, chicken.getChickenTypeId());
		NbtCompound cap = new NbtCompound();
		chicken.writeCustomDataToNbt(cap);
		writeCapture(spawnEggStack, cap);
	}
}

