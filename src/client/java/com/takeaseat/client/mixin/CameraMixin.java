package com.takeaseat.client.mixin;

import com.takeaseat.TakeASeatConfig;
import com.takeaseat.client.TakeASeatClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow public abstract Vec3 position();

	@Shadow protected abstract void setPosition(Vec3 position);

	@Shadow public abstract boolean isDetached();

	@Unique private double takeaseat$smoothOffset = 0.0;

	@Inject(method = "update", at = @At("TAIL"))
	private void takeaseat$focusOnSittingPlayer(DeltaTracker deltaTracker, CallbackInfo ci) {
		TakeASeatConfig cfg = TakeASeatConfig.getConfig();
		boolean sitting = cfg.enableSitCameraFocus && TakeASeatClient.isSitting();
		if (sitting && cfg.onlyLowerCameraInFirstPerson && this.isDetached()) {
			sitting = false;
		}

		double target = sitting ? cfg.cameraFocusOffset : 0.0;
		this.takeaseat$smoothOffset += (target - this.takeaseat$smoothOffset) * 0.3;
		if (Math.abs(this.takeaseat$smoothOffset - target) < 1.0e-3) {
			this.takeaseat$smoothOffset = target;
		}

		if (this.takeaseat$smoothOffset > 1.0e-4) {
			Vec3 p = this.position();
			double drop = this.isDetached()
					? takeaseat$clampDrop(p, this.takeaseat$smoothOffset)
					: this.takeaseat$smoothOffset;
			if (drop > 1.0e-4) {
				this.setPosition(new Vec3(p.x, p.y - drop, p.z));
			}
		}
	}

	@Unique
	private static double takeaseat$clampDrop(Vec3 from, double drop) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		Entity cam = mc.getCameraEntity();
		if (level == null) return drop;

		for (int i = 0; i < 8; i++) {
			Vec3 corner = from.add(
					((i & 1) * 2 - 1) * 0.1,
					((i >> 1 & 1) * 2 - 1) * 0.1,
					((i >> 2 & 1) * 2 - 1) * 0.1);
			HitResult hit = level.clip(new ClipContext(corner, corner.subtract(0.0, drop, 0.0),
					ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cam));
			if (hit.getType() != HitResult.Type.MISS) {
				drop = Math.min(drop, hit.getLocation().distanceTo(from) - 0.1);
			}
		}
		return drop;
	}
}
