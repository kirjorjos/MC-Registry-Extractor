package com.kirjorjos.mcregistryextractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ExtractBlocks {
	
	@SuppressWarnings("null")
	public static JsonObject extractBlocks(ServerPlayer player) {
    JsonObject root = new JsonObject();
    JsonObject blocksRoot = new JsonObject();
    root.add("blocks", blocksRoot);

    for (Block block : ForgeRegistries.BLOCKS) {
			ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
			if (id == null) continue;

			String namespace = id.getNamespace();
			String path = id.getPath();

			JsonObject namespaceObj = blocksRoot.has(namespace)
							? blocksRoot.getAsJsonObject(namespace)
							: new JsonObject();

			JsonObject blockJson = new JsonObject();

			// name
			blockJson.addProperty("name", block.getName().getString());

			// mod
			ModContainer mod = ModList.get().getModContainerById(namespace).orElse(null);
			blockJson.addProperty("mod",
							mod != null ? mod.getModInfo().getDisplayName() : namespace);

			// item
			Item item = block.asItem();
			blockJson.addProperty("item",
							item == Items.AIR ? null :
											ForgeRegistries.ITEMS.getKey(item).toString());

			// opaque
			blockJson.addProperty("opaque",
							block.defaultBlockState().isSolidRender(player.level, BlockPos.ZERO));

			// shearable
			blockJson.addProperty("shearable", ( block instanceof IForgeShearable) ? ((IForgeShearable) block).isShearable(ItemStack.EMPTY, null, null) : false);

			// sounds
			SoundType sound = block.defaultBlockState().getSoundType();
			JsonObject sounds = new JsonObject();
			sounds.addProperty("break", sound.getBreakSound().getLocation().toString());
			sounds.addProperty("place", sound.getPlaceSound().getLocation().toString());
			sounds.addProperty("step", sound.getStepSound().getLocation().toString());
			blockJson.add("sounds", sounds);

			// plant age (max)
			int plantAge = -1;
			if (block instanceof CropBlock crop) {
					plantAge = crop.getMaxAge();
			}
			blockJson.addProperty("plantAge", plantAge);

			// fluid
			FluidState fluidState = block.defaultBlockState().getFluidState();

			String fluidId = fluidState.isEmpty()
							? null
							: ForgeRegistries.FLUIDS
										.getKey(fluidState.getType())
										.toString();

			blockJson.addProperty("fluid", fluidId);
			blockJson.addProperty("fluidCapacity", 0);

			// energy
			JsonObject energy = new JsonObject();
			energy.addProperty("isContainer", block instanceof EntityBlock);
			energy.addProperty("capacity", 0);
			energy.addProperty("stored", 0);
			blockJson.add("energy", energy);

			// inventory
			blockJson.add("inventory", new JsonArray());

			// tags
			JsonArray tags = new JsonArray();
			block.builtInRegistryHolder().tags()
				.forEach(tag -> {
						String tagName = tag.location().toString();
						tags.add(tagName);
				});
			blockJson.add("tags", tags);

			namespaceObj.add(path, blockJson);
			blocksRoot.add(namespace, namespaceObj);
    }

    return root;
	}

}
