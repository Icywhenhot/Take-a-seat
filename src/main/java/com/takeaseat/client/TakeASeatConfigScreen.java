package com.takeaseat.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * Placeholder for the old settings screen.
 *
 * <p>The configuration now lives in {@code config/TakeASeatConfig.json}. This class remains as a
 * compatibility stub so the client package layout stays stable during the NeoForge port.
 */
public final class TakeASeatConfigScreen {
	private TakeASeatConfigScreen() {}

	public static Screen create(Screen parent) {
		return parent;
	}
}
