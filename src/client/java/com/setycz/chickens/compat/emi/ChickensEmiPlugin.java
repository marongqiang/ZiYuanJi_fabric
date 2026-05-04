package com.setycz.chickens.compat.emi;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.world.item.ChickenSpawnEggItem;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@EmiEntrypoint
public final class ChickensEmiPlugin implements EmiPlugin {
	public static final EmiRecipeCategory LAYING = new EmiRecipeCategory(
			ChickensFabricMod.id("emi/laying"),
			EmiStack.of(new ItemStack(ModItems.SPAWN_EGG))
	);

	public static final EmiRecipeCategory BREEDING = new EmiRecipeCategory(
			ChickensFabricMod.id("emi/breeding"),
			EmiStack.of(new ItemStack(ModItems.CHICKEN_CATCHER))
	);

	@Override
	public void register(EmiRegistry registry) {
		registry.addCategory(LAYING);
		registry.addCategory(BREEDING);

		for (ChickenType type : ChickenTypes.all()) {
			registry.addRecipe(new LayingEmiRecipe(type));

			if (type.parent1() != null && type.parent2() != null) {
				registry.addRecipe(new BreedingEmiRecipe(type));
			}
		}
	}

	static ItemStack eggStack(Identifier chickenType) {
		ItemStack stack = new ItemStack(ModItems.SPAWN_EGG);
		ChickenSpawnEggItem.writeType(stack, chickenType);
		stack.setCustomName(Text.translatable("emi.chickens.chicken", chickenType.toString()));
		return stack;
	}
}

