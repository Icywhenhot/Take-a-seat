package com.takeaseat.network;

import com.takeaseat.TakeASeat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Both-sides networking. The client tells the server "player X started/stopped sitting with animation Y";
 * the server simply re-broadcasts that to every connected client so everyone mirrors the pose.
 */
public final class TakeASeatNetworking {
	public static final ResourceLocation START_SIT_ID = TakeASeat.id("start_sit");
	public static final ResourceLocation STOP_SIT_ID = TakeASeat.id("stop_sit");

	/** Server-side record of who is currently sitting (and with which animation), for resyncing late joiners. */
	private static final Map<UUID, ResourceLocation> SITTING = new ConcurrentHashMap<>();

	private TakeASeatNetworking() {}

	/** Sent when a player begins a sitting animation. Carries who, and which animation. */
	public record StartSitPayload(UUID playerUuid, ResourceLocation animId) implements CustomPacketPayload {
		public static final Type<StartSitPayload> TYPE = new Type<>(START_SIT_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, StartSitPayload> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, StartSitPayload::playerUuid,
				ResourceLocation.STREAM_CODEC, StartSitPayload::animId,
				StartSitPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Sent when a player stops sitting. */
	public record StopSitPayload(UUID playerUuid) implements CustomPacketPayload {
		public static final Type<StopSitPayload> TYPE = new Type<>(STOP_SIT_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, StopSitPayload> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, StopSitPayload::playerUuid,
				StopSitPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playBidirectional(StartSitPayload.TYPE, StartSitPayload.CODEC, TakeASeatNetworking::handleStartSit);
		registrar.playBidirectional(StopSitPayload.TYPE, StopSitPayload.CODEC, TakeASeatNetworking::handleStopSit);
	}

	private static void handleStartSit(StartSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			// Serverbound: context.player() is the sending ServerPlayer.
			if (!(context.player() instanceof ServerPlayer)) {
				return;
			}
			SITTING.put(payload.playerUuid(), payload.animId());
			PacketDistributor.sendToAllPlayers(payload);
		});
	}

	private static void handleStopSit(StopSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer)) {
				return;
			}
			SITTING.remove(payload.playerUuid());
			PacketDistributor.sendToAllPlayers(payload);
		});
	}

	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer joined)) {
			return;
		}
		for (Map.Entry<UUID, ResourceLocation> entry : SITTING.entrySet()) {
			if (entry.getKey().equals(joined.getUUID())) {
				continue;
			}
			PacketDistributor.sendToPlayer(joined, new StartSitPayload(entry.getKey(), entry.getValue()));
		}
	}

	public static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		if (server == null || SITTING.isEmpty()) {
			return;
		}
		var online = server.getPlayerList().getPlayers();
		SITTING.keySet().removeIf(uuid -> online.stream().noneMatch(player -> player.getUUID().equals(uuid)));
	}
}
