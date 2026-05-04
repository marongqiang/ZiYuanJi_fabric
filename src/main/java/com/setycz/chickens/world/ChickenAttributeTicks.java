package com.setycz.chickens.world;

import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
/**
 * 三项 NBT/实体属性（沿用键名 {@code Growth}、{@code Gain}、{@code Strength}）表示等级 1–10：
 * <ul>
 *     <li>Growth：幼体长成成年所需时间（越高级越快）</li>
 *     <li>Gain：两次下蛋之间的间隔（越高级越短）</li>
 *     <li>Strength：繁殖后的交配冷却（越高级越短）</li>
 * </ul>
 * 10 级对应最短 20 秒（400 tick）；1 级对应原版鸡该机制的参考时长。
 */
public final class ChickenAttributeTicks {
	public static final int MIN_TICKS = 20 * 20;
	/** 原版鸡一次产蛋周期：6000–12000 tick，取中值作 1 级线性插值基准 */
	public static final int VANILLA_LAY_INTERVAL_MEDIAN = 9000;
	/** 原版繁殖后冷却 {@link net.minecraft.entity.passive.AnimalEntity#BREEDING_COOLDOWN} */
	public static final int VANILLA_BREED_COOLDOWN = 6000;
	/** 幼鸡从 {@link PassiveEntity#BABY_AGE} 长到成年的 tick 数 */
	public static final int VANILLA_BABY_MATURATION_TICKS = -PassiveEntity.BABY_AGE;

	private ChickenAttributeTicks() {}

	public static int clampStat(int v) {
		if (v < 1) return 1;
		if (v > 10) return 10;
		return v;
	}

	/**
	 * 从「原版量级」线性插值到 {@link #MIN_TICKS}；{@code stat==1} 为原版参考，{@code stat==10} 为 20 秒。
	 */
	public static int ticksForStat(int stat, int vanillaTicks) {
		stat = clampStat(stat);
		if (stat >= 10) return MIN_TICKS;
		return vanillaTicks - (vanillaTicks - MIN_TICKS) * (stat - 1) / 9;
	}

	/** 下一次产蛋间隔（成鸡）；Gain 越高越短 */
	public static int nextLayCooldownTicks(Random random, int gainStat) {
		int ref = VANILLA_LAY_INTERVAL_MEDIAN;
		if (clampStat(gainStat) <= 1) {
			ref = 6000 + random.nextInt(6000);
		}
		return Math.max(MIN_TICKS, ticksForStat(gainStat, ref));
	}

	/** 用于 EMI 等展示：无随机，取中值 */
	public static int layIntervalTicksForDisplay(int gainStat) {
		return Math.max(MIN_TICKS, ticksForStat(gainStat, VANILLA_LAY_INTERVAL_MEDIAN));
	}

	public static int breedCooldownTicks(int strengthStat) {
		return Math.max(MIN_TICKS, ticksForStat(strengthStat, VANILLA_BREED_COOLDOWN));
	}

	/** 幼体长成成年总时长（tick）；Growth 越高总时长越短 */
	public static int maturationTotalTicks(int growthStat) {
		return Math.max(MIN_TICKS, ticksForStat(growthStat, VANILLA_BABY_MATURATION_TICKS));
	}

	/**
	 * 每游戏 tick 在原版 +1 年龄之外，再额外增加的年龄单位（幼体 {@code breedingAge} 递增至 0）。
	 */
	public static int extraBabyAgePerTick(int growthStat) {
		int total = maturationTotalTicks(growthStat);
		int per = Math.max(1, (VANILLA_BABY_MATURATION_TICKS + total - 1) / total);
		return Math.max(0, per - 1);
	}

	public static MutableText durationText(int ticks) {
		float sec = ticks / 20.0f;
		if (sec >= 90.0f) {
			float min = sec / 60.0f;
			return Text.translatable("item.chickens.duration_minutes", String.format("%.1f", min));
		}
		return Text.translatable("item.chickens.duration_seconds", String.format("%.1f", sec));
	}

	/** 带单位的短字符串，用于「生长：N级，…」一行内嵌（Jade/收容物品等） */
	public static String durationCompactForTooltip(int ticks) {
		float sec = ticks / 20.0f;
		if (sec >= 90.0f) {
			float min = sec / 60.0f;
			String m = (Math.abs(min - Math.round(min)) < 0.001f)
					? String.valueOf(Math.round(min))
					: String.format("%.1f", min);
			return Text.translatable("item.chickens.duration_minutes_compact", m).getString();
		}
		String s = (ticks % 20 == 0) ? String.valueOf(ticks / 20) : String.format("%.1f", sec);
		return Text.translatable("item.chickens.duration_seconds_compact", s).getString();
	}

	public static MutableText timingLineGrow(int growthStat) {
		int lv = clampStat(growthStat);
		String time = durationCompactForTooltip(maturationTotalTicks(lv));
		return Text.translatable("item.chickens.captured_chicken.timing_grow", lv, time);
	}

	public static MutableText timingLineLay(int gainStat) {
		int lv = clampStat(gainStat);
		String time = durationCompactForTooltip(layIntervalTicksForDisplay(lv));
		return Text.translatable("item.chickens.captured_chicken.timing_lay", lv, time);
	}

	public static MutableText timingLineBreed(int strengthStat) {
		int lv = clampStat(strengthStat);
		String time = durationCompactForTooltip(breedCooldownTicks(lv));
		return Text.translatable("item.chickens.captured_chicken.timing_breed", lv, time);
	}
}
