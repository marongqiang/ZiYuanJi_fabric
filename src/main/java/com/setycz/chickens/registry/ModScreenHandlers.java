package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensFabricMod;
import com.setycz.chickens.world.screen.BreederScreenHandler;
import com.setycz.chickens.world.screen.HenhouseScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
	private ModScreenHandlers() {}

	public static final ScreenHandlerType<HenhouseScreenHandler> HENHOUSE = Registry.register(
			Registries.SCREEN_HANDLER,
			ChickensFabricMod.id("henhouse"),
			new ScreenHandlerType<>(HenhouseScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
	);

	public static final ScreenHandlerType<BreederScreenHandler> BREEDER = Registry.register(
			Registries.SCREEN_HANDLER,
			ChickensFabricMod.id("breeder"),
			new ScreenHandlerType<>(BreederScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
	);

	public static void init() {
		// classload triggers registrations
	}
}

