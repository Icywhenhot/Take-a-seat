package com.saltywater.sittingplus;

import com.saltywater.sittingplus.networking.SittingPlusNetworking;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1787;
import net.minecraft.class_1792;
import net.minecraft.class_1821;
import net.minecraft.class_1829;
import net.minecraft.class_1937;
import net.minecraft.class_2244;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2354;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2510;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3865;
import net.minecraft.class_3922;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4587;
import net.minecraft.class_5498;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3675.class_307;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

public class SittingPlusClient implements ClientModInitializer {
   private static class_304 sitKey;
   private static KeyframeAnimationPlayer sitAnimationPlayer;
   private static class_5498 previousPerspective = null;
   private int animationState = 0;
   private static final class_2960[] ANIMATION_IDS_STAIRS = new class_2960[]{
      class_2960.method_60655("sittingplus", "chairsitting"),
      class_2960.method_60655("sittingplus", "chairsitting2"),
      class_2960.method_60655("sittingplus", "chairsitting3"),
      class_2960.method_60655("sittingplus", "chairsitting4")
   };
   private static final class_2960[] ANIMATION_IDS_GROUND = new class_2960[]{
      class_2960.method_60655("sittingplus", "kneesitting"),
      class_2960.method_60655("sittingplus", "buttsit"),
      class_2960.method_60655("sittingplus", "buttsit2"),
      class_2960.method_60655("sittingplus", "kneeleaning")
   };
   private static final class_2960[] ANIMATION_IDS_FENCES = new class_2960[]{
      class_2960.method_60655("sittingplus", "fencesitting"), class_2960.method_60655("sittingplus", "fencesitting2")
   };
   private static final class_2960[] ANIMATION_IDS_BEDS = new class_2960[]{
      class_2960.method_60655("sittingplus", "bedlyingdown"),
      class_2960.method_60655("sittingplus", "bedlyingdown2"),
      class_2960.method_60655("sittingplus", "bedlyingdown3")
   };
   private static final class_2960[] ANIMATION_IDS_SWORD = new class_2960[]{
      class_2960.method_60655("sittingplus", "swordsit"), class_2960.method_60655("sittingplus", "swordsit2")
   };
   private static final class_2960[] ANIMATION_IDS_AXE = new class_2960[]{class_2960.method_60655("sittingplus", "sittingaxe")};
   private static final class_2960[] ANIMATION_IDS_SHOVEL = new class_2960[]{class_2960.method_60655("sittingplus", "sittingshovel")};
   private static final class_2960[] ANIMATION_IDS_FISHING_ROD = new class_2960[]{class_2960.method_60655("sittingplus", "fishing")};

   public void onInitializeClient() {
      sitKey = new class_304("key.sittingplus.sit", class_307.field_1668, 88, "category.sittingplus");
      KeyBindingHelper.registerKeyBinding(sitKey);
      SittingPlusNetworking.registerClientReceivers();
      UseBlockCallback.EVENT.register(this::onRightClickBlock);
      ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
      WorldRenderEvents.START.register(this::onWorldRenderStart);
   }

   private class_1269 onRightClickBlock(class_1657 player, class_1937 world, class_1268 hand, class_3965 hit) {
      if (!SittingPlusConfig.getConfig().enableClickToSit) {
         return class_1269.field_5811;
      } else if (!(player instanceof class_746 local && world.field_9236)) {
         return class_1269.field_5811;
      } else if (!local.method_6047().method_7960()) {
         return class_1269.field_5811;
      } else {
         class_2338 pos = hit.method_17777();
         class_2680 state = world.method_8320(pos);
         class_2248 block = state.method_26204();
         if (block instanceof class_2510) {
            double offset = 0.4;
            double x = pos.method_10263() + 0.5;
            double y = pos.method_10264() + 0.5;
            double z = pos.method_10260() + 0.5;
            switch ((class_2350)state.method_11654(class_2510.field_11571)) {
               case field_11043:
                  z += offset;
                  break;
               case field_11035:
                  z -= offset;
                  break;
               case field_11039:
                  x += offset;
                  break;
               case field_11034:
                  x -= offset;
            }

            local.method_23327(x, y, z);
            switch ((class_2350)state.method_11654(class_2510.field_11571)) {
               case field_11043:
                  local.method_36456(0.0F);
                  break;
               case field_11035:
                  local.method_36456(180.0F);
                  break;
               case field_11039:
                  local.method_36456(270.0F);
                  break;
               case field_11034:
                  local.method_36456(90.0F);
            }

            local.method_36457(0.0F);
            AnimationStack animStack = PlayerAnimationAccess.getPlayerAnimLayer(local);
            if (animStack != null) {
               this.stopCurrentAnimation(animStack);
               this.animationState = 0;
               this.playAnimation(animStack, ANIMATION_IDS_STAIRS);
               this.setThirdPersonIfEnabled();
            }

            return class_1269.field_5812;
         } else {
            return class_1269.field_5811;
         }
      }
   }

   private void onClientTick(class_310 client) {
      class_746 player = client.field_1724;
      if (player != null && client.method_1569()) {
         AnimationStack animStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
         if (sitKey.method_1436() && animStack != null) {
            this.stopCurrentAnimation(animStack);
            class_243 eye = player.method_33571();
            class_243 dir = player.method_5720().method_1021(2.0);
            class_243 hitEnd = eye.method_1019(dir);
            class_239 trace = client.field_1687.method_17742(new class_3959(eye, hitEnd, class_3960.field_17559, class_242.field_1348, player));
            if (trace.method_17783() == class_240.field_1332) {
               class_2338 bp = ((class_3965)trace).method_17777();
               class_2248 b = client.field_1687.method_8320(bp).method_26204();
               if (b instanceof class_3922) {
                  this.playAnimation(animStack, new class_2960[]{class_2960.method_60655("sittingplus", "campfiresit")});
                  this.setThirdPersonIfEnabled();
                  return;
               }

               if (b instanceof class_3865) {
                  this.playAnimation(animStack, new class_2960[]{class_2960.method_60655("sittingplus", "furnacesit")});
                  this.setThirdPersonIfEnabled();
                  return;
               }
            }

            class_1792 held = player.method_6047().method_7909();
            if (held instanceof class_1829) {
               this.playAnimation(animStack, ANIMATION_IDS_SWORD);
               return;
            }

            if (held instanceof class_1743) {
               this.playAnimation(animStack, ANIMATION_IDS_AXE);
               return;
            }

            if (held instanceof class_1821) {
               this.playAnimation(animStack, ANIMATION_IDS_SHOVEL);
               return;
            }

            if (held instanceof class_1787) {
               this.playAnimation(animStack, ANIMATION_IDS_FISHING_ROD);
               return;
            }

            class_243 start = player.method_19538();
            class_243 end = new class_243(player.method_23317(), player.method_23318() - 1.5, player.method_23321());
            class_3965 gr = client.field_1687.method_17742(new class_3959(start, end, class_3960.field_17558, class_242.field_1348, player));
            if (gr.method_17783() == class_240.field_1332) {
               class_2338 bpx = gr.method_17777();
               class_2248 gg = client.field_1687.method_8320(bpx).method_26204();
               if (gg instanceof class_2510) {
                  this.playAnimation(animStack, ANIMATION_IDS_STAIRS);
                  this.setThirdPersonIfEnabled();
                  return;
               }

               if (gg instanceof class_2354) {
                  this.playAnimation(animStack, ANIMATION_IDS_FENCES);
                  this.setThirdPersonIfEnabled();
                  return;
               }

               if (gg instanceof class_2244) {
                  this.playAnimation(animStack, ANIMATION_IDS_BEDS);
                  this.setThirdPersonIfEnabled();
                  return;
               }
            }

            this.playAnimation(animStack, ANIMATION_IDS_GROUND);
            this.setThirdPersonIfEnabled();
         }

         if (animStack != null) {
            boolean moving = player.field_3913.field_3905 != 0.0F
               || player.field_3913.field_3907 != 0.0F
               || player.field_3913.field_3904
               || player.field_3913.field_3903;
            if (moving) {
               this.stopCurrentAnimation(animStack);
               this.animationState = 0;
            }
         }

         long delayMs = SittingPlusConfig.getConfig().afkSitDelaySeconds * 1000L;
         long now = System.currentTimeMillis();
         if (now - delayMs >= now && sitAnimationPlayer == null && animStack != null) {
            this.playAnimation(animStack, ANIMATION_IDS_GROUND);
            this.setThirdPersonIfEnabled();
         }
      }
   }

   private void onWorldRenderStart(WorldRenderContext ctx) {
      if (sitAnimationPlayer != null) {
         if (!SittingPlusConfig.getConfig().onlyLowerCameraInFirstPerson || class_310.method_1551().field_1690.method_31044() == class_5498.field_26664) {
            class_4587 stack = ctx.matrixStack();
            if (stack != null) {
               stack.method_22904(0.0, 0.7, 0.0);
            }
         }
      }
   }

   private void playAnimation(AnimationStack stack, class_2960[] animations) {
      if (animations != null && animations.length != 0) {
         if (this.animationState < 0 || this.animationState >= animations.length) {
            this.animationState = 0;
         }

         class_2960 id = animations[this.animationState];
         KeyframeAnimation data = (KeyframeAnimation)PlayerAnimationRegistry.getAnimation(id);
         if (data != null) {
            sitAnimationPlayer = new KeyframeAnimationPlayer(data);
            stack.addAnimLayer(0, sitAnimationPlayer);
            class_746 player = class_310.method_1551().field_1724;
            if (player != null) {
               SittingPlusNetworking.sendStartSit(player.method_5667(), id);
            }

            this.animationState = (this.animationState + 1) % animations.length;
         }
      }
   }

   private void stopCurrentAnimation(AnimationStack stack) {
      if (sitAnimationPlayer != null && stack != null) {
         stack.removeLayer(0);
         sitAnimationPlayer = null;
         class_310 client = class_310.method_1551();
         class_746 player = client.field_1724;
         if (player != null) {
            SittingPlusNetworking.sendStopSit(player.method_5667());
            if (SittingPlusConfig.getConfig().enableThirdPersonOnSit && previousPerspective != null) {
               client.field_1690.method_31043(previousPerspective);
               previousPerspective = null;
            }
         }
      }
   }

   private void setThirdPersonIfEnabled() {
      if (SittingPlusConfig.getConfig().enableThirdPersonOnSit) {
         class_310 client = class_310.method_1551();
         class_5498 current = client.field_1690.method_31044();
         if (current == class_5498.field_26664) {
            previousPerspective = current;
            client.field_1690.method_31043(class_5498.field_26665);
         } else {
            previousPerspective = null;
         }
      }
   }
}
