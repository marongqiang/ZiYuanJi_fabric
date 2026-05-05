package com.setycz.chickens.world.item;

import com.setycz.chickens.data.CapturedChickenModelIndex;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.world.ChickenAttributeTicks;
import com.setycz.chickens.world.ChickenUiContext;
import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 物品形态的鸡（对齐 More Chickens {@code ChickenItem}：{@code stacksTo(1)}、对被点方块一格 {@code above()} 再放鸡）。
 * 捕获数据在 {@link ChickenSpawnEggItem#NBT_CAPTURE}；放置仅用 {@link ChickensChickenEntity#readCustomDataFromNbt}，
 * 避免刷怪蛋路径 {@code setChickenTypeId} 重置产蛋计时等。
 */
public final class CapturedChickenItem extends Item {
	public CapturedChickenItem(Settings settings) {
		super(settings);
	}

	public static void packFromEntity(ChickensChickenEntity chicken, ItemStack stack) {
		NbtCompound cap = new NbtCompound();
		chicken.writeCustomDataToNbt(cap);
		ChickenSpawnEggItem.writeCapture(stack, cap);
		int cmd = CapturedChickenModelIndex.customModelData(chicken.getChickenTypeId());
		if (cmd > 0) {
			stack.getOrCreateNbt().putInt("CustomModelData", cmd);
		} else {
			stack.getOrCreateNbt().remove("CustomModelData");
		}
	}

	/**
	 * 创造模式 / EMI 索引用：与成年鸡默认属性等价的 {@link ChickenSpawnEggItem#NBT_CAPTURE}，非游戏内击鸡掉落物。
	 */
	public static ItemStack createDisplayStack(Identifier typeId) {
		ItemStack stack = new ItemStack(ModItems.CAPTURED_CHICKEN);
		NbtCompound cap = new NbtCompound();
		cap.putString("Type", typeId.toString());
		cap.putInt("Growth", 1);
		cap.putInt("Gain", 1);
		cap.putInt("Strength", 1);
		cap.putInt("EggTime", 20 * 60);
		ChickenSpawnEggItem.writeCapture(stack, cap);
		int cmd = CapturedChickenModelIndex.customModelData(typeId);
		if (cmd > 0) {
			stack.getOrCreateNbt().putInt("CustomModelData", cmd);
		} else {
			stack.getOrCreateNbt().remove("CustomModelData");
		}
		return stack;
	}

	private static Identifier readTypeFromCapture(ItemStack stack) {
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap != null && cap.contains("Type")) {
			try {
				return new Identifier(cap.getString("Type"));
			} catch (Exception ignored) {
			}
		}
		return ChickenTypes.defaultId();
	}

	@Override
	public Text getName(ItemStack stack) {
		Identifier id = readTypeFromCapture(stack);
		return Text.translatable(
				"item.chickens.captured_chicken.named",
				Text.translatable(ChickenTypes.getOrDefault(id).nameKey()));
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || cap.isEmpty()) {
			tooltip.add(Text.translatable("item.chickens.captured_chicken.empty").formatted(Formatting.DARK_GRAY));
			return;
		}
		int grow = ChickenAttributeTicks.clampStat(cap.contains("Growth") ? cap.getInt("Growth") : 1);
		int lay = ChickenAttributeTicks.clampStat(cap.contains("Gain") ? cap.getInt("Gain") : 1);
		int breed = ChickenAttributeTicks.clampStat(cap.contains("Strength") ? cap.getInt("Strength") : 1);
		boolean machineDetail = world != null && world.isClient && ChickenUiContext.isRenderingCapturedChickenTooltips();
		if (machineDetail) {
			int growTotal = ChickenAttributeTicks.maturationTotalTicks(grow);
			int growRem = clampRemTicksToCap(
					isBabyCapture(cap) ? Math.max(0, -cap.getInt("Age")) : 0,
					growTotal);
			int layTotal = ChickenAttributeTicks.layIntervalTicksForDisplay(lay);
			int layRemRaw = (!isBabyCapture(cap) && cap.contains("EggTime")) ? Math.max(0, cap.getInt("EggTime")) : 0;
			// 繁殖箱等不向物品写 EggTime 时 NBT 可能远大于当前周期；钳制避免「548/20 秒」类显示
			int layRem = clampRemTicksToCap(layRemRaw, layTotal);
			int breedTotal = ChickenAttributeTicks.breedCooldownTicks(breed);
			int breedRem = clampRemTicksToCap(
					cap.contains("BreedCooldown") ? Math.max(0, cap.getInt("BreedCooldown")) : 0,
					breedTotal);
			tooltip.add(Text.translatable(
					"item.chickens.captured_chicken.detail_grow",
					grow, ceilTicksToSec(growRem), ceilTicksToSec(growTotal)).formatted(Formatting.GRAY));
			tooltip.add(Text.translatable(
					"item.chickens.captured_chicken.detail_lay",
					lay, ceilTicksToSec(layRem), ceilTicksToSec(layTotal)).formatted(Formatting.GRAY));
			tooltip.add(Text.translatable(
					"item.chickens.captured_chicken.detail_breed",
					breed, ceilTicksToSec(breedRem), ceilTicksToSec(breedTotal)).formatted(Formatting.GRAY));
		} else {
			tooltip.add(ChickenAttributeTicks.timingLineGrow(grow).formatted(Formatting.GRAY));
			tooltip.add(ChickenAttributeTicks.timingLineLay(lay).formatted(Formatting.GRAY));
			tooltip.add(ChickenAttributeTicks.timingLineBreed(breed).formatted(Formatting.GRAY));
			if (!isBabyCapture(cap) && cap.contains("EggTime") && cap.getInt("EggTime") > 0) {
				tooltip.add(Text.translatable(
						"item.chickens.captured_chicken.egg_remaining",
						ChickenAttributeTicks.durationCompactForTooltip(cap.getInt("EggTime"))).formatted(Formatting.GRAY));
			}
			if (cap.contains("BreedCooldown") && cap.getInt("BreedCooldown") > 0) {
				tooltip.add(Text.translatable(
						"item.chickens.captured_chicken.breed_remaining",
						ChickenAttributeTicks.durationCompactForTooltip(cap.getInt("BreedCooldown"))).formatted(Formatting.DARK_GRAY));
			}
		}
		tooltip.add(Text.translatable("item.chickens.captured_chicken.hint_place").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		ItemStack stack = context.getStack();
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || cap.isEmpty()) {
			return ActionResult.FAIL;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		// More Chickens：固定在被点击方块顶上生成，不按点击面偏移
		BlockPos pos = context.getBlockPos().up();

		ChickensChickenEntity entity = ModEntities.CHICKENS_CHICKEN.create(serverWorld);
		if (entity == null) {
			return ActionResult.FAIL;
		}

		entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, serverWorld.random.nextFloat() * 360.0F, 0.0F);
		entity.initialize(serverWorld, serverWorld.getLocalDifficulty(pos), SpawnReason.SPAWN_EGG, null, null);
		entity.readCustomDataFromNbt(cap);

		serverWorld.spawnEntity(entity);

		if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}

	private static boolean isBabyCapture(NbtCompound cap) {
		return cap.contains("Age") && cap.getInt("Age") < 0;
	}

	private static int clampRemTicksToCap(int remTicks, int totalTicks) {
		if (totalTicks <= 0) {
			return 0;
		}
		return Math.min(Math.max(0, remTicks), totalTicks);
	}

	private static int ceilTicksToSec(int ticks) {
		if (ticks <= 0) {
			return 0;
		}
		return (ticks + 19) / 20;
	}

	/** 与实体 {@code eggLayTime} 类似：仅在鸡舍/繁殖箱界面绘制时，根据 {@code EggTime} 显示槽下进度条 */
	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		if (!ChickenUiContext.isRenderingCapturedChickenTooltips()) {
			return false;
		}
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		return cap != null && !isBabyCapture(cap) && cap.contains("EggTime") && cap.getInt("EggTime") > 0;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		NbtCompound cap = ChickenSpawnEggItem.readCaptureCompound(stack);
		if (cap == null || !cap.contains("EggTime")) {
			return 0;
		}
		int gain = ChickenAttributeTicks.clampStat(cap.contains("Gain") ? cap.getInt("Gain") : 1);
		int interval = ChickenAttributeTicks.layIntervalTicksForDisplay(gain);
		int rem = clampRemTicksToCap(Math.max(0, cap.getInt("EggTime")), interval);
		return (int) (13.0 * rem / Math.max(1, interval));
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return 0xFF00CC44;
	}
}
