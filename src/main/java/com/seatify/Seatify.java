package com.seatify;

import com.seatify.network.SeatifyNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Take a Seat – common (both-sides) entrypoint.
 *
 * <p>Spiritual successor to the "Sitting Plus" mod. Same behaviour, new identity,
 * rebuilt for Minecraft 26.1.2 on the {@code com.zigythebird.playeranim}
 * (Player Animation Library) backend instead of the discontinued kosmx player-animator.
 */
@Mod(Seatify.MOD_ID)
public class Seatify {
	public static final String MOD_ID = "seatify";
	public static final Logger LOGGER = LoggerFactory.getLogger("Take a Seat");

	public Seatify(IEventBus modBus) {
		modBus.addListener(SeatifyNetworking::registerPayloadHandlers);

		NeoForge.EVENT_BUS.addListener(SeatifyNetworking::onPlayerLoggedIn);
		NeoForge.EVENT_BUS.addListener(SeatifyNetworking::onServerTick);

		LOGGER.info("Take a Seat initialized.");
	}

	/** Helper for building {@code seatify:<path>} identifiers. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
