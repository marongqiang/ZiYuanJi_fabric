package com.setycz.chickens.compat.emi;

import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BreedingEmiRecipe extends BasicEmiRecipe {
	private final ChickenType type;

	public BreedingEmiRecipe(ChickenType type) {
		super(ChickensEmiPlugin.BREEDING, id(type), 110, 26);
		this.type = type;

		Identifier p1 = type.parent1();
		Identifier p2 = type.parent2();
		if (p1 == null || p2 == null) {
			throw new IllegalArgumentException("Breeding recipe requires parents");
		}

		this.inputs.add(EmiStack.of(ChickensEmiPlugin.eggStack(p1)));
		this.inputs.add(EmiStack.of(ChickensEmiPlugin.eggStack(p2)));
		this.outputs.add(EmiStack.of(ChickensEmiPlugin.eggStack(type.id())));
	}

	private static Identifier id(ChickenType type) {
		return new Identifier(type.id().getNamespace(), "emi/breeding/" + type.id().getPath());
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(inputs.get(0), 0, 4);
		widgets.addSlot(inputs.get(1), 18, 4);
		widgets.addFillingArrow(44, 5, 2000);
		widgets.addSlot(outputs.get(0), 74, 4).recipeContext(this);

		float chance = ChickenTypes.childChancePercent(type.id());
		widgets.addText(Text.translatable("emi.chickens.breeding_chance", String.format("%.1f", chance)), 0, 18, 0x404040, false);
	}
}

