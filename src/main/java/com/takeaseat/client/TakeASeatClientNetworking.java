package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import com.takeaseat.network.TakeASeatNetworking.StartSitPayload;
import com.takeaseat.network.TakeASeatNetworking.StopSitPayload;
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
 * players' avatars. The local player is animated directly (see {@link TakeASeatClient}), so we skip ourselves.
 *
 * <p>{@link #REMOTE_SITS} remembers who is currently sitting so that a player whose entity loads in late
 * (e.g. you just joined and they were already seated) still gets their pose applied — see {@link #reconcile}.
 */
public final class TakeASeatClientNetworking {
	private TakeASeatClientNetworking() {}

	private static final Map<UUID, Identifier> REMOTE_SITS = new ConcurrentHashMap<>();

	public static void registerClientReceivers(RegisterClientPayloadHandlersEvent event) {
		event.register(StartSitPayload.TYPE, TakeASeatClientNetworking::handleStartSit);
		event.register(StopSitPayload.TYPE, TakeASeatClientNetworking::handleStopSit);
	}

	private static void handleStartSit(StartSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft client = Minecraft.getInstance();
			Player self = client.player;
			if (self != null && self.getUUID().equals(payload.playerUuid())) return; // we animate ourselves
			REMOTE_SITS.put(payload.playerUuid(), payload.animId());
			PlayerAnimationController controller = controllerFor(client, payload.playerUuid());
			boolean applied = controller != null && controller.triggerAnimation(payload.animId());
			TakeASeat.LOGGER.info("[TakeASeat][net] recv StartSit for {} anim={} applied={} (entityLoaded={})",
					shortId(payload.playerUuid()), payload.animId().getPath(), applied, controller != null);
		});
	}

	private static void handleStopSit(StopSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft client = Minecraft.getInstance();
			Player self = client.player;
			// Skip our own echo, exactly like handleStartSit does. The local player's stop is driven
			// directly by TakeASeatClient#standUp; letting a delayed self-echo also call stop() here can
			// land on top of a fresh re-sit and, if it hits before the new animation commits, resurrect it
			// into a stuck pose. The server broadcasts to everyone including the sender, so without this
			// guard we would always double-handle ourselves.
			if (self != null && self.getUUID().equals(payload.playerUuid())) {
				REMOTE_SITS.remove(payload.playerUuid());
				TakeASeat.LOGGER.debug("[TakeASeat][net] recv StopSit for self — ignored (local stand handled by standUp)");
				return;
			}
			REMOTE_SITS.remove(payload.playerUuid());
			PlayerAnimationController controller = controllerFor(client, payload.playerUuid());
			if (controller != null) {
				// stopTriggeredAnimation() before stop() so the stop is durable even when a
				// StartSit+StopSit pair arrives within a single tick (same resurrection race as the
				// local player — see TakeASeatClient#standUp). Without it a remote player can get stuck
				// in the sit pose.
				controller.stopTriggeredAnimation();
				controller.stop();
			}
			TakeASeat.LOGGER.info("[TakeASeat][net] recv StopSit for {} stopped={}",
					shortId(payload.playerUuid()), controller != null);
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
				TakeASeat.LOGGER.info("[TakeASeat][net] reconcile re-applied sit {} to {} (entity loaded in late)",
						id.getPath(), shortId(p.getUUID()));
			}
		}
	}

	/** First 8 chars of a UUID — enough to correlate log lines without dumping the whole thing. */
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
		ClientPacketDistributor.sendToServer(new StartSitPayload(uuid, anim));
	}

	public static void sendStopSit(UUID uuid) {
		TakeASeat.LOGGER.info("[TakeASeat][net] send StopSit");
		ClientPacketDistributor.sendToServer(new StopSitPayload(uuid));
	}
}
