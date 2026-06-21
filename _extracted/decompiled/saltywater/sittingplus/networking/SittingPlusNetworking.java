package com.saltywater.sittingplus.networking;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1657;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_742;
import net.minecraft.class_8710;
import net.minecraft.class_9139;
import net.minecraft.class_8710.class_9154;

public class SittingPlusNetworking {
   private static final class_2960 START_SIT_ID = class_2960.method_60655("sittingplus", "start_sit");
   private static final class_2960 STOP_SIT_ID = class_2960.method_60655("sittingplus", "stop_sit");

   public static void register() {
      ServerPlayNetworking.registerGlobalReceiver(
         SittingPlusNetworking.StartSitPayload.ID, (payload, context) -> context.player().method_5682().execute(() -> {
            for (class_3222 sp : context.player().method_5682().method_3760().method_14571()) {
               ServerPlayNetworking.send(sp, payload);
            }
         })
      );
      ServerPlayNetworking.registerGlobalReceiver(SittingPlusNetworking.StopSitPayload.ID, (payload, context) -> context.player().method_5682().execute(() -> {
         for (class_3222 sp : context.player().method_5682().method_3760().method_14571()) {
            ServerPlayNetworking.send(sp, payload);
         }
      }));
   }

   public static void registerClientReceivers() {
      ClientPlayNetworking.registerGlobalReceiver(SittingPlusNetworking.StartSitPayload.ID, (payload, context) -> context.client().execute(() -> {
         if (context.client().field_1687 != null) {
            for (class_1657 p : context.client().field_1687.method_18456()) {
               if (p.method_5667().equals(payload.playerUuid()) && p instanceof class_742 ace) {
                  AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(ace);
                  if (stack != null && PlayerAnimationRegistry.getAnimation(payload.animId()) instanceof KeyframeAnimation anim) {
                     stack.addAnimLayer(0, new KeyframeAnimationPlayer(anim));
                  }
               }
            }
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(SittingPlusNetworking.StopSitPayload.ID, (payload, context) -> context.client().execute(() -> {
         if (context.client().field_1687 != null) {
            for (class_1657 p : context.client().field_1687.method_18456()) {
               if (p.method_5667().equals(payload.playerUuid()) && p instanceof class_742 ace) {
                  AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(ace);
                  if (stack != null) {
                     stack.removeLayer(0);
                  }
               }
            }
         }
      }));
   }

   public static void sendStartSit(UUID uuid, class_2960 anim) {
      ClientPlayNetworking.send(new SittingPlusNetworking.StartSitPayload(uuid, anim));
   }

   public static void sendStopSit(UUID uuid) {
      ClientPlayNetworking.send(new SittingPlusNetworking.StopSitPayload(uuid));
   }

   public record StartSitPayload(UUID playerUuid, class_2960 animId) implements class_8710 {
      public static final class_9154<SittingPlusNetworking.StartSitPayload> ID = new class_9154(SittingPlusNetworking.START_SIT_ID);
      public static final class_9139<class_2540, SittingPlusNetworking.StartSitPayload> CODEC = class_9139.method_56438((payload, buf) -> {
         buf.method_10797(payload.playerUuid());
         buf.method_10812(payload.animId());
      }, buf -> new SittingPlusNetworking.StartSitPayload(buf.method_10790(), buf.method_10810()));

      public class_9154<? extends class_8710> method_56479() {
         return ID;
      }

      static {
         PayloadTypeRegistry.playC2S().register(ID, CODEC);
         PayloadTypeRegistry.playS2C().register(ID, CODEC);
      }
   }

   public record StopSitPayload(UUID playerUuid) implements class_8710 {
      public static final class_9154<SittingPlusNetworking.StopSitPayload> ID = new class_9154(SittingPlusNetworking.STOP_SIT_ID);
      public static final class_9139<class_2540, SittingPlusNetworking.StopSitPayload> CODEC = class_9139.method_56438(
         (payload, buf) -> buf.method_10797(payload.playerUuid()), buf -> new SittingPlusNetworking.StopSitPayload(buf.method_10790())
      );

      public class_9154<? extends class_8710> method_56479() {
         return ID;
      }

      static {
         PayloadTypeRegistry.playC2S().register(ID, CODEC);
         PayloadTypeRegistry.playS2C().register(ID, CODEC);
      }
   }
}
