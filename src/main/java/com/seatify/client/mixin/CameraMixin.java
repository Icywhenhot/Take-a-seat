package com.seatify.client.mixin;

import com.seatify.SeatifyConfig;
import com.seatify.client.SeatifyClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the camera while the local player is sitting so the focus settles on the seated body.
 * Applied after {@code Camera.update} has positioned the camera for the frame; the offset is eased
 * in/out so it reads as a smooth camera move rather than a snap.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow public abstract Vec3 position();

	@Shadow protected abstract void setPosition(Vec3 position);

	@Shadow public abstract boolean isDetached();

	@Unique private double seatify$smoothOffset = 0.0;

	@Inject(method = "update", at = @At("TAIL"))
	private void seatify$focusOnSittingPlayer(DeltaTracker deltaTracker, CallbackInfo ci) {
		SeatifyConfig cfg = SeatifyConfig.getConfig();
		boolean sitting = cfg.enableSitCameraFocus && SeatifyClient.isSitting();
		// If configured, only apply the lower in first person (isDetached() == third person).
		if (sitting && cfg.onlyLowerCameraInFirstPerson && this.isDetached()) {
			sitting = false;
		}

		double target = sitting ? cfg.cameraFocusOffset : 0.0;
		this.seatify$smoothOffset += (target - this.seatify$smoothOffset) * 0.3;
		if (Math.abs(this.seatify$smoothOffset - target) < 1.0e-3) {
			this.seatify$smoothOffset = target;
		}

		if (this.seatify$smoothOffset > 1.0e-4) {
			Vec3 p = this.position();
			this.setPosition(new Vec3(p.x, p.y - this.seatify$smoothOffset, p.z));
		}
	}
}
