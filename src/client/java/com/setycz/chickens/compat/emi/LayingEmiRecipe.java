package com.setycz.chickens.compat.emi;

import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.world.ChickenAttributeTicks;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class LayingEmiRecipe extends BasicEmiRecipe {
	private final ChickenType type;

	public LayingEmiRecipe(ChickenType type) {
		super(ChickensEmiPlugin.LAYING, id(type), 90, 26);
		this.type = type;

		this.inputs.add(EmiStack.of(ChickensEmiPlugin.eggStack(type.id())));
		this.outputs.add(EmiStack.of(type.layItem()));
	}

	private static Identifier id(ChickenType type) {
		return new Identifier(type.id().getNamespace(), "emi/laying/" + type.id().getPath());
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(inputs.get(0), 0, 4);
		widgets.addFillingArrow(24, 5, 2000);
		widgets.addSlot(outputs.get(0), 54, 4).recipeContext(this);

		String shortLay = ChickenAttributeTicks.durationText(ChickenAttributeTicks.layIntervalTicksForDisplay(10)).getString();
		String longLay = ChickenAttributeTicks.durationText(ChickenAttributeTicks.layIntervalTicksForDisplay(1)).getString();
		widgets.addText(Text.translatable("emi.chickens.laying_interval", shortLay, longLay), 0, 18, 0x404040, false);
	}
}

