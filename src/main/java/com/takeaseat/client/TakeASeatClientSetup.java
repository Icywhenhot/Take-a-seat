package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = TakeASeat.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TakeASeatClientSetup {
	private TakeASeatClientSetup() {}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		TakeASeatClient.registerKeyMappings(event);
	}
}
