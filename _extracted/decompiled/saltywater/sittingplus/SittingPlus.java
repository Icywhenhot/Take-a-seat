package com.saltywater.sittingplus;

import com.saltywater.sittingplus.networking.SittingPlusNetworking;
import net.fabricmc.api.ModInitializer;

public class SittingPlus implements ModInitializer {
   public static final String MODID = "sittingplus";

   public void onInitialize() {
      SittingPlusNetworking.register();
   }
}
