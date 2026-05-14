package com.setycz.chickens.world.block;

import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.world.blockentity.BreederBlockEntity;
import com.setycz.chickens.world.item.CapturedChickenItem;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class BreederBlock extends BlockWithEntity {
	public BreederBlock() {
		super(Settings.create().strength(2.0f));
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BreederBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> givenType) {
		return world.isClient || givenType != ModBlockEntities.BREEDER ? null
				: (BlockEntityTicker<T>) (w, p, st, entity) ->
				BreederBlockEntity.tickServer(w, p, st, (BreederBlockEntity) entity);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof BreederBlockEntity breeder) {
				ItemScatterer.spawn(world, pos, breeder);
				world.updateComparators(pos, this);
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (!(world.getBlockEntity(pos) instanceof BreederBlockEntity breeder)) {
			return ActionResult.PASS;
		}

		ItemStack held = player.getStackInHand(hand);

		// 手持收容鸡且非潜行：尝试放入繁殖箱空位
		if (!held.isEmpty() && !player.isSneaking()
				&& held.getItem() instanceof CapturedChickenItem && canAcceptCapture(held)) {
			for (int s = BreederBlockEntity.SLOT_CHICKEN_1; s <= BreederBlockEntity.SLOT_NEXT_LAST; s++) {
				if (breeder.getStack(s).isEmpty()) {
					breeder.setStack(s, held.split(1));
					return ActionResult.CONSUME;
				}
			}
		}

		// 空手且潜行：从繁殖箱取出收容鸡
		if (held.isEmpty() && player.isSneaking()) {
			for (int s = BreederBlockEntity.SLOT_NEXT_LAST; s >= BreederBlockEntity.SLOT_CHICKEN_1; s--) {
				ItemStack st = breeder.getStack(s);
				if (!st.isEmpty()) {
					ItemStack give = st.copy();
					breeder.setStack(s, ItemStack.EMPTY);
					player.getInventory().insertStack(give);
					return ActionResult.CONSUME;
				}
			}
		}

		player.openHandledScreen((NamedScreenHandlerFactory) breeder);
		return ActionResult.CONSUME;
	}

	private static boolean canAcceptCapture(ItemStack stack) {
		NbtCompound c = ChickenSpawnEggItem.readCaptureCompound(stack);
		return c != null && !c.isEmpty();
	}
}
