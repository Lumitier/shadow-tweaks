package com.lumitier.shadowtweaks.client;

import com.lumitier.shadowtweaks.ShadowTweaks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-Entrypoint. Laedt die Config, mehr passiert hier nicht -
 * die eigentliche Arbeit macht die Mixin.
 */
public class ShadowTweaksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ShadowTweaks.load();
		ShadowTweaks.LOGGER.info("[Shadow Tweaks] Client-Init ok. (Phase 1: Geruest, Config, Groesse.)");

		// LumiLib ist optional - deshalb erst fragen, dann anfassen.
		// Der Fang um Throwable ist Absicht: LumiLib hat einen eigenen
		// Versionsrhythmus. Aendert sich dort etwas, soll DIESE Mod
		// weiterlaufen und nur nicht im Hub auftauchen.
		if (FabricLoader.getInstance().isModLoaded("lumilib")) {
			try {
				LumiLibCompat.register();
			} catch (Throwable t) {
				ShadowTweaks.LOGGER.warn("[Shadow Tweaks] LumiLib-Anmeldung fehlgeschlagen.", t);
			}
		}
	}
}
