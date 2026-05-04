package com.setycz.chickens.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.setycz.chickens.ChickensFabricMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChickenTypeManager implements SimpleSynchronousResourceReloadListener {
	private static final Gson GSON = new Gson();
	private static final Identifier FALLBACK_ENTITY_TEXTURE =
			new Identifier("minecraft", "textures/entity/chicken.png");

	/** 实体贴图必须是合法 Identifier 路径（全小写等）；旧资源包常有 PascalCase 文件名，在此处纠正以免整类型被丢弃 */
	private static Identifier parseTexture(Identifier chickenId, String raw) {
		try {
			return new Identifier(raw);
		} catch (InvalidIdentifierException first) {
			int colon = raw.indexOf(':');
			if (colon > 0) {
				String namespace = raw.substring(0, colon);
				String path = raw.substring(colon + 1).toLowerCase(Locale.ROOT);
				try {
					Identifier normalized = new Identifier(namespace, path);
					ChickensFabricMod.LOGGER.warn(
							"鸡类型 {} 的 texture 不符合 Identifier 规则，已使用小写路径：{} -> {}",
							chickenId,
							raw,
							normalized
					);
					return normalized;
				} catch (InvalidIdentifierException ignored) {
					// fall through to default
				}
			}
			ChickensFabricMod.LOGGER.warn(
					"鸡类型 {} 的 texture 无法解析，使用默认贴图：{} （{}）",
					chickenId,
					raw,
					first.toString().replace('\n', ' ')
			);
			return FALLBACK_ENTITY_TEXTURE;
		}
	}

	@Override
	public Identifier getFabricId() {
		return ChickensFabricMod.id("chicken_types");
	}

	@Override
	public void reload(ResourceManager manager) {
		Map<Identifier, ChickenType> loaded = new HashMap<>();

		var resources = manager.findResources(
				"chickens",
				id -> id.getPath().endsWith(".json") && id.getPath().startsWith("chickens/"));
		for (var entry : resources.entrySet()) {
			Identifier fileId = entry.getKey();
			try (var reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				Identifier chickenId = chickenIdFromPath(fileId);
				ChickenType type = parse(chickenId, json);
				loaded.put(chickenId, type);
			} catch (Exception e) {
				ChickensFabricMod.LOGGER.error("加载鸡类型失败：{}", fileId, e);
			}
		}

		// 如果 datapack 没给任何类型，也保证至少有一个默认
		if (!loaded.containsKey(ChickenTypes.defaultId())) {
			loaded.put(ChickenTypes.defaultId(), defaultWhite(ChickenTypes.defaultId()));
		}

		ChickenTypes.setAll(loaded);
	}

	private static Identifier chickenIdFromPath(Identifier fileId) {
		// data/<namespace>/chickens/<path>.json  -> <namespace>:<path>
		// 例：data/chickens/chickens/white.json -> chickens:white
		String path = fileId.getPath();
		if (path.startsWith("chickens/")) {
			path = path.substring("chickens/".length());
		}
		if (path.endsWith(".json")) {
			path = path.substring(0, path.length() - ".json".length());
		}
		return new Identifier(fileId.getNamespace(), path);
	}

	private static ChickenType parse(Identifier id, JsonObject json) {
		String defaultNameKey = "entity.chickens." + id.getPath() + ".name";
		String nameKey = getString(json, "name_key", defaultNameKey);
		Identifier texture = parseTexture(id, getString(json, "texture", "minecraft:textures/entity/chicken.png"));
		int bg = getInt(json, "bg_color", 0xFFFFFF);
		int fg = getInt(json, "fg_color", 0xFFFF00);

		ItemStack lay = readItemStack(json.get("lay_item"), getInt(json, "lay_count", 1));
		applyNbt(lay, json.get("lay_nbt"));
		ItemStack drop = json.has("drop_item")
				? readItemStack(json.get("drop_item"), getInt(json, "drop_count", 1))
				: lay.copy();
		applyNbt(drop, json.get("drop_nbt"));

		float layCoef = getFloat(json, "lay_coefficient", 1.0f);
		SpawnType spawnType = SpawnType.valueOf(getString(json, "spawn_type", "NONE").toUpperCase());

		Identifier p1 = getIdOrNull(json.get("parent1"));
		Identifier p2 = getIdOrNull(json.get("parent2"));

		return new ChickenType(id, nameKey, texture, bg, fg, lay, drop, layCoef, spawnType, p1, p2);
	}

	private static ChickenType defaultWhite(Identifier id) {
		return parse(id, GSON.fromJson("""
				{
				  "name_key": "entity.chickens.chickens_chicken",
				  "texture": "minecraft:textures/entity/chicken.png",
				  "bg_color": 16777215,
				  "fg_color": 16776960,
				  "lay_item": "minecraft:egg",
				  "lay_count": 1,
				  "spawn_type": "NORMAL"
				}
				""", JsonObject.class));
	}

	private static ItemStack readItemStack(JsonElement el, int count) {
		if (el == null || el.isJsonNull()) {
			return ItemStack.EMPTY;
		}
		Identifier id = new Identifier(el.getAsString());
		Item item = Registries.ITEM.get(id);
		return new ItemStack(item, Math.max(1, count));
	}

	private static void applyNbt(ItemStack stack, JsonElement nbtElement) {
		if (stack.isEmpty() || nbtElement == null || nbtElement.isJsonNull()) {
			return;
		}
		try {
			NbtCompound nbt = StringNbtReader.parse(nbtElement.getAsString());
			stack.setNbt(nbt);
		} catch (Exception e) {
			throw new IllegalArgumentException("无法解析物品 NBT: " + nbtElement, e);
		}
	}

	private static String getString(JsonObject obj, String key, String def) {
		return obj.has(key) ? obj.get(key).getAsString() : def;
	}

	private static int getInt(JsonObject obj, String key, int def) {
		return obj.has(key) ? obj.get(key).getAsInt() : def;
	}

	private static float getFloat(JsonObject obj, String key, float def) {
		return obj.has(key) ? obj.get(key).getAsFloat() : def;
	}

	private static Identifier getIdOrNull(JsonElement el) {
		if (el == null || el.isJsonNull()) return null;
		String s = el.getAsString();
		if (s == null || s.isEmpty()) return null;
		return new Identifier(s);
	}
}

