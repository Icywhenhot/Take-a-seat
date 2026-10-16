package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;

@EventBusSubscriber(modid = TakeASeat.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TakeASeatClientMod {
	private TakeASeatClientMod() {}

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent event) {
		if (ModList.get().isLoaded("cloth_config")) {
			ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
					() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> TakeASeatConfigScreen.create(parent)));
		}
	}
}
