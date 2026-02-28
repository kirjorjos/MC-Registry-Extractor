package com.kirjorjos.mcregistryextractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class ExtractItems {
	public static JsonObject extractItems() {
			JsonObject items = new JsonObject();

			for (Item item : ForgeRegistries.ITEMS) {
					@SuppressWarnings("null")
					ItemStack stack = new ItemStack(item);
					ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);

					String namespace = id.getNamespace();
					String path = id.getPath();

					// create namespace object if needed
					items.add(namespace, items.has(namespace) ? items.get(namespace).getAsJsonObject() : new JsonObject());
					JsonObject nsObj = items.get(namespace).getAsJsonObject();

					JsonObject itemJson = new JsonObject();
					itemJson.addProperty("name", stack.getHoverName().getString());
					
					ModContainer mod = ModList.get().getModContainerById(namespace).orElse(null);
					itemJson.addProperty("mod", mod != null ? mod.getModInfo().getDisplayName() : namespace);
					
					itemJson.addProperty("block", item instanceof BlockItem bi ? ForgeRegistries.BLOCKS.getKey(bi.getBlock()).toString() : null);
					itemJson.addProperty("maxSize", item.getMaxStackSize());
					itemJson.addProperty("maxDamage", item.isDamageable(stack) ? item.getMaxDamage() : -1);
					itemJson.addProperty("isEnchantable", !stack.isEmpty() && stack.isEnchantable());
					itemJson.addProperty("fuelBurnTime", ForgeHooks.getBurnTime(stack, null));
					itemJson.addProperty("feContainer", stack.getCapability(CapabilityEnergy.ENERGY).isPresent());
					
					if (item instanceof BucketItem bucket) {
						itemJson.addProperty("isFluidContainer", true);
						itemJson.addProperty("fluid", ForgeRegistries.FLUIDS.getKey(bucket.getFluid()).toString());
						itemJson.addProperty("fluidCapacity", 1000);
					} else {
						itemJson.addProperty("isFluidContainer", false);
						itemJson.addProperty("fluidCapacity", 0);
					}

					int invSize = 0;
					var handlerCap = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
					if (handlerCap.isPresent()) {
						invSize = handlerCap.orElse(null).getSlots();
					}
					itemJson.addProperty("inventorySize", invSize);

					int toolTier = 0;
					float efficiency = 1.0f;
					if (item instanceof TieredItem tiered) {
						toolTier = tiered.getTier().getLevel();
					}
					if (item instanceof DiggerItem digger) {
						efficiency = digger.getTier().getSpeed();
					}
					itemJson.addProperty("toolTier", toolTier);
					itemJson.addProperty("efficiency", efficiency);

					// tooltips
					JsonArray tooltipArray = new JsonArray();
					for (var component : stack.getTooltipLines(null, TooltipFlag.Default.ADVANCED)) {
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

			return items;
	}

}
