package com.seatify.client;

import com.seatify.Seatify;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Seatify.MOD_ID, value = Dist.CLIENT)
public final class SeatifyClientEvents {
	private SeatifyClientEvents() {}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		SeatifyClient.onClientTick(event);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		SeatifyClient.onRightClickBlock(event);
	}
}
