package com.takeaseat;

import com.takeaseat.network.TakeASeatNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TakeASeat – common (both-sides) entrypoint.
 *
 * <p>Spiritual successor to the "Sitting Plus" mod. Same behaviour, new identity,
 * rebuilt for Minecraft 1.21.11 on the {@code com.zigythebird.playeranim}
 * (Player Animation Library) backend instead of the discontinued kosmx player-animator.
 */
public class TakeASeat implements ModInitializer {
	public static final String MOD_ID = "takeaseat";
	public static final Logger LOGGER = LoggerFactory.getLogger("Take a Seat");

	/** Helper for building {@code takeaseat:<path>} identifiers. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Registers the sit/stand network payloads and the server-side re-broadcast.
		TakeASeatNetworking.registerCommon();
		LOGGER.info("Take a Seat initialized.");
	}
}
