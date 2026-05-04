package com.setycz.chickens.client;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.client.render.ChickensChickenRenderer;
import com.setycz.chickens.client.screen.BreederScreen;
import com.setycz.chickens.client.screen.HenhouseScreen;
import com.setycz.chickens.compat.CompatLoader;
import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.registry.ModScreenHandlers;
import com.setycz.chickens.world.item.LiquidEggItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@SuppressWarnings("deprecation")
public final class ChickensFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChickenItemColors.register();
		FabricModelPredicateProviderRegistry.register(
				ModItems.LIQUID_EGG,
				ChickensFabricMod.id("liquid_variant"),
				(stack, world, holder, seed) -> LiquidEggItem.readLiquid(stack) == Blocks.LAVA ? 1.0F : 0.0F);
		EntityRendererRegistry.register(ModEntities.CHICKENS_CHICKEN, ChickensChickenRenderer::new);
		HandledScreens.register(ModScreenHandlers.HENHOUSE, HenhouseScreen::new);
		HandledScreens.register(ModScreenHandlers.BREEDER, BreederScreen::new);
		CompatLoader.initClient();
	}
}

