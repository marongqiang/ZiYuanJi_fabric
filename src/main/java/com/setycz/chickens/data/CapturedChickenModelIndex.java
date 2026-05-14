package com.setycz.chickens.data;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收容鸡物品的 CustomModelData 映射，与 Gradle 生成的 {@code captured_chicken.json} 覆盖条目顺序一致（按 Identifier 字典序）。
 */
public final class CapturedChickenModelIndex {
	private CapturedChickenModelIndex() {}

	private static Map<Identifier, Integer> cmdMap = new HashMap<>();

	public static synchronized void rebuild(Collection<ChickenType> types) {
		List<ChickenType> sorted = new ArrayList<>(types);
		sorted.sort(Comparator.comparing(t -> t.id().toString()));
		Map<Identifier, Integer> map = new HashMap<>(sorted.size());
		int cmd = 1;
		for (ChickenType t : sorted) {
			map.put(t.id(), cmd++);
		}
		cmdMap = map;
	}

	public static int customModelData(Identifier chickenId) {
		return cmdMap.getOrDefault(chickenId, 0);
	}

	public static int capturedVariantCount() {
		return cmdMap.size();
	}
}
