package com.takeaseat;

import com.takeaseat.network.TakeASeatNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TakeASeat.MOD_ID)
public class TakeASeat {
	public static final String MOD_ID = "takeaseat";
	public static final Logger LOGGER = LoggerFactory.getLogger("Take a Seat");

	public TakeASeat(IEventBus modBus) {
		modBus.addListener(TakeASeatNetworking::registerPayloadHandlers);

		NeoForge.EVENT_BUS.addListener(TakeASeatNetworking::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(TakeASeatNetworking::onServerTick);

		LOGGER.info("Take a Seat initialized.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
