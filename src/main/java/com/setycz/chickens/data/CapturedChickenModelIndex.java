package com.setycz.chickens.data;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** 收容鸡物品的 CustomModelData，与 Gradle 生成的 {@code captured_chicken.json} 覆盖条目顺序一致（按 Identifier 字典序）。 */
public final class CapturedChickenModelIndex {
	private CapturedChickenModelIndex() {}

	private static final Identifier[] EMPTY = new Identifier[0];

	private static Identifier[] orderedIds = EMPTY;
	private static int[] customModelForOrdinal = new int[0];

	public static synchronized void rebuild(Collection<ChickenType> types) {
		List<ChickenType> sorted = new ArrayList<>(types);
		sorted.sort(Comparator.comparing(t -> t.id().toString()));

		int n = sorted.size();
		orderedIds = new Identifier[n];
		customModelForOrdinal = new int[n];
		for (int i = 0; i < n; i++) {
			orderedIds[i] = sorted.get(i).id();
			customModelForOrdinal[i] = i + 1;
		}
	}

	/** Vanilla {@code CustomModelData} predicate；无匹配时返回 0，使用收容鸡默认模型（生鸡肉占位）。 */
	public static int customModelData(Identifier chickenId) {
		for (int i = 0; i < orderedIds.length; i++) {
			if (orderedIds[i].equals(chickenId)) {
				return customModelForOrdinal[i];
			}
		}
		return 0;
	}

	public static int capturedVariantCount() {
		return orderedIds.length;
	}
}
