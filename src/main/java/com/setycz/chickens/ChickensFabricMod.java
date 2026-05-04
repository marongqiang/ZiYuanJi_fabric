package com.setycz.chickens;

import com.setycz.chickens.compat.CompatLoader;
import com.setycz.chickens.data.ChickenTypeManager;
import com.setycz.chickens.registry.ModEntities;
import com.setycz.chickens.registry.ModItems;
import com.setycz.chickens.registry.ModBlocks;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.registry.ModItemGroups;
import com.setycz.chickens.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChickensFabricMod implements ModInitializer {
	public static final String MODID = "chickens";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static Identifier id(String path) {
		return new Identifier(MODID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModScreenHandlers.init();
		ModItemGroups.init();

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ChickenTypeManager());
		ChickenTeachHandler.init();

		CompatLoader.initCommon();
		LOGGER.info("Chickens (Fabric 1.20.1) 初始化完成");
	}
}

