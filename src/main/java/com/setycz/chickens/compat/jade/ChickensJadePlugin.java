package com.setycz.chickens.compat.jade;

import com.setycz.chickens.world.ChickenAttributeTicks;
import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ChickensJadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerEntityComponent(Provider.INSTANCE, ChickensChickenEntity.class);
	}

	private static final class Provider implements IEntityComponentProvider {
		private static final Provider INSTANCE = new Provider();

		@Override
		public Identifier getUid() {
			return new Identifier("chickens", "entity_stats");
		}

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			if (!(accessor.getEntity() instanceof ChickensChickenEntity chicken)) return;

			tooltip.add(Text.translatable("jade.chickens.tier", chicken.getTier()));
			tooltip.add(ChickenAttributeTicks.timingLineGrow(chicken.getGrowth()));
			tooltip.add(ChickenAttributeTicks.timingLineLay(chicken.getGain()));
			tooltip.add(ChickenAttributeTicks.timingLineBreed(chicken.getStrength()));

			if (!chicken.isBaby()) {
				int minutes = chicken.getEggProgressMinutes();
				if (minutes <= 0) {
					tooltip.add(Text.translatable("jade.chickens.next_egg"));
				} else {
					tooltip.add(Text.translatable("jade.chickens.egg_progress", minutes));
				}
			}
		}
	}
}

