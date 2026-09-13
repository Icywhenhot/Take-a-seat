package com.takeaseat.network;

import com.takeaseat.TakeASeat;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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

	public static void registerCommon() {
		PayloadTypeRegistry.playC2S().register(StartSitPayload.TYPE, StartSitPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StartSitPayload.TYPE, StartSitPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StopSitPayload.TYPE, StopSitPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StopSitPayload.TYPE, StopSitPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(StartSitPayload.TYPE, (payload, context) -> {
			ServerPlayer sender = context.player();
			MinecraftServer server = sender.level().getServer();
			if (server == null) return;
			SITTING.put(payload.playerUuid(), payload.animId());
			server.execute(() -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					ServerPlayNetworking.send(p, payload);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(StopSitPayload.TYPE, (payload, context) -> {
			ServerPlayer sender = context.player();
			MinecraftServer server = sender.level().getServer();
			if (server == null) return;
			SITTING.remove(payload.playerUuid());
			server.execute(() -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					ServerPlayNetworking.send(p, payload);
				}
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joined = handler.player;
			for (Map.Entry<UUID, ResourceLocation> entry : SITTING.entrySet()) {
				if (entry.getKey().equals(joined.getUUID())) continue;
				ServerPlayNetworking.send(joined, new StartSitPayload(entry.getKey(), entry.getValue()));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SITTING.remove(handler.player.getUUID()));
	}
}
