package com.takeaseat.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration. Only loaded when Mod Menu is installed (it reads the {@code modmenu} entrypoint),
 * so Take a Seat has no hard dependency on it. The screen itself is built with Cloth Config (bundled).
 */
public class TakeASeatModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return TakeASeatConfigScreen::create;
	}
}
