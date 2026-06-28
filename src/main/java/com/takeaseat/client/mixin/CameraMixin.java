package com.takeaseat.client.mixin;

import com.takeaseat.TakeASeatConfig;
import com.takeaseat.client.TakeASeatClient;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the camera while the local player is sitting so the focus settles on the seated body.
 * Applied after {@code Camera.setup} has positioned the camera for the frame; the offset is eased
 * in/out so it reads as a smooth camera move rather than a snap.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow public abstract Vec3 position();

	@Shadow protected abstract void setPosition(Vec3 position);

	@Shadow public abstract boolean isDetached();

	@Unique private double takeaseat$smoothOffset = 0.0;

	@Inject(method = "setup", at = @At("TAIL"))
	private void takeaseat$focusOnSittingPlayer(CallbackInfo ci) {
		TakeASeatConfig cfg = TakeASeatConfig.getConfig();
		boolean sitting = cfg.enableSitCameraFocus && TakeASeatClient.isSitting();
		// If configured, only apply the lower in first person (isDetached() == third person).
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
			this.setPosition(new Vec3(p.x, p.y - this.takeaseat$smoothOffset, p.z));
		}
	}
}
