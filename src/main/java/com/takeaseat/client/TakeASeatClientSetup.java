package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = TakeASeat.MOD_ID, value = Dist.CLIENT)
public final class TakeASeatClientSetup {
	private TakeASeatClientSetup() {}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		TakeASeatClient.registerKeyMappings(event);
	}

	@SubscribeEvent
	public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
		TakeASeatClientNetworking.registerClientReceivers(event);
	}
}
