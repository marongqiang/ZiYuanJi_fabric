package com.setycz.chickens.world.entity;

import com.setycz.chickens.data.ChickenType;
import com.setycz.chickens.data.ChickenTypes;
import com.setycz.chickens.data.SpawnType;
import com.setycz.chickens.world.ChickenAttributeTicks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ChickensChickenEntity extends ChickenEntity {
	private static final TrackedData<String> TRACKED_TYPE = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.STRING);
	/** 幼体长成速度等级（1 最慢≈原版，10 最快 20 秒） */
	private static final TrackedData<Integer> TRACKED_GROWTH = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** 下蛋间隔等级 */
	private static final TrackedData<Integer> TRACKED_GAIN = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** 繁殖交配冷却等级 */
	private static final TrackedData<Integer> TRACKED_STRENGTH = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> TRACKED_EGG_MINUTES = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** 距离下一次模组产蛋剩余的 tick（同步到客户端）；供 Jade 等读取原版 eggLayTime 显示用 */
	private static final TrackedData<Integer> TRACKED_LAY_TICKS = DataTracker.registerData(ChickensChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final String NBT_TYPE = "Type";
	private static final String NBT_GROWTH = "Growth";
	private static final String NBT_GAIN = "Gain";
	private static final String NBT_STRENGTH = "Strength";
	private static final String NBT_BREED_COOLDOWN = "BreedCooldown";
	private static final String NBT_WATER_TEACH = "WaterTeach";
	private static final String NBT_TOTEM_TEACH = "TotemTeach";
	private static final String NBT_STAR_TEACH = "StarTeach";
	private static final String NBT_LOG_TEACH = "LogTeach";
	private static final String NBT_LOG_TEACH_TARGET = "LogTeachTarget";
	private static final String NBT_DYE_TEACH = "DyeTeach";
	private static final String NBT_DYE_TEACH_TARGET = "DyeTeachTarget";

	private int timeUntilNextEgg = 20 * 60;
	private int waterTeach = 0;
	private int totemTeach = 0;
	private int starTeach = 0;
	private int logTeach = 0;
	@Nullable private Identifier logTeachTarget = null;
	private int dyeTeach = 0;
	@Nullable private Identifier dyeTeachTarget = null;

	public ChickensChickenEntity(EntityType<? extends ChickenEntity> type, World world) {
		super(type, world);
	}

	public Identifier getChickenTypeId() {
		String raw = this.dataTracker.get(TRACKED_TYPE);
		try {
			return new Identifier(raw);
		} catch (Exception ignored) {
			return ChickenTypes.defaultId();
		}
	}

	public void setChickenTypeId(Identifier id) {
		this.dataTracker.set(TRACKED_TYPE, id.toString());
		resetTimeUntilNextEgg();
	}

	public ChickenType getChickenType() {
		return ChickenTypes.getOrDefault(getChickenTypeId());
	}

	public int getWaterTeach() {
		return waterTeach;
	}

	public void setWaterTeach(int waterTeach) {
		this.waterTeach = Math.max(0, waterTeach);
	}

	public int getTotemTeach() {
		return totemTeach;
	}

	public void setTotemTeach(int totemTeach) {
		this.totemTeach = Math.max(0, totemTeach);
	}

	public int getStarTeach() {
		return starTeach;
	}

	public void setStarTeach(int starTeach) {
		this.starTeach = Math.max(0, starTeach);
	}

	public int getLogTeach() {
		return logTeach;
	}

	public void setLogTeach(int logTeach) {
		this.logTeach = Math.max(0, logTeach);
	}

	public @Nullable Identifier getLogTeachTarget() {
		return logTeachTarget;
	}

	public void setLogTeachTarget(@Nullable Identifier target) {
		this.logTeachTarget = target;
	}

	public int getDyeTeach() {
		return dyeTeach;
	}

	public void setDyeTeach(int dyeTeach) {
		this.dyeTeach = Math.max(0, dyeTeach);
	}

	public @Nullable Identifier getDyeTeachTarget() {
		return dyeTeachTarget;
	}

	public void setDyeTeachTarget(@Nullable Identifier target) {
		this.dyeTeachTarget = target;
	}

	@Override
	public Text getName() {
		if (this.hasCustomName()) {
			return super.getName();
		}
		return Text.translatable(getChickenType().nameKey());
	}

	public int getTier() {
		return ChickenTypes.tierOf(getChickenTypeId());
	}

	public int getGrowth() {
		return dataTracker.get(TRACKED_GROWTH);
	}

	public int getGain() {
		return dataTracker.get(TRACKED_GAIN);
	}

	public int getStrength() {
		return dataTracker.get(TRACKED_STRENGTH);
	}

	public int getEggProgressMinutes() {
		return dataTracker.get(TRACKED_EGG_MINUTES);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(TRACKED_TYPE, ChickenTypes.defaultId().toString());
		dataTracker.startTracking(TRACKED_GROWTH, 1);
		dataTracker.startTracking(TRACKED_GAIN, 1);
		dataTracker.startTracking(TRACKED_STRENGTH, 1);
		dataTracker.startTracking(TRACKED_EGG_MINUTES, 0);
		dataTracker.startTracking(TRACKED_LAY_TICKS, 20 * 60);
	}

	@Override
	public void breed(ServerWorld world, AnimalEntity other) {
		super.breed(world, other);
		this.setBreedingAge(ChickenAttributeTicks.breedCooldownTicks(this.getStrength()));
		if (other instanceof ChickensChickenEntity mate) {
			mate.setBreedingAge(ChickenAttributeTicks.breedCooldownTicks(mate.getStrength()));
		}
	}

	@Override
	public void tickMovement() {
		// Vanilla chicken decrements eggLayTime in tickMovement() and drops minecraft:egg.
		// This mod lays `ChickenType.layItem()` separately in tick(); disable the vanilla drops.
		this.eggLayTime = Integer.MAX_VALUE;
		super.tickMovement();
		// Jade 等读 eggLayTime÷20 当“秒”；须在 tickMovement 内与真实冷却一致（原版也在此阶段递减）
		int remain = getWorld().isClient()
			? Math.max(0, dataTracker.get(TRACKED_LAY_TICKS))
			: Math.max(0, timeUntilNextEgg);
		this.eggLayTime = remain > 0 ? remain : 1;
	}

	private void syncLayTicksTracked() {
		if (getWorld().isClient()) {
			return;
		}
		dataTracker.set(TRACKED_LAY_TICKS, Math.max(0, timeUntilNextEgg));
	}

	@Override
	public void tick() {
		World world = this.getWorld();
		boolean serverCountsDown = !world.isClient && !this.isBaby() && !this.hasVehicle();

		// 先递减再 super.tick()，使本 tick 内 tickMovement 读到的冷却与逻辑一致（正常逐 tick 倒计时）
		if (serverCountsDown) {
			timeUntilNextEgg--;
			syncLayTicksTracked();
			if (timeUntilNextEgg % 20 == 0) {
				dataTracker.set(TRACKED_EGG_MINUTES, Math.max(0, timeUntilNextEgg / (20 * 60)));
			}
		}

		super.tick();

		if (!world.isClient && isBaby()) {
			int extra = ChickenAttributeTicks.extraBabyAgePerTick(getGrowth());
			if (extra > 0) {
				setBreedingAge(Math.min(0, getBreedingAge() + extra));
			}
		}

		if (serverCountsDown && timeUntilNextEgg <= 0) {
			layEggs();
			resetTimeUntilNextEgg();
		}
	}

	private void layEggs() {
		ItemStack item = getChickenType().layItem().copy();
		if (!item.isEmpty()) {
			dropStack(item);
		}
		if (getChickenTypeId().equals(ChickenTypes.smartId())) {
			dropStack(new ItemStack(net.minecraft.item.Items.FEATHER));
		}
	}

	private void resetTimeUntilNextEgg() {
		timeUntilNextEgg = ChickenAttributeTicks.nextLayCooldownTicks(this.random, getGain());
		dataTracker.set(TRACKED_EGG_MINUTES, Math.max(0, timeUntilNextEgg / (20 * 60)));
		syncLayTicksTracked();
	}

	@Override
	public @Nullable ChickenEntity createChild(ServerWorld world, net.minecraft.entity.passive.PassiveEntity entity) {
		if (!(entity instanceof ChickensChickenEntity mate)) return null;

		Identifier childType = ChickenTypes.randomChild(this.getChickenTypeId(), mate.getChickenTypeId());
		if (childType == null) return null;

		ChickensChickenEntity child = (ChickensChickenEntity) this.getType().create(world);
		if (child == null) return null;

		child.setChickenTypeId(childType);

		Identifier thisType = this.getChickenTypeId();
		Identifier mateType = mate.getChickenTypeId();
		boolean mutating = thisType.equals(mateType) && childType.equals(thisType);
		if (mutating) {
			increaseStats(child, this, mate);
		} else if (childType.equals(thisType)) {
			inheritStats(child, this);
		} else if (childType.equals(mateType)) {
			inheritStats(child, mate);
		}

		return child;
	}

	private static void inheritStats(ChickensChickenEntity child, ChickensChickenEntity parent) {
		child.dataTracker.set(TRACKED_GROWTH, parent.getGrowth());
		child.dataTracker.set(TRACKED_GAIN, parent.getGain());
		child.dataTracker.set(TRACKED_STRENGTH, parent.getStrength());
	}

	private static void increaseStats(ChickensChickenEntity child, ChickensChickenEntity p1, ChickensChickenEntity p2) {
		int s1 = p1.getStrength();
		int s2 = p2.getStrength();
		child.dataTracker.set(TRACKED_GROWTH, newStat(s1, s2, p1.getGrowth(), p2.getGrowth(), child.random.nextInt(2) + 1));
		child.dataTracker.set(TRACKED_GAIN, newStat(s1, s2, p1.getGain(), p2.getGain(), child.random.nextInt(2) + 1));
		child.dataTracker.set(TRACKED_STRENGTH, newStat(s1, s2, s1, s2, child.random.nextInt(2) + 1));
	}

	private static int newStat(int s1, int s2, int v1, int v2, int mutation) {
		int value = (v1 * s1 + v2 * s2) / Math.max(1, (s1 + s2)) + mutation;
		if (value < 1) return 1;
		if (value > 10) return 10;
		return value;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putString(NBT_TYPE, getChickenTypeId().toString());
		nbt.putInt(NBT_GROWTH, getGrowth());
		nbt.putInt(NBT_GAIN, getGain());
		nbt.putInt(NBT_STRENGTH, getStrength());
		nbt.putInt("EggTime", timeUntilNextEgg);
		if (!isBaby() && getBreedingAge() > 0) {
			nbt.putInt(NBT_BREED_COOLDOWN, getBreedingAge());
		}
		if (waterTeach > 0) {
			nbt.putInt(NBT_WATER_TEACH, waterTeach);
		}
		if (totemTeach > 0) {
			nbt.putInt(NBT_TOTEM_TEACH, totemTeach);
		}
		if (starTeach > 0) {
			nbt.putInt(NBT_STAR_TEACH, starTeach);
		}
		if (logTeach > 0) {
			nbt.putInt(NBT_LOG_TEACH, logTeach);
		}
		if (logTeachTarget != null) {
			nbt.putString(NBT_LOG_TEACH_TARGET, logTeachTarget.toString());
		}
		if (dyeTeach > 0) {
			nbt.putInt(NBT_DYE_TEACH, dyeTeach);
		}
		if (dyeTeachTarget != null) {
			nbt.putString(NBT_DYE_TEACH_TARGET, dyeTeachTarget.toString());
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains(NBT_TYPE)) {
			dataTracker.set(TRACKED_TYPE, nbt.getString(NBT_TYPE));
		}
		dataTracker.set(TRACKED_GROWTH, clampStat(nbt.getInt(NBT_GROWTH)));
		dataTracker.set(TRACKED_GAIN, clampStat(nbt.getInt(NBT_GAIN)));
		dataTracker.set(TRACKED_STRENGTH, clampStat(nbt.getInt(NBT_STRENGTH)));
		if (nbt.contains("EggTime")) {
			timeUntilNextEgg = Math.max(1, nbt.getInt("EggTime"));
		}
		waterTeach = Math.max(0, nbt.getInt(NBT_WATER_TEACH));
		totemTeach = Math.max(0, nbt.getInt(NBT_TOTEM_TEACH));
		starTeach = Math.max(0, nbt.getInt(NBT_STAR_TEACH));
		logTeach = Math.max(0, nbt.getInt(NBT_LOG_TEACH));
		if (nbt.contains(NBT_LOG_TEACH_TARGET)) {
			logTeachTarget = new Identifier(nbt.getString(NBT_LOG_TEACH_TARGET));
		} else {
			logTeachTarget = null;
		}
		dyeTeach = Math.max(0, nbt.getInt(NBT_DYE_TEACH));
		if (nbt.contains(NBT_DYE_TEACH_TARGET)) {
			dyeTeachTarget = new Identifier(nbt.getString(NBT_DYE_TEACH_TARGET));
		} else {
			dyeTeachTarget = null;
		}
		dataTracker.set(TRACKED_EGG_MINUTES, Math.max(0, timeUntilNextEgg / (20 * 60)));
		dataTracker.set(TRACKED_LAY_TICKS, Math.max(0, timeUntilNextEgg));
		if (!isBaby() && nbt.contains(NBT_BREED_COOLDOWN)) {
			setBreedingAge(Math.max(0, nbt.getInt(NBT_BREED_COOLDOWN)));
		}
	}

	private static int clampStat(int v) {
		return ChickenAttributeTicks.clampStat(v);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
		if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION) {
			SpawnType spawnType = getSpawnTypeForWorld(world.toServerWorld());
			var candidates = ChickenTypes.getSpawnCandidates(spawnType);
			if (!candidates.isEmpty()) {
				var chosen = candidates.get(world.getRandom().nextInt(candidates.size()));
				setChickenTypeId(chosen.id());
			}
		}
		return data;
	}

	private SpawnType getSpawnTypeForWorld(ServerWorld world) {
		if (world.getDimension().ultrawarm()) {
			return SpawnType.HELL;
		}
		if (world.getBiome(getBlockPos()).value().isCold(getBlockPos())) {
			return SpawnType.SNOW;
		}
		return SpawnType.NORMAL;
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		super.dropLoot(source, causedByPlayer);
		ItemStack drop = getChickenType().dropItem().copy();
		if (!drop.isEmpty()) {
			this.dropStack(drop);
		}
	}
}
