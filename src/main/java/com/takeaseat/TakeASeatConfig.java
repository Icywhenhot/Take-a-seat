package com.takeaseat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TakeASeatConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("TakeASeatConfig.json");

	public boolean enableClickToSit = true;
	public boolean enableThirdPersonOnSit = true;
	public boolean enableSitCameraFocus = true;
	public double cameraFocusOffset = 0.55;
	public boolean onlyLowerCameraInFirstPerson = false;
	public boolean enableAfkSit = false;
	public int afkSitDelaySeconds = 60;

	private static TakeASeatConfig instance;

	public static TakeASeatConfig getConfig() {
		if (instance == null) {
			instance = new TakeASeatConfig();
			instance.loadConfig();
		}
		return instance;
	}

	private void loadConfig() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				TakeASeatConfig loaded = GSON.fromJson(reader, TakeASeatConfig.class);
				if (loaded != null) {
					this.enableClickToSit = loaded.enableClickToSit;
					this.enableThirdPersonOnSit = loaded.enableThirdPersonOnSit;
					this.enableSitCameraFocus = loaded.enableSitCameraFocus;
					this.cameraFocusOffset = loaded.cameraFocusOffset;
					this.onlyLowerCameraInFirstPerson = loaded.onlyLowerCameraInFirstPerson;
					this.enableAfkSit = loaded.enableAfkSit;
					this.afkSitDelaySeconds = loaded.afkSitDelaySeconds;
				}
			} catch (IOException e) {
				TakeASeat.LOGGER.error("Failed to read TakeASeatConfig.json", e);
			}
		} else {
			this.saveConfig();
		}
	}

	public void saveConfig() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			TakeASeat.LOGGER.error("Failed to write TakeASeatConfig.json", e);
		}
	}
}
