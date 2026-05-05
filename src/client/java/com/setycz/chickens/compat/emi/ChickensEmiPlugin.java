package com.setycz.chickens.compat.emi;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.item.CapturedChickenItem;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@EmiEntrypoint
public final class ChickensEmiPlugin implements EmiPlugin {
	public static final EmiRecipeCategory LAYING = new EmiRecipeCategory(
			ChickensFabricMod.id("emi/laying"),
			EmiStack.of(new ItemStack(ModItems.SPAWN_EGG))
	);

	public static final EmiRecipeCategory BREEDING = new EmiRecipeCategory(
			ChickensFabricMod.id("emi/breeding"),
			EmiStack.of(CapturedChickenItem.createDisplayStack(ChickenTypes.defaultId()))
	);

	@Override
	public void register(EmiRegistry registry) {
		// 不按 NBT 比较时，同 id 物品会串味：刷怪蛋筛下蛋配方；收容鸡筛繁殖配方
		registry.setDefaultComparison(ModItems.SPAWN_EGG, Comparison.compareNbt());
		registry.setDefaultComparison(ModItems.CAPTURED_CHICKEN, Comparison.compareNbt());

		registry.addCategory(LAYING);
		registry.addCategory(BREEDING);

		for (ChickenType type : ChickenTypes.all()) {
			registry.addRecipe(new LayingEmiRecipe(type));

			if (type.parent1() != null && type.parent2() != null) {
				registry.addRecipe(new BreedingEmiRecipe(type));
			}
		}
		// 各鸡刷怪蛋勿再 addEmiStack：创造模式物品栏（ModItemGroups）已列出全部变体，EMI 会索引一遍；
		// 再注册会与索引重复，出现两个「沙子鸡刷怪蛋」等。
	}

	static ItemStack eggStack(Identifier chickenType) {
		return ModItems.spawnEggStackForChickenType(chickenType);
	}

	static ItemStack pocketStack(Identifier chickenType) {
		return CapturedChickenItem.createDisplayStack(chickenType);
	}
}

