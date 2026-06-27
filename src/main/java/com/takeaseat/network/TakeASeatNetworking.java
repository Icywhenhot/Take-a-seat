package com.takeaseat.network;

import com.takeaseat.TakeASeat;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Both-sides networking. The client tells the server "player X started/stopped sitting with animation Y";
 * the server simply re-broadcasts that to every connected client so everyone mirrors the pose.
 */
public final class TakeASeatNetworking {
	public static final Identifier START_SIT_ID = TakeASeat.id("start_sit");
	public static final Identifier STOP_SIT_ID = TakeASeat.id("stop_sit");

	/** Server-side record of who is currently sitting (and with which animation), for resyncing late joiners. */
	private static final Map<UUID, Identifier> SITTING = new ConcurrentHashMap<>();

	private TakeASeatNetworking() {}

	/** Sent when a player begins a sitting animation. Carries who, and which animation. */
	public record StartSitPayload(UUID playerUuid, Identifier animId) implements CustomPacketPayload {
		public static final Type<StartSitPayload> TYPE = new Type<>(START_SIT_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, StartSitPayload> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, StartSitPayload::playerUuid,
				Identifier.STREAM_CODEC, StartSitPayload::animId,
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

		// Tell a joining player about everyone already sitting, and forget players who leave.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joined = handler.player;
			for (Map.Entry<UUID, Identifier> entry : SITTING.entrySet()) {
				if (entry.getKey().equals(joined.getUUID())) continue;
				ServerPlayNetworking.send(joined, new StartSitPayload(entry.getKey(), entry.getValue()));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SITTING.remove(handler.player.getUUID()));
	}
}
