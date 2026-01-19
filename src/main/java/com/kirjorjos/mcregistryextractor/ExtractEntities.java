package com.kirjorjos.mcregistryextractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ExtractEntities {
	public static JsonObject extractEntities() {
    JsonObject root = new JsonObject();
    JsonObject entitiesRoot = new JsonObject();
    root.add("entities", entitiesRoot);

    for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
			ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
			if (id == null) continue;

			String namespace = id.getNamespace();
			String path = id.getPath();

			JsonObject namespaceObj = entitiesRoot.has(namespace)
							? entitiesRoot.getAsJsonObject(namespace)
							: new JsonObject();

			JsonObject entityJson = new JsonObject();

			// Basic info
			entityJson.addProperty("name", type.getDescription().getString());
			ModContainer mod = ModList.get().getModContainerById(namespace).orElse(null);
			entityJson.addProperty("mod", mod != null ? mod.getModInfo().getDisplayName() : namespace);

			// Class traits
			MobCategory category = type.getCategory();
			entityJson.addProperty("isMob", category.equals(MobCategory.MONSTER));
			entityJson.addProperty("isAnimal", category.equals(MobCategory.CREATURE) || category.equals(MobCategory.WATER_CREATURE));
			entityJson.addProperty("isPlayer", type.equals(EntityType.PLAYER));
			entityJson.addProperty("minecart", type.equals(EntityType.MINECART));
			entityJson.addProperty("isItem", type.equals(EntityType.ITEM));

			// Size
			entityJson.addProperty("width", type.getWidth());
			entityJson.addProperty("height", type.getHeight());

			// Instance-level defaults
			entityJson.addProperty("health", 0);
			entityJson.addProperty("isBurning", false);
			entityJson.addProperty("isWet", false);
			entityJson.addProperty("isCrouching", false);
			entityJson.addProperty("isEating", false);
			entityJson.addProperty("age", 0);
			entityJson.addProperty("isChild", false);
			entityJson.addProperty("canBreed", false);
			entityJson.addProperty("isInLove", false);
			entityJson.addProperty("isShearable", false);
			entityJson.add("inventory", new JsonArray());
			entityJson.add("armorInventory", new JsonArray());
			entityJson.add("fluids", new JsonArray());
			entityJson.addProperty("entityType", id.toString());
			entityJson.add("tags", new JsonArray());

			// Sounds (optional)
			String hurtSound = null;
			String deathSound = null;

			JsonObject sounds = new JsonObject();
			sounds.addProperty("hurt", hurtSound);
			sounds.addProperty("death", deathSound);
			entityJson.add("sounds", sounds);

			// Add to namespace
			namespaceObj.add(path, entityJson);
			entitiesRoot.add(namespace, namespaceObj);
    }

    return root;
	}

}
