package com.seatify;

import com.seatify.network.SeatifyNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seatify – common (both-sides) entrypoint.
 *
 * <p>Spiritual successor to the "Sitting Plus" mod. Same behaviour, new identity,
 * rebuilt for Minecraft 26.1.2 on the {@code com.zigythebird.playeranim}
 * (Player Animation Library) backend instead of the discontinued kosmx player-animator.
 */
public class Seatify implements ModInitializer {
	public static final String MOD_ID = "seatify";
	public static final Logger LOGGER = LoggerFactory.getLogger("Seatify");

	/** Helper for building {@code seatify:<path>} identifiers. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Registers the sit/stand network payloads and the server-side re-broadcast.
		SeatifyNetworking.registerCommon();
		LOGGER.info("Seatify initialized.");
	}
}
