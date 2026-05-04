package com.setycz.chickens.world.item;

import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 彩色蛋外观的产物；生成鸡的方式与 {@link ChickenSpawnEggItem} 相同（仅对方块使用），不可投掷孵鸡。
 */
public final class ColoredEggItem extends Item {
	public static final String NBT_CHICKEN_TYPE = "Type";

	public ColoredEggItem(Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		ChickenType type = ChickenTypes.getOrDefault(readType(stack));
		return Text.translatable("item.chickens.colored_egg.named", Text.translatable(type.nameKey()));
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.chickens.colored_egg.tooltip").formatted(Formatting.GRAY));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		return ChickenSpawnEggItem.spawnChickenAt(context, readType(context.getStack()));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return TypedActionResult.pass(user.getStackInHand(hand));
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
	}
}
