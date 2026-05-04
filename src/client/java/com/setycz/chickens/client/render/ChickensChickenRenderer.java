package com.setycz.chickens.client.render;

import com.setycz.chickens.world.entity.ChickensChickenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ChickenEntityRenderer;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.util.Identifier;

public final class ChickensChickenRenderer extends ChickenEntityRenderer {
	public ChickensChickenRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(ChickenEntity entity) {
		if (entity instanceof ChickensChickenEntity chickens) {
			return chickens.getChickenType().texture();
		}
		return super.getTexture(entity);
	}
}

