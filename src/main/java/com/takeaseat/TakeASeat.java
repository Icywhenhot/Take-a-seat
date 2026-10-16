package com.takeaseat;

import com.takeaseat.network.TakeASeatNetworking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TakeASeat.MOD_ID)
public class TakeASeat {
	public static final String MOD_ID = "takeaseat";
	public static final Logger LOGGER = LoggerFactory.getLogger("Take a Seat");

	public TakeASeat() {
		TakeASeatNetworking.register();

		MinecraftForge.EVENT_BUS.addListener(TakeASeatNetworking::onPlayerLoggedIn);
		MinecraftForge.EVENT_BUS.addListener(TakeASeatNetworking::onServerTick);

		LOGGER.info("Take a Seat initialized.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
