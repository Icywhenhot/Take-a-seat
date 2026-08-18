package com.takeaseat.network;

import com.takeaseat.TakeASeat;
import net.minecraft.core.UUIDUtil;
import com.takeaseat.client.TakeASeatClientNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
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

public final class TakeASeatNetworking {
	public static final ResourceLocation START_SIT_ID = TakeASeat.id("start_sit");
	public static final ResourceLocation STOP_SIT_ID = TakeASeat.id("stop_sit");

	private static final Map<UUID, ResourceLocation> SITTING = new ConcurrentHashMap<>();

	private TakeASeatNetworking() {}

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

		PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playBidirectional(StartSitPayload.TYPE, StartSitPayload.CODEC, (payload, context) -> {
			if (context.flow() == PacketFlow.CLIENTBOUND) {
				TakeASeatClientNetworking.handleStartSit(payload, context);
			} else {
				handleStartSit(payload, context);
			}
		});
		registrar.playBidirectional(StopSitPayload.TYPE, StopSitPayload.CODEC, (payload, context) -> {
			if (context.flow() == PacketFlow.CLIENTBOUND) {
				TakeASeatClientNetworking.handleStopSit(payload, context);
			} else {
				handleStopSit(payload, context);
			}
		});
	}

	private static void handleStartSit(StartSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer sender)) {
				return;
			}
			SITTING.put(payload.playerUuid(), payload.animId());
			TakeASeat.LOGGER.info("[TakeASeat][server] StartSit from {} anim={} — rebroadcasting to all players",
					sender.getName().getString(), payload.animId().getPath());
			PacketDistributor.sendToAllPlayers(payload);
		});
	}

	private static void handleStopSit(StopSitPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer sender)) {
				return;
			}
			SITTING.remove(payload.playerUuid());
			TakeASeat.LOGGER.info("[TakeASeat][server] StopSit from {} — rebroadcasting to all players",
					sender.getName().getString());
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
