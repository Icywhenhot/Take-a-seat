package com.takeaseat;

import com.takeaseat.network.TakeASeatNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TakeASeat implements ModInitializer {
	public static final String MOD_ID = "takeaseat";
	public static final Logger LOGGER = LoggerFactory.getLogger("Take a Seat");

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		TakeASeatNetworking.registerCommon();
		LOGGER.info("Take a Seat initialized.");
	}
}
