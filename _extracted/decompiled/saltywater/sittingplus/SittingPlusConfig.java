package com.saltywater.sittingplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class SittingPlusConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("SittingPlusConfig.json");
   public boolean enableClickToSit = true;
   public boolean enableThirdPersonOnSit = true;
   public boolean onlyLowerCameraInFirstPerson = false;
   public boolean enableAfkSit = true;
   public int afkSitDelaySeconds = 60;
   private static SittingPlusConfig instance;

   public static SittingPlusConfig getConfig() {
      if (instance == null) {
         instance = new SittingPlusConfig();
         instance.loadConfig();
      }

      return instance;
   }

   private void loadConfig() {
      if (Files.exists(CONFIG_PATH)) {
         try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            SittingPlusConfig loaded = (SittingPlusConfig)GSON.fromJson(reader, SittingPlusConfig.class);
            if (loaded != null) {
               this.enableClickToSit = loaded.enableClickToSit;
               this.enableThirdPersonOnSit = loaded.enableThirdPersonOnSit;
               this.onlyLowerCameraInFirstPerson = loaded.onlyLowerCameraInFirstPerson;
               this.enableAfkSit = loaded.enableAfkSit;
               this.afkSitDelaySeconds = loaded.afkSitDelaySeconds;
            }
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      } else {
         this.saveConfig();
      }
   }

   public void saveConfig() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());

         try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
         }
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }
}
