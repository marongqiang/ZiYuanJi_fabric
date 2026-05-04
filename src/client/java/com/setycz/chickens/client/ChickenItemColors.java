package com.setycz.chickens.client;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import com.setycz.chickens.world.item.ColoredEggItem;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * 刷怪蛋与(Resource Chickens 数据里的)产蛋块上蛋共用同一套画法：原版 {@code spawn_egg}/{@code spawn_egg_overlay}
 * + 每条鸡类型上的 {@code bg_color}/{@code fg_color}，视觉上与 RC 的蛋色字段一致。
 */
public final class ChickenItemColors {
	private ChickenItemColors() {}

	public static void register() {
		ColorProviderRegistry.ITEM.register(ChickenItemColors::chickenSpawnEgg, ModItems.SPAWN_EGG);
		ColorProviderRegistry.ITEM.register((s, i) -> fixedSpawnEgg(ChickensFabricMod.id("cobblestone"), i), ModItems.COBBLESTONE_CHICKEN_SPAWN_EGG);
		ColorProviderRegistry.ITEM.register((s, i) -> fixedSpawnEgg(ChickensFabricMod.id("stone"), i), ModItems.STONE_CHICKEN_SPAWN_EGG);
		ColorProviderRegistry.ITEM.register((s, i) -> fixedSpawnEgg(ChickensFabricMod.id("smooth_stone"), i), ModItems.SMOOTH_STONE_CHICKEN_SPAWN_EGG);
		ColorProviderRegistry.ITEM.register(ChickenItemColors::coloredLayEgg, ModItems.COLORED_EGG);
	}

	private static int chickenSpawnEgg(ItemStack stack, int tintIndex) {
		Identifier id = ChickenSpawnEggItem.readType(stack);
		return tintFromType(ChickenTypes.get(id), tintIndex);
	}

	private static int fixedSpawnEgg(Identifier typeId, int tintIndex) {
		return tintFromType(ChickenTypes.get(typeId), tintIndex);
	}

	private static int coloredLayEgg(ItemStack stack, int tintIndex) {
		Identifier id = ColoredEggItem.readType(stack);
		return tintFromType(ChickenTypes.get(id), tintIndex);
	}

	private static int tintFromType(ChickenType type, int tintIndex) {
		if (type == null) {
			return opaque(0xFFFFFF);
		}
		int rgb = tintIndex == 0 ? type.bgColor() : type.fgColor();
		return opaque(rgb);
	}

	private static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}
}
