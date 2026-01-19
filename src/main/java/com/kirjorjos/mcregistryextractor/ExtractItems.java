package com.kirjorjos.mcregistryextractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class ExtractItems {
	public static JsonObject extractItems() {
			JsonObject root = new JsonObject();
			JsonObject namespaces = new JsonObject();

			for (Item item : ForgeRegistries.ITEMS) {
					@SuppressWarnings("null")
					ItemStack stack = new ItemStack(item);
					ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);

					String namespace = id.getNamespace();
					String path = id.getPath();

					// create namespace object if needed
					namespaces.add(namespace, namespaces.has(namespace) ? namespaces.get(namespace).getAsJsonObject() : new JsonObject());
					JsonObject nsObj = namespaces.get(namespace).getAsJsonObject();

					JsonObject itemJson = new JsonObject();
					itemJson.addProperty("name", stack.getHoverName().getString());
					itemJson.addProperty("mod", capitalize(namespace));
					itemJson.addProperty("block", item instanceof BlockItem bi ? ForgeRegistries.BLOCKS.getKey(bi.getBlock()).toString() : null);
					itemJson.addProperty("maxSize", item.getMaxStackSize());
					itemJson.addProperty("maxDamage", item.isDamageable(stack) ? item.getMaxDamage() : -1);
					itemJson.addProperty("isEnchantable", !stack.isEmpty() && stack.isEnchantable());
					itemJson.addProperty("fuelBurnTime", ForgeHooks.getBurnTime(stack, null));
					itemJson.addProperty("feContainer", stack.getCapability(CapabilityEnergy.ENERGY).isPresent());
					itemJson.addProperty("isFluidContainer", false); // placeholder
					itemJson.addProperty("fluidCapacity", 0);
					itemJson.addProperty("inventorySize", stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent() ? 1 : 0);

					// tooltips
					JsonArray tooltipArray = new JsonArray();
					for (var component : stack.getTooltipLines(null, TooltipFlag.Default.NORMAL)) {
							tooltipArray.add(component.getString());
					}
					itemJson.add("tooltip", tooltipArray);

					JsonArray tagsArray = new JsonArray();
					item.builtInRegistryHolder().tags()
					.forEach(tag -> {
							String tagName = tag.location().toString();
							tagsArray.add(tagName);
					});
					itemJson.add("tags", tagsArray);

					nsObj.add(path, itemJson);
			}

			root.add("items", namespaces);
			return root;
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

}
