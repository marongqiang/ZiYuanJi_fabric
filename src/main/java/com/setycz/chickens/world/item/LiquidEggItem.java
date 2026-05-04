package com.setycz.chickens.world.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LiquidEggItem extends Item {
	public static final String NBT_LIQUID = "Liquid";

	public LiquidEggItem(Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		Block liquid = readLiquid(stack);
		String key = liquid == Blocks.LAVA ? "item.chickens.liquid_egg.lava" : "item.chickens.liquid_egg.water";
		return Text.translatable(key);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.chickens.liquid_egg.tooltip"));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		Block liquid = readLiquid(stack);

		BlockHitResult hit = raycast(world, user, RaycastContext.FluidHandling.NONE);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return TypedActionResult.pass(stack);
		}

		if (!world.isClient) {
			BlockPos pos = hit.getBlockPos();
			BlockPos placePos = world.getBlockState(pos).isReplaceable() ? pos : pos.offset(hit.getSide());

			if (!user.canPlaceOn(placePos, hit.getSide(), stack)) {
				return TypedActionResult.fail(stack);
			}

			BlockState state = world.getBlockState(placePos);
			if (!state.isReplaceable()) {
				return TypedActionResult.fail(stack);
			}

			world.setBlockState(placePos, liquid.getDefaultState(), Block.NOTIFY_ALL);
			if (!user.isCreative()) {
				stack.decrement(1);
			}
		}

		return TypedActionResult.success(stack, world.isClient);
	}

	public static Block readLiquid(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		if (nbt != null && nbt.contains(NBT_LIQUID)) {
			String s = nbt.getString(NBT_LIQUID);
			if ("lava".equalsIgnoreCase(s) || "minecraft:lava".equalsIgnoreCase(s)) return Blocks.LAVA;
			if ("water".equalsIgnoreCase(s) || "minecraft:water".equalsIgnoreCase(s)) return Blocks.WATER;
		}
		return Blocks.WATER;
	}

	public static void writeLiquid(ItemStack stack, Identifier liquidId) {
		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.putString(NBT_LIQUID, liquidId.toString());
	}
}

