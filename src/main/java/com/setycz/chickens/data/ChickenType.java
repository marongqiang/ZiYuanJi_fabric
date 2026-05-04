package com.setycz.chickens.data;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;

public record ChickenType(
		Identifier id,
		String nameKey,
		Identifier texture,
		int bgColor,
		int fgColor,
		ItemStack layItem,
		ItemStack dropItem,
		float layCoefficient,
		SpawnType spawnType,
		@Nullable Identifier parent1,
		@Nullable Identifier parent2
) {
	public boolean isBase() {
		return parent1 == null || parent2 == null;
	}

	public boolean canSpawnNaturally() {
		return isBase() && spawnType != SpawnType.NONE;
	}

	public boolean isChildOf(Identifier p1, Identifier p2) {
		return parent1 != null
				&& parent2 != null
				&& (parent1.equals(p1) && parent2.equals(p2) || parent1.equals(p2) && parent2.equals(p1));
	}
}

