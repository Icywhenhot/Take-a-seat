package com.takeaseat.client;

import com.takeaseat.TakeASeatConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds the Take a Seat settings screen with Cloth Config. Values are written straight back into
 * {@link TakeASeatConfig} and persisted via {@code saveConfig()} when the screen's Save is pressed.
 */
public final class TakeASeatConfigScreen {
	private TakeASeatConfigScreen() {}

	public static Screen create(Screen parent) {
		TakeASeatConfig cfg = TakeASeatConfig.getConfig();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Take a Seat"));
		builder.setSavingRunnable(cfg::saveConfig);

		ConfigEntryBuilder eb = builder.entryBuilder();

		ConfigCategory sitting = builder.getOrCreateCategory(Component.literal("Sitting"));
		sitting.addEntry(eb.startBooleanToggle(Component.literal("Click stairs to sit"), cfg.enableClickToSit)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Right-click a stair with an empty hand to sit on it."))
				.setSaveConsumer(v -> cfg.enableClickToSit = v)
				.build());

		ConfigCategory camera = builder.getOrCreateCategory(Component.literal("Camera"));
		camera.addEntry(eb.startBooleanToggle(Component.literal("Switch to third person on sit"), cfg.enableThirdPersonOnSit)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Restored to your previous view when you stand up."))
				.setSaveConsumer(v -> cfg.enableThirdPersonOnSit = v)
				.build());
		camera.addEntry(eb.startBooleanToggle(Component.literal("Lower camera to focus on model"), cfg.enableSitCameraFocus)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.enableSitCameraFocus = v)
				.build());
		camera.addEntry(eb.startDoubleField(Component.literal("Camera focus offset (blocks)"), cfg.cameraFocusOffset)
				.setDefaultValue(0.55)
				.setMin(0.0).setMax(2.0)
				.setSaveConsumer(v -> cfg.cameraFocusOffset = v)
				.build());
		camera.addEntry(eb.startBooleanToggle(Component.literal("Only lower camera in first person"), cfg.onlyLowerCameraInFirstPerson)
				.setDefaultValue(false)
				.setSaveConsumer(v -> cfg.onlyLowerCameraInFirstPerson = v)
				.build());

		ConfigCategory afk = builder.getOrCreateCategory(Component.literal("AFK auto-sit"));
		afk.addEntry(eb.startBooleanToggle(Component.literal("Enable AFK auto-sit"), cfg.enableAfkSit)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Automatically sit on the ground after being idle for a while."))
				.setSaveConsumer(v -> cfg.enableAfkSit = v)
				.build());
		afk.addEntry(eb.startIntSlider(Component.literal("AFK delay (seconds)"), cfg.afkSitDelaySeconds, 5, 600)
				.setDefaultValue(60)
				.setSaveConsumer(v -> cfg.afkSitDelaySeconds = v)
				.build());

		return builder.build();
	}
}
