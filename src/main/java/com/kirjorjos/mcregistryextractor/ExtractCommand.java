package com.kirjorjos.mcregistryextractor;

import net.minecraft.network.chat.Component;

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
													String args = StringArgumentType.getString(context, "args");
													JsonObject items = ExtractItems.extractItems();
													return JSONWriter.write(args, items);
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