package com.takeaseat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config, stored at {@code config/TakeASeatConfig.json}.
 * Mirrors the original mod's options.
 */
public class TakeASeatConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("TakeASeatConfig.json");
	private static final Path LEGACY_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("SeatifyConfig.json");

	/** Right-click an empty hand on stairs to sit down on them. */
	public boolean enableClickToSit = true;
	/** Automatically switch to third person while sitting (restored when you stand). */
	public boolean enableThirdPersonOnSit = true;
	/** Smoothly lower the camera while sitting so the focus settles on the player model. */
	public boolean enableSitCameraFocus = true;
	/** How far (in blocks) to lower the camera while sitting. */
	public double cameraFocusOffset = 0.55;
	/** If true, only apply the camera-lowering while in first person (skip it in third person). */
	public boolean onlyLowerCameraInFirstPerson = false;
	/**
	 * Auto-sit after being idle. NOTE: in the original mod this feature was dead code and never fired.
	 * It is implemented here but defaults to {@code false} to preserve the original behaviour; flip it on if you want it.
	 */
	public boolean enableAfkSit = false;
	/** Seconds of no movement input before the AFK auto-sit kicks in. */
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
		Path loadPath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
		if (Files.exists(loadPath)) {
			try (Reader reader = Files.newBufferedReader(loadPath)) {
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
			if (Files.exists(LEGACY_CONFIG_PATH) && !LEGACY_CONFIG_PATH.equals(CONFIG_PATH)) {
				Files.deleteIfExists(LEGACY_CONFIG_PATH);
			}
		} catch (IOException e) {
			TakeASeat.LOGGER.error("Failed to write TakeASeatConfig.json", e);
		}
	}
}
