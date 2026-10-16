package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import com.takeaseat.network.TakeASeatNetworking;
import com.takeaseat.network.TakeASeatNetworking.StartSitPayload;
import com.takeaseat.network.TakeASeatNetworking.StopSitPayload;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TakeASeatClientNetworking {
	private TakeASeatClientNetworking() {}

	private static final Map<UUID, ResourceLocation> REMOTE_SITS = new ConcurrentHashMap<>();

	public static void handleStartSit(StartSitPayload payload) {
		Minecraft client = Minecraft.getInstance();
		Player self = client.player;
		if (self != null && self.getUUID().equals(payload.playerUuid())) return;
		REMOTE_SITS.put(payload.playerUuid(), payload.animId());
		ModifierLayer<IAnimation> controller = controllerFor(client, payload.playerUuid());
		boolean applied = controller != null && TakeASeatClient.play(controller, payload.animId());
		TakeASeat.LOGGER.info("[TakeASeat][net] recv StartSit for {} anim={} applied={} (entityLoaded={})",
				shortId(payload.playerUuid()), payload.animId().getPath(), applied, controller != null);
	}

	public static void handleStopSit(StopSitPayload payload) {
		Minecraft client = Minecraft.getInstance();
		Player self = client.player;

		if (self != null && self.getUUID().equals(payload.playerUuid())) {
			REMOTE_SITS.remove(payload.playerUuid());
			TakeASeat.LOGGER.debug("[TakeASeat][net] recv StopSit for self — ignored (local stand handled by standUp)");
			return;
		}
		REMOTE_SITS.remove(payload.playerUuid());
		ModifierLayer<IAnimation> controller = controllerFor(client, payload.playerUuid());
		if (controller != null) {
			controller.setAnimation(null);
		}
		TakeASeat.LOGGER.info("[TakeASeat][net] recv StopSit for {} stopped={}",
				shortId(payload.playerUuid()), controller != null);
	}

	public static void reconcile(Minecraft client) {
		if (client.level == null) {
			REMOTE_SITS.clear();
			return;
		}
		if (REMOTE_SITS.isEmpty()) return;
		for (Player p : client.level.players()) {
			if (p == client.player) continue;
			ResourceLocation id = REMOTE_SITS.get(p.getUUID());
			if (id == null || !(p instanceof AbstractClientPlayer)) continue;
			ModifierLayer<IAnimation> controller = controllerFor(p);
			if (controller != null && !controller.isActive()) {
				TakeASeatClient.play(controller, id);
				TakeASeat.LOGGER.info("[TakeASeat][net] reconcile re-applied sit {} to {} (entity loaded in late)",
						id.getPath(), shortId(p.getUUID()));
			}
		}
	}

	private static String shortId(UUID uuid) {
		return uuid.toString().substring(0, 8);
	}

	private static ModifierLayer<IAnimation> controllerFor(Minecraft client, UUID uuid) {
		if (client.level == null) return null;
		for (Player p : client.level.players()) {
			if (p.getUUID().equals(uuid) && p instanceof AbstractClientPlayer) {
				return controllerFor(p);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static ModifierLayer<IAnimation> controllerFor(Player player) {
		if (!(player instanceof AbstractClientPlayer clientPlayer)) return null;
		IAnimation layer = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(TakeASeatClient.SIT_LAYER);
		return layer instanceof ModifierLayer<?> ? (ModifierLayer<IAnimation>) layer : null;
	}

	public static void sendStartSit(UUID uuid, ResourceLocation anim) {
		if (!serverHasChannel()) {
			TakeASeat.LOGGER.debug("[TakeASeat][net] server has no start_sit channel — sitting locally only");
			return;
		}
		TakeASeat.LOGGER.info("[TakeASeat][net] send StartSit anim={}", anim.getPath());
		TakeASeatNetworking.CHANNEL.sendToServer(new StartSitPayload(uuid, anim));
	}

	public static void sendStopSit(UUID uuid) {
		if (!serverHasChannel()) {
			TakeASeat.LOGGER.debug("[TakeASeat][net] server has no stop_sit channel — standing locally only");
			return;
		}
		TakeASeat.LOGGER.info("[TakeASeat][net] send StopSit");
		TakeASeatNetworking.CHANNEL.sendToServer(new StopSitPayload(uuid));
	}

	private static boolean serverHasChannel() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null && TakeASeatNetworking.CHANNEL.isRemotePresent(connection.getConnection());
	}
}
