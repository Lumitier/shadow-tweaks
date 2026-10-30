package com.lumitier.shadowtweaks.client;

import com.lumitier.shadowtweaks.ShadowTweaks;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-Entrypoint. Laedt die Config, mehr passiert hier nicht -
 * die eigentliche Arbeit macht die Mixin.
 */
public class ShadowTweaksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ShadowTweaks.load();
		ShadowTweaks.LOGGER.info("[Shadow Tweaks] Client-Init ok. (Phase 1: Geruest, Config, Groesse.)");
	}
}
