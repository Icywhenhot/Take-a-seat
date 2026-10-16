package com.takeaseat.network;

import com.takeaseat.TakeASeat;
import com.takeaseat.client.TakeASeatClientNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TakeASeatNetworking {
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(TakeASeat.id("sitting"),
			() -> "1", NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));

	private static final Map<UUID, ResourceLocation> SITTING = new ConcurrentHashMap<>();

	private TakeASeatNetworking() {}

	public record StartSitPayload(UUID playerUuid, ResourceLocation animId) {
		private static StartSitPayload decode(FriendlyByteBuf buf) {
			return new StartSitPayload(buf.readUUID(), buf.readResourceLocation());
		}

		private void encode(FriendlyByteBuf buf) {
			buf.writeUUID(playerUuid);
			buf.writeResourceLocation(animId);
		}
	}

	public record StopSitPayload(UUID playerUuid) {
		private static StopSitPayload decode(FriendlyByteBuf buf) {
			return new StopSitPayload(buf.readUUID());
		}

		private void encode(FriendlyByteBuf buf) {
			buf.writeUUID(playerUuid);
		}
	}

	public static void register() {
		CHANNEL.registerMessage(0, StartSitPayload.class, StartSitPayload::encode, StartSitPayload::decode, TakeASeatNetworking::handleStartSit);
		CHANNEL.registerMessage(1, StopSitPayload.class, StopSitPayload::encode, StopSitPayload::decode, TakeASeatNetworking::handleStopSit);
	}

	private static void handleStartSit(StartSitPayload payload, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			if (context.getDirection().getReceptionSide().isClient()) {
				TakeASeatClientNetworking.handleStartSit(payload);
				return;
			}
			ServerPlayer sender = context.getSender();
			if (sender == null || !sender.getUUID().equals(payload.playerUuid())) return;
			SITTING.put(payload.playerUuid(), payload.animId());
			for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
				sendToPlayer(player, payload);
			}
		});
		context.setPacketHandled(true);
	}

	private static void handleStopSit(StopSitPayload payload, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			if (context.getDirection().getReceptionSide().isClient()) {
				TakeASeatClientNetworking.handleStopSit(payload);
				return;
			}
			ServerPlayer sender = context.getSender();
			if (sender == null || !sender.getUUID().equals(payload.playerUuid())) return;
			SITTING.remove(payload.playerUuid());
			for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
				sendToPlayer(player, payload);
			}
		});
		context.setPacketHandled(true);
	}

	private static void sendToPlayer(ServerPlayer player, Object payload) {
		if (CHANNEL.isRemotePresent(player.connection.connection)) {
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
		}
	}

	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer joined)) return;
		for (Map.Entry<UUID, ResourceLocation> entry : SITTING.entrySet()) {
			if (entry.getKey().equals(joined.getUUID())) continue;
			sendToPlayer(joined, new StartSitPayload(entry.getKey(), entry.getValue()));
		}
	}

	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		var server = event.getServer();
		if (server == null || SITTING.isEmpty()) return;
		var online = server.getPlayerList().getPlayers();
		SITTING.keySet().removeIf(uuid -> online.stream().noneMatch(player -> player.getUUID().equals(uuid)));
	}
}
