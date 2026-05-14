package com.setycz.chickens.world.item;

import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

/**
 * 对齐 More Chickens Catcher：成鸡收成时在地上生成「物品鸡」实体掉落（朝上初速），先入世界再捡起；
 * 幼鸡冒烟、hurt 音效、消耗耐久且不收。
 */
public final class ChickenCatcherItem extends Item {
	public ChickenCatcherItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.chickens.chicken_catcher.tooltip1").formatted(Formatting.GRAY));
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		World world = user.getWorld();
		if (!(entity instanceof ChickensChickenEntity chicken)) {
			return ActionResult.PASS;
		}
		if (!chicken.getPassengerList().isEmpty() || chicken.hasVehicle()) {
			return ActionResult.FAIL;
		}

		if (world.isClient) {
			var rand = world.random;
			double x = entity.getX();
			double y = entity.getBodyY(0.5);
			double z = entity.getZ();
			if (chicken.isBaby()) {
				for (int k = 0; k < 20; k++) {
					world.addParticle(
							ParticleTypes.SMOKE,
							x + (rand.nextDouble() * 0.6 - 0.3),
							y + rand.nextDouble() * 0.6,
							z + (rand.nextDouble() * 0.6 - 0.3),
							rand.nextGaussian() * 0.02,
							rand.nextGaussian() * 0.02,
							rand.nextGaussian() * 0.02);
				}
			} else {
				for (int k = 0; k < 20; k++) {
					world.addParticle(
							ParticleTypes.POOF,
							x + (rand.nextDouble() * 0.6 - 0.3),
							y + rand.nextDouble() * 0.6,
							z + (rand.nextDouble() * 0.6 - 0.3),
							rand.nextGaussian() * 0.02,
							rand.nextGaussian() * 0.2,
							rand.nextGaussian() * 0.02);
				}
			}
			return ActionResult.SUCCESS;
		}

		if (chicken.isBaby()) {
			world.playSound(user, chicken.getBlockPos(), SoundEvents.ENTITY_CHICKEN_HURT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
			if (!user.isCreative()) {
				damageTool(stack, user, hand);
			}
			user.sendMessage(Text.translatable("message.chickens.catcher.baby"), true);
			return ActionResult.SUCCESS;
		}

		ItemStack pocket = new ItemStack(ModItems.CAPTURED_CHICKEN);
		CapturedChickenItem.packFromEntity(chicken, pocket);

		world.playSound(user, chicken.getBlockPos(), SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.NEUTRAL, 1.0F, 1.0F);

		ItemEntity drop = new ItemEntity(world, chicken.getX(), chicken.getY(), chicken.getZ(), pocket);
		drop.setVelocity(0.0D, 0.2D, 0.0D);
		world.spawnEntity(drop);
		chicken.discard();

		if (!user.isCreative()) {
			damageTool(stack, user, hand);
		}
		return ActionResult.SUCCESS;
	}

	private static void damageTool(ItemStack stack, PlayerEntity user, Hand hand) {
		if (user instanceof ServerPlayerEntity sp) {
			stack.damage(1, sp, p -> p.sendToolBreakStatus(hand));
		} else {
			stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
		}
	}
}
