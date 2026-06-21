package com.seatify.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration. Only loaded when Mod Menu is installed (it reads the {@code modmenu} entrypoint),
 * so Seatify has no hard dependency on it. The screen itself is built with Cloth Config (bundled).
 */
public class SeatifyModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SeatifyConfigScreen::create;
	}
}
