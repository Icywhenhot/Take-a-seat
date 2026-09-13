package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import com.takeaseat.network.TakeASeatNetworking.StartSitPayload;
import com.takeaseat.network.TakeASeatNetworking.StopSitPayload;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TakeASeatClientNetworking {
	private TakeASeatClientNetworking() {}

	private static final Map<UUID, Identifier> REMOTE_SITS = new ConcurrentHashMap<>();

	public static void registerClientReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(StartSitPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					Player self = context.client().player;
					if (self != null && self.getUUID().equals(payload.playerUuid())) return;
					REMOTE_SITS.put(payload.playerUuid(), payload.animId());
					PlayerAnimationController controller = controllerFor(context.client(), payload.playerUuid());
					boolean applied = controller != null && controller.triggerAnimation(payload.animId());
					TakeASeat.LOGGER.info("[TakeASeat][net] recv StartSit for {} anim={} applied={} (entityLoaded={})",
							shortId(payload.playerUuid()), payload.animId().getPath(), applied, controller != null);
				}));

		ClientPlayNetworking.registerGlobalReceiver(StopSitPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					Player self = context.client().player;
					if (self != null && self.getUUID().equals(payload.playerUuid())) {
						REMOTE_SITS.remove(payload.playerUuid());
						TakeASeat.LOGGER.debug("[TakeASeat][net] recv StopSit for self — ignored (local stand handled by standUp)");
						return;
					}
					REMOTE_SITS.remove(payload.playerUuid());
					PlayerAnimationController controller = controllerFor(context.client(), payload.playerUuid());
					if (controller != null) {
						controller.stopTriggeredAnimation();
						controller.stop();
					}
					TakeASeat.LOGGER.info("[TakeASeat][net] recv StopSit for {} stopped={}",
							shortId(payload.playerUuid()), controller != null);
				}));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REMOTE_SITS.clear());
	}

	public static void reconcile(Minecraft client) {
		if (REMOTE_SITS.isEmpty() || client.level == null) return;
		for (Player p : client.level.players()) {
			if (p == client.player) continue;
			Identifier id = REMOTE_SITS.get(p.getUUID());
			if (id == null || !(p instanceof AbstractClientPlayer)) continue;
			PlayerAnimationController controller = controllerFor(p);
			if (controller != null && !controller.isActive()) {
				controller.triggerAnimation(id);
				TakeASeat.LOGGER.info("[TakeASeat][net] reconcile re-applied sit {} to {} (entity loaded in late)",
						id.getPath(), shortId(p.getUUID()));
			}
		}
	}

	private static String shortId(UUID uuid) {
		return uuid.toString().substring(0, 8);
	}

	private static PlayerAnimationController controllerFor(Minecraft client, UUID uuid) {
		if (client.level == null) return null;
		for (Player p : client.level.players()) {
			if (p.getUUID().equals(uuid) && p instanceof AbstractClientPlayer) {
				return controllerFor(p);
			}
		}
		return null;
	}

	private static PlayerAnimationController controllerFor(Player player) {
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, TakeASeatClient.SIT_LAYER);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}

	public static void sendStartSit(UUID uuid, Identifier anim) {
		TakeASeat.LOGGER.info("[TakeASeat][net] send StartSit anim={}", anim.getPath());
		ClientPlayNetworking.send(new StartSitPayload(uuid, anim));
	}

	public static void sendStopSit(UUID uuid) {
		TakeASeat.LOGGER.info("[TakeASeat][net] send StopSit");
		ClientPlayNetworking.send(new StopSitPayload(uuid));
	}
}
