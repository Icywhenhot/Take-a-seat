package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TakeASeat.MOD_ID, value = Dist.CLIENT)
public final class TakeASeatClientEvents {
	private TakeASeatClientEvents() {}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		TakeASeatClient.onClientTick(event);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		TakeASeatClient.onRightClickBlock(event);
	}
}
