package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TakeASeat.MOD_ID, value = Dist.CLIENT)
public final class TakeASeatClientEvents {
	private TakeASeatClientEvents() {}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		TakeASeatClient.onClientTick(event);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		TakeASeatClient.onRightClickBlock(event);
	}

	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event) {
		TakeASeatClient.registerClientCommands(event);
	}
}
