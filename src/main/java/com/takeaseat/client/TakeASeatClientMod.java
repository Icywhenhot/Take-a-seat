package com.takeaseat.client;

import com.takeaseat.TakeASeat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TakeASeat.MOD_ID, dist = Dist.CLIENT)
public class TakeASeatClientMod {
	public TakeASeatClientMod(ModContainer container) {
		if (ModList.get().isLoaded("cloth_config")) {
			container.registerExtensionPoint(IConfigScreenFactory.class,
					(mod, parent) -> TakeASeatConfigScreen.create(parent));
		}
	}
}
