package com.setycz.chickens.compat;

import com.setycz.chickens.ChickensFabricMod;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatLoader {
	private CompatLoader() {}

	public static void initCommon() {
		// EMI 通过 @EmiEntrypoint 注解自动发现，无需在此处手动初始化
	}

	public static void initClient() {
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			safeRun("Jade", "com.setycz.chickens.compat.jade.JadeCompat", "initClient");
		}
	}

	private static void safeRun(String name, String className, String methodName) {
		try {
			Class<?> clazz = Class.forName(className);
			clazz.getMethod(methodName).invoke(null);
			ChickensFabricMod.LOGGER.info("{} 兼容已启用", name);
		} catch (Throwable t) {
			ChickensFabricMod.LOGGER.warn("{} 兼容加载失败（可忽略，但该功能将不可用）", name, t);
		}
	}
}

