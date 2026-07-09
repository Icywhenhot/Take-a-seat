package com.seatify.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.seatify.Seatify;
import com.seatify.SeatifyConfig;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class SeatifyClient implements ClientModInitializer {
	/** PAL animation-layer id under which the sitting controller is registered on every player. */
	public static final Identifier SIT_LAYER = Seatify.id("sit");
	/** Datapack/user-extensible tag of blocks that act as chairs. */
	public static final TagKey<Block> SITTABLE = TagKey.create(Registries.BLOCK, Seatify.id("sittable"));

	private static KeyMapping sitKey;
	private static boolean isSitting = false;
	private static CameraType previousPerspective = null;

	private int animationState = 0;
	private long lastActivityMs = System.currentTimeMillis();

	// --- diagnostics: track the controller's real animation state so we can flag desyncs ---
	private boolean diagPrevActive = false;
	private boolean diagWarnedDesync = false;

	// Animation sets (cycled through on repeated presses). All names live inside buttsit.json.
	private static final Identifier[] GROUND = ids("kneesitting", "buttsit", "buttsit2", "kneeleaning");
	private static final Identifier[] STAIRS = ids("chairsitting", "chairsitting2", "chairsitting3", "chairsitting4");
	private static final Identifier[] FENCES = ids("fencesitting", "fencesitting2");
	private static final Identifier[] BEDS = ids("bedlyingdown", "bedlyingdown2", "bedlyingdown3");
	private static final Identifier[] SWORD = ids("swordsit", "swordsit2");
	private static final Identifier[] AXE = ids("sittingaxe");
	private static final Identifier[] SHOVEL = ids("sittingshovel");
	private static final Identifier[] FISHING = ids("fishing");
	private static final Identifier[] CAMPFIRE = ids("campfiresit");
	private static final Identifier[] FURNACE = ids("furnacesit");

	private static final String[] POSE_NAMES =
			{"ground", "chair", "fence", "bed", "sword", "axe", "shovel", "fishing", "campfire", "furnace"};

	private static Identifier[] ids(String... names) {
		Identifier[] out = new Identifier[names.length];
		for (int i = 0; i < names.length; i++) out[i] = Seatify.id(names[i]);
		return out;
	}

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Seatify.id("sit"));
		sitKey = new KeyMapping("key.seatify.sit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, category);
		KeyMappingHelper.registerKeyMapping(sitKey);

		// Attach a sitting animation controller to every client player (local + remote).
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(SIT_LAYER, 1000,
				player -> new PlayerAnimationController(player,
						(controller, state, animationSetter) -> PlayState.STOP));

		SeatifyClientNetworking.registerClientReceivers();
		UseBlockCallback.EVENT.register(this::onRightClickBlock);
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
				dispatcher.register(ClientCommands.literal("sit")
						.executes(ctx -> {
							commandContextSit();
							return 1;
						})
						.then(ClientCommands.argument("pose", StringArgumentType.word())
								.suggests((c, b) -> {
									for (String p : POSE_NAMES) b.suggest(p);
									return b.buildFuture();
								})
								.executes(ctx -> runPoseCommand(ctx.getSource(), StringArgumentType.getString(ctx, "pose"), 1))
								.then(ClientCommands.argument("variant", IntegerArgumentType.integer(1))
										.executes(ctx -> runPoseCommand(ctx.getSource(),
												StringArgumentType.getString(ctx, "pose"),
												IntegerArgumentType.getInteger(ctx, "variant")))))));
	}

	// Right-click an empty hand on a stair block to sit on it.
	private InteractionResult onRightClickBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
		if (!SeatifyConfig.getConfig().enableClickToSit) return InteractionResult.PASS;
		if (!(player instanceof LocalPlayer local) || !world.isClientSide()) return InteractionResult.PASS;
		if (!local.getMainHandItem().isEmpty()) return InteractionResult.PASS;
		if (!canSit(local)) return InteractionResult.PASS;

		BlockPos pos = hit.getBlockPos();
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (!(block instanceof StairBlock)) return InteractionResult.PASS;

		double offset = 0.4;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		Direction facing = state.getValue(StairBlock.FACING);
		switch (facing) {
			case NORTH -> z += offset;
			case SOUTH -> z -= offset;
			case WEST -> x += offset;
			case EAST -> x -= offset;
			default -> {}
		}
		local.setPos(x, y, z);
		switch (facing) {
			case NORTH -> local.setYRot(0.0F);
			case SOUTH -> local.setYRot(180.0F);
			case WEST -> local.setYRot(270.0F);
			case EAST -> local.setYRot(90.0F);
			default -> {}
		}
		local.setXRot(0.0F);

		PlayerAnimationController controller = controllerFor(local);
		if (controller != null) {
			this.animationState = 0;
			playAnimation(controller, local, STAIRS);
		}
		return InteractionResult.SUCCESS;
	}

	private void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || !client.isWindowActive()) {
			SeatifyClientNetworking.reconcile(client);
			return;
		}

		Input in = player.input.keyPresses;
		// Directional keys + jump + sneak only. Deliberately NOT sprint: the sprint key reports "down"
		// whenever it is physically held (many players hold it permanently), which would make `moving`
		// perpetually true and stand you up the instant you sit. This mirrors the original mod, which
		// checked movementForward/Sideways + jumping + sneaking and never looked at sprint.
		boolean moving = in.forward() || in.backward() || in.left() || in.right() || in.jump() || in.shift();
		if (moving) lastActivityMs = System.currentTimeMillis();

		PlayerAnimationController controller = controllerFor(player);

		boolean pressedSitThisTick = false;
		if (sitKey.consumeClick() && client.screen == null) {
			if (controller != null) {
				Seatify.LOGGER.info("[Seatify] sit key pressed (isSitting={}, moving={})", isSitting, moving);
				handleSitPress(client, player, controller);
				pressedSitThisTick = true;
			} else {
				Seatify.LOGGER.warn("[Seatify] sit key pressed but the local player has no animation controller");
			}
		}

		// Moving cancels the sit (this is the only path that restores the camera).
		// Never cancel on the same tick we just (re)triggered a sit: the animation hasn't been committed
		// yet, so stopping it now would leave a dangling triggered animation that the next render frame
		// re-applies — a "resurrected" pose that can no longer be cancelled. Skipping one tick lets the
		// trigger commit; if the player is still moving next tick, the cancel fires cleanly then.
		if (isSitting && moving && !pressedSitThisTick) {
			standUp(controller, player, "movement[" + heldMovementKeys(in) + "]");
			this.animationState = 0;
		}

		// Optional AFK auto-sit (off by default; was dead code in the original mod).
		SeatifyConfig cfg = SeatifyConfig.getConfig();
		if (cfg.enableAfkSit && !isSitting && client.screen == null && canSit(player)) {
			long delayMs = cfg.afkSitDelaySeconds * 1000L;
			if (System.currentTimeMillis() - lastActivityMs >= delayMs && controller != null) {
				Seatify.LOGGER.info("[Seatify] AFK auto-sit firing after {}s idle", cfg.afkSitDelaySeconds);
				playAnimation(controller, player, GROUND);
			}
		}

		// Watchdog: flag the instant our sit flag disagrees with the real animation state.
		runDiagnostics(controller);

		// Keep remote players' poses in sync (covers late-loading entities after a join).
		SeatifyClientNetworking.reconcile(client);
	}

	private void handleSitPress(Minecraft client, LocalPlayer player, PlayerAnimationController controller) {
		if (!canSit(player)) {
			Seatify.LOGGER.info("[Seatify] sit blocked (canSit=false): onGround={} passenger={} inWater={} swimming={} fallFlying={} sleeping={}",
					player.onGround(), player.isPassenger(), player.isInWater(), player.isSwimming(), player.isFallFlying(), player.isSleeping());
			return;
		}
		// Note: we do NOT stop the current animation here. triggerAnimation() replaces it in place,
		// and calling stop would restore the camera mid-cycle (resetting a manual F5 change).
		Level level = player.level();

		// 1) Looking at a campfire or furnace?
		Vec3 eye = player.getEyePosition();
		Vec3 reach = player.getLookAngle().scale(2.0);
		Vec3 lookEnd = eye.add(reach);
		BlockHitResult look = level.clip(new ClipContext(eye, lookEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (look.getType() == HitResult.Type.BLOCK) {
			Block looked = level.getBlockState(look.getBlockPos()).getBlock();
			if (looked instanceof CampfireBlock) {
				playAnimation(controller, player, CAMPFIRE);
				return;
			}
			if (looked instanceof AbstractFurnaceBlock) {
				playAnimation(controller, player, FURNACE);
				return;
			}
		}

		// 2) Holding a tool/weapon/fishing rod?
		ItemStack held = player.getMainHandItem();
		if (held.is(ItemTags.SWORDS)) {
			playAnimation(controller, player, SWORD);
			return;
		}
		if (held.is(ItemTags.AXES)) {
			playAnimation(controller, player, AXE);
			return;
		}
		if (held.is(ItemTags.SHOVELS)) {
			playAnimation(controller, player, SHOVEL);
			return;
		}
		if (held.getItem() instanceof FishingRodItem) {
			playAnimation(controller, player, FISHING);
			return;
		}

		// 3) What am I standing on?
		Vec3 start = player.position();
		Vec3 down = new Vec3(player.getX(), player.getY() - 1.5, player.getZ());
		BlockHitResult ground = level.clip(new ClipContext(start, down, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (ground.getType() == HitResult.Type.BLOCK) {
			BlockState belowState = level.getBlockState(ground.getBlockPos());
			Block below = belowState.getBlock();
			if (below instanceof StairBlock) {
				playAnimation(controller, player, STAIRS);
				return;
			}
			if (below instanceof FenceBlock) {
				playAnimation(controller, player, FENCES);
				return;
			}
			if (below instanceof BedBlock) {
				playAnimation(controller, player, BEDS);
				return;
			}
			if (below instanceof SlabBlock && belowState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
				playAnimation(controller, player, STAIRS);
				return;
			}
			if (below instanceof CarpetBlock) {
				playAnimation(controller, player, GROUND);
				return;
			}
			if (belowState.is(SITTABLE, s -> true)) {
				playAnimation(controller, player, STAIRS);
				return;
			}
		}

		// 4) Default ground sit.
		playAnimation(controller, player, GROUND);
	}

	// ----- /sit command -----

	private void commandContextSit() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) return;
		PlayerAnimationController controller = controllerFor(player);
		if (controller != null) {
			handleSitPress(client, player, controller);
		}
	}

	private int runPoseCommand(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, String pose, int variant) {
		if (!commandSit(pose, variant)) {
			source.sendFeedback(Component.literal(
					"Seatify: unknown pose '" + pose + "'. Try one of: ground, chair, fence, bed, sword, axe, shovel, fishing, campfire, furnace"));
		}
		return 1;
	}

	/** @return false only if the pose name is unknown. */
	private boolean commandSit(String pose, int variant) {
		Identifier[] set = poseSet(pose);
		if (set == null) return false;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || !canSit(player)) return true;
		PlayerAnimationController controller = controllerFor(player);
		if (controller == null) return true;
		this.animationState = Math.floorMod(variant - 1, set.length);
		playAnimation(controller, player, set);
		return true;
	}

	private static Identifier[] poseSet(String name) {
		return switch (name.toLowerCase(Locale.ROOT)) {
			case "ground", "floor" -> GROUND;
			case "chair", "stairs", "stair" -> STAIRS;
			case "fence" -> FENCES;
			case "bed", "lie", "liedown" -> BEDS;
			case "sword" -> SWORD;
			case "axe" -> AXE;
			case "shovel" -> SHOVEL;
			case "fishing", "rod" -> FISHING;
			case "campfire", "fire" -> CAMPFIRE;
			case "furnace" -> FURNACE;
			default -> null;
		};
	}

	// ----- animation plumbing -----

	private void playAnimation(PlayerAnimationController controller, LocalPlayer player, Identifier[] animations) {
		if (controller == null || animations == null || animations.length == 0) return;
		if (this.animationState < 0 || this.animationState >= animations.length) this.animationState = 0;

		Identifier id = animations[this.animationState];
		boolean wasSitting = isSitting;
		boolean triggered = controller.triggerAnimation(id);
		Seatify.LOGGER.info("[Seatify] SIT anim={} variantIdx={} wasSitting={} triggerOk={}",
				id.getPath(), this.animationState, wasSitting, triggered);
		if (triggered) {
			isSitting = true;
			SeatifyClientNetworking.sendStartSit(player.getUUID(), id);
			this.animationState = (this.animationState + 1) % animations.length;
			lastActivityMs = System.currentTimeMillis();
			// Only switch perspective on the initial sit, so a manual F5 while seated is preserved.
			if (!wasSitting) {
				setThirdPersonIfEnabled();
			}
		} else {
			// The layer id is registered but this animation name isn't in buttsit.json (or failed to load).
			Seatify.LOGGER.warn("[Seatify] SIT failed: triggerAnimation returned false for '{}' — animation missing from the resource pack?", id);
		}
	}

	private void standUp(PlayerAnimationController controller, LocalPlayer player, String reason) {
		if (!isSitting || controller == null) {
			Seatify.LOGGER.debug("[Seatify] stand ignored (reason={}): isSitting={} controllerNull={}", reason, isSitting, controller == null);
			return;
		}
		// Clear the deferred triggered animation BEFORE stop(). stop() alone only sets the state to
		// STOPPED and leaves triggeredAnimation set; if we stood up in the same tick we sat (e.g. tapping
		// the sit key while a movement key is held), the animation hasn't been committed to
		// currentRawAnimation yet, so the next frame would rebuild and "resurrect" it — leaving us stuck
		// in the pose with isSitting already false, uncancellable until relog. stopTriggeredAnimation()
		// forgets the trigger so the stop actually sticks.
		boolean clearedTrigger = controller.stopTriggeredAnimation();
		controller.stop();
		isSitting = false;
		SeatifyClientNetworking.sendStopSit(player.getUUID());
		Seatify.LOGGER.info("[Seatify] STAND reason={} clearedTrigger={}", reason, clearedTrigger);
		if (SeatifyConfig.getConfig().enableThirdPersonOnSit && previousPerspective != null) {
			Minecraft.getInstance().options.setCameraType(previousPerspective);
			previousPerspective = null;
		}
	}

	/** Comma-separated list of the movement keys currently held — for the STAND log line. */
	private static String heldMovementKeys(Input in) {
		StringBuilder sb = new StringBuilder();
		if (in.forward()) sb.append("forward,");
		if (in.backward()) sb.append("backward,");
		if (in.left()) sb.append("left,");
		if (in.right()) sb.append("right,");
		if (in.jump()) sb.append("jump,");
		if (in.shift()) sb.append("sneak,");
		if (sb.length() > 0) sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Per-tick watchdog. Logs controller-active transitions, and WARNs the moment our {@link #isSitting}
	 * flag disagrees with the controller's real animation state — the exact signature of the stuck-sit bug.
	 */
	private void runDiagnostics(PlayerAnimationController controller) {
		boolean active = controller != null && controller.isActive();
		if (active != diagPrevActive) {
			Seatify.LOGGER.info("[Seatify][diag] controller.isActive {} -> {} (isSitting={})", diagPrevActive, active, isSitting);
			diagPrevActive = active;
		}
		if (isSitting != active) {
			if (!diagWarnedDesync) {
				if (!isSitting && active) {
					Seatify.LOGGER.warn("[Seatify][diag] DESYNC: animation is ACTIVE but isSitting=false. "
							+ "This is the stuck-sit signature (a resurrected pose); the move-to-stand path will NOT fire, "
							+ "so the player is stuck until relog. Something stopped us mid-trigger — check the log just above for a STAND or network stop.");
				} else {
					Seatify.LOGGER.warn("[Seatify][diag] DESYNC: isSitting=true but the animation is NOT active. "
							+ "The pose ended without going through standUp() (e.g. animation finished on its own or was stopped by the network).");
				}
				diagWarnedDesync = true;
			}
		} else if (diagWarnedDesync) {
			Seatify.LOGGER.info("[Seatify][diag] desync resolved (isSitting={}, active={})", isSitting, active);
			diagWarnedDesync = false;
		}
	}

	private void setThirdPersonIfEnabled() {
		if (!SeatifyConfig.getConfig().enableThirdPersonOnSit) return;
		Minecraft client = Minecraft.getInstance();
		CameraType current = client.options.getCameraType();
		if (current == CameraType.FIRST_PERSON) {
			previousPerspective = current;
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else {
			previousPerspective = null;
		}
	}

	private static boolean canSit(LocalPlayer player) {
		return player.onGround()
				&& !player.isPassenger()
				&& !player.isInWater()
				&& !player.isSwimming()
				&& !player.isFallFlying()
				&& !player.isSleeping();
	}

	private static PlayerAnimationController controllerFor(Player player) {
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, SIT_LAYER);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}

	/** Whether the local player is currently in a Seatify sitting animation (read by the camera mixin). */
	public static boolean isSitting() {
		return isSitting;
	}
}
