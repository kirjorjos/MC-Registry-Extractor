package com.kirjorjos.mcregistryextractor;

import net.minecraft.network.chat.Component;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ExtractCommand {

	@SuppressWarnings("null")
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
			dispatcher.register(
					Commands.literal("extract")
							.then(
									Commands.argument("args", StringArgumentType.greedyString())
											.executes(context -> {

												JsonObject root = new JsonObject();

												// Create the "metadata" object
												JsonObject metadata = new JsonObject();
												metadata.addProperty("schemaVersion", "1");
												metadata.addProperty("gameVersion", "1.19.2");
												metadata.addProperty("description", "Generated game registries for WIP Integrated Dynamics project");

												// Add the metadata object to the root
												root.add("metadata", metadata);
												String args = StringArgumentType.getString(context, "args");
												root.add("items", ExtractItems.extractItems());
												root.add("blocks", ExtractBlocks.extractBlocks());
												root.add("fluids", ExtractFluids.extractFluids());
												root.add("entities", ExtractEntities.extractEntities());
												try {
													String path = JSONWriter.write(args, root);
													context.getSource().sendSuccess(
														Component.literal("Wrote to: " + path),
														true
													);
												} catch(IOException e) {
													context.getSource().sendFailure(
														Component.literal("Failed to write to \"" + args + "\"")
													);
													return 0;
												}
												return 1;
											})
							)
							.executes(context -> {
									context.getSource().sendFailure(
											Component.literal("Usage: /extract <file path>")
									);
									return 0;
							})
			);
	}
}