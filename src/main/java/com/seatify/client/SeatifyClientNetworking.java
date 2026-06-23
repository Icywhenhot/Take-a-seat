package com.seatify.client;

import com.seatify.network.SeatifyNetworking.StartSitPayload;
import com.seatify.network.SeatifyNetworking.StopSitPayload;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side networking: applies the sit/stand animations broadcast by the server onto <em>other</em>
 * players' avatars. The local player is animated directly (see {@link SeatifyClient}), so we skip ourselves.
 *
 * <p>{@link #REMOTE_SITS} remembers who is currently sitting so that a player whose entity loads in late
 * (e.g. you just joined and they were already seated) still gets their pose applied — see {@link #reconcile}.
 */
public final class SeatifyClientNetworking {
	private SeatifyClientNetworking() {}

	private static final Map<UUID, Identifier> REMOTE_SITS = new ConcurrentHashMap<>();

	public static void registerClientReceivers(RegisterClientPayloadHandlersEvent event) {
		event.register(StartSitPayload.TYPE, SeatifyClientNetworking::handleStartSit);
		event.register(StopSitPayload.TYPE, SeatifyClientNetworking::handleStopSit);
	}

	private static void handleStartSit(StartSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft client = Minecraft.getInstance();
			Player self = client.player;
			if (self != null && self.getUUID().equals(payload.playerUuid())) return; // we animate ourselves
			REMOTE_SITS.put(payload.playerUuid(), payload.animId());
			PlayerAnimationController controller = controllerFor(client, payload.playerUuid());
			if (controller != null) controller.triggerAnimation(payload.animId());
		});
	}

	private static void handleStopSit(StopSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft client = Minecraft.getInstance();
			REMOTE_SITS.remove(payload.playerUuid());
			PlayerAnimationController controller = controllerFor(client, payload.playerUuid());
			if (controller != null) controller.stop();
		});
	}

	/** Re-apply sits to players whose entity has since loaded (e.g. you just joined). Cheap no-op when idle. */
	public static void reconcile(Minecraft client) {
		if (client.level == null) {
			REMOTE_SITS.clear();
			return;
		}
		if (REMOTE_SITS.isEmpty()) return;
		for (Player p : client.level.players()) {
			if (p == client.player) continue;
			Identifier id = REMOTE_SITS.get(p.getUUID());
			if (id == null || !(p instanceof AbstractClientPlayer)) continue;
			PlayerAnimationController controller = controllerFor(p);
			if (controller != null && !controller.isActive()) {
				controller.triggerAnimation(id);
			}
		}
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
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, SeatifyClient.SIT_LAYER);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}

	public static void sendStartSit(UUID uuid, Identifier anim) {
		ClientPacketDistributor.sendToServer(new StartSitPayload(uuid, anim));
	}

	public static void sendStopSit(UUID uuid) {
		ClientPacketDistributor.sendToServer(new StopSitPayload(uuid));
	}
}
