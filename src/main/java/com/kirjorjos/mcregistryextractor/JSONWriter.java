package com.kirjorjos.mcregistryextractor;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class JSONWriter {

	public static int write(String path, JsonObject json) {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (FileWriter writer = new FileWriter(path)) {
				gson.toJson(json, writer);
		} catch (IOException e) {
			MCRegistryExtractor.getLogger().warn(e.getLocalizedMessage());
			return 1;
		}

		return 0;
	}

}
