package com.seatify.client;

import com.seatify.Seatify;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = Seatify.MOD_ID, value = Dist.CLIENT)
public final class SeatifyClientSetup {
	private SeatifyClientSetup() {}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		SeatifyClient.registerKeyMappings(event);
	}

	@SubscribeEvent
	public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
		SeatifyClientNetworking.registerClientReceivers(event);
	}
}
