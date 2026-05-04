package com.setycz.chickens.compat.jade;

/**
 * 通过 {@code CompatLoader} 反射调用，避免未安装 Jade 时类加载崩溃。
 */
public final class JadeCompat {
	private JadeCompat() {}

	public static void initClient() {
		// 兼容由 Jade 自身的 @WailaPlugin 扫描驱动，这里只需确保类被打包即可。
	}
}

