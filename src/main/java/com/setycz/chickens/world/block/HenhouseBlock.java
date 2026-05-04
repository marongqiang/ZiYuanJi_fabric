package com.setycz.chickens.world.block;

import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.world.blockentity.HenhouseBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class HenhouseBlock extends BlockWithEntity {
	public HenhouseBlock() {
		super(Settings.create().strength(2.0f));
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new HenhouseBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> givenType) {
		return world.isClient || givenType != ModBlockEntities.HENHOUSE ? null
				: (BlockEntityTicker<T>) (w, p, st, entity) ->
				HenhouseBlockEntity.tickServer(w, p, st, (HenhouseBlockEntity) entity);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof NamedScreenHandlerFactory factory) {
			player.openHandledScreen(factory);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof HenhouseBlockEntity henhouse) {
				ItemScatterer.spawn(world, pos, henhouse);
				world.updateComparators(pos, this);
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}

