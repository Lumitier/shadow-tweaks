package com.lumitier.shadowtweaks.client;

import com.lumitier.lumilib.api.LumiLibApi;
import com.lumitier.lumilib.api.LumiMod;
import com.lumitier.lumilib.api.SettingsPage;
import com.lumitier.shadowtweaks.ShadowTweaks;
import com.lumitier.shadowtweaks.config.ShadowTweaksScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;

/**
 * Anmeldung bei LumiLib.
 *
 * WARUM EINE EIGENE KLASSE - das ist der wichtige Teil:
 * LumiLib ist OPTIONAL. Stuende der Verweis darauf im Initialisierer der
 * Mod, wuerde die Java-Laufzeit beim Start versuchen, LumiLibs Klassen
 * aufzuloesen - und ohne LumiLib startet die Mod gar nicht mehr. Liegt
 * der Verweis dagegen in einer eigenen Klasse, wird sie erst geladen,
 * wenn jemand sie anfasst. Vorher wird abgefragt, ob LumiLib da ist.
 *
 * NAME, VERSION UND BESCHREIBUNG KOMMEN AUS DER fabric.mod.json:
 * Sie stehen dort ohnehin, Mod Menu und Modrinth lesen dieselben Werte.
 * Sie hier noch einmal einzutragen hiesse, zwei Stellen gleich halten zu
 * muessen - und nach dem naechsten Versionssprung stuende im Hub die
 * alte Nummer.
 *
 * ZUM ICON: nichts zu tun. LumiLib liest es aus dem Jar dieser Mod.
 */
public final class LumiLibCompat {

	private LumiLibCompat() {}

	public static void register() {
		ModContainer container = FabricLoader.getInstance()
			.getModContainer(ShadowTweaks.MOD_ID).orElse(null);

		String version = container == null ? ""
			: container.getMetadata().getVersion().getFriendlyString();
		String name = container == null ? "Shadow Tweaks"
			: container.getMetadata().getName();
		String summary = container == null ? ""
			: container.getMetadata().getDescription();

		LumiLibApi.register(new LumiMod(
			ShadowTweaks.MOD_ID,
			Text.literal(name),
			version,
			Text.literal(summary),
			// Kein eigenes Sprite - LumiLib nimmt das Icon aus dem Jar.
			null,
			// Der bestehende YACL-Bildschirm. Sobald LumiLib Schieberegler
			// und Farbwaehler kann, wird daraus eine SettingsPage und die
			// Einstellungen stehen direkt im Hub.
			// YACL bleibt als Rueckfallebene - Mod Menus Zahnrad
			// oeffnet weiterhin diesen Bildschirm.
			ShadowTweaksScreen::create,
			LumiLibCompat::settings));

		// Server-Regeln (LumiLibControlPlugin). Eigener Fang: Mit einer
		// LumiLib aelter als 1.3.0 gibt es die Methode nicht - dann soll die
		// Mod trotzdem im Hub stehen, nur eben ohne Server-Steuerung.
		try {
			LumiLibApi.onServerRulesChanged(() ->
				ShadowTweaks.serverDisabled = LumiLibApi.isDisabledByServer(ShadowTweaks.MOD_ID));
		} catch (Throwable t) {
			ShadowTweaks.LOGGER.warn("[Shadow Tweaks] LumiLib is too old for server control (needs 1.3.0+).");
		}
	}

	/**
	 * Die Einstellungen, wie LumiLibs Hub sie zeichnet.
	 *
	 * DIESELBEN FELDER wie der YACL-Bildschirm, nur anders beschrieben.
	 * Beide Wege schreiben direkt in die statischen Felder von
	 * ShadowTweaks und rufen dasselbe save() - es gibt also nur EINEN
	 * Stand, egal ueber welchen Weg man ihn aendert.
	 */
	private static SettingsPage settings() {
		SettingsPage.Builder b = SettingsPage.builder(ShadowTweaks.MOD_ID)
			.section("general");

		// Der Hinweis erscheint nur, wenn Minecrafts eigener
		// Schatten-Schalter aus ist - dann tut die Mod bewusst nichts,
		// und ohne Hinweis sieht das nach einem Fehler aus.
		if (vanillaShadowsOff()) {
			b.warning("shadows_off");
		}

		return b
				.toggle("mod_enabled",
					() -> ShadowTweaks.enabled, v -> ShadowTweaks.enabled = v, true)
			.section("appearance")
				.slider("scale", 0.0F, 3.0F, 0.05F,
					() -> ShadowTweaks.scale, v -> ShadowTweaks.scale = v, 1.0F)
				.slider("opacity", 0.0F, 1.0F, 0.05F,
					() -> ShadowTweaks.opacity, v -> ShadowTweaks.opacity = v, 1.0F)
				.intSlider("distance", 4, 64, 1,
					() -> ShadowTweaks.distance, v -> ShadowTweaks.distance = v, 16)
			.section("categories")
				.toggle("show_players",
					() -> ShadowTweaks.showPlayers, v -> ShadowTweaks.showPlayers = v, true)
				.toggle("show_hostile",
					() -> ShadowTweaks.showHostile, v -> ShadowTweaks.showHostile = v, true)
				.toggle("show_passive",
					() -> ShadowTweaks.showPassive, v -> ShadowTweaks.showPassive = v, true)
				.toggle("show_items",
					() -> ShadowTweaks.showItems, v -> ShadowTweaks.showItems = v, true)
				.toggle("show_other",
					() -> ShadowTweaks.showOther, v -> ShadowTweaks.showOther = v, true)
			.save(ShadowTweaks::save)
			.build();
	}

	/**
	 * Steht Minecrafts eigener Schatten-Schalter auf Aus?
	 *
	 * Vanilla prueft in updateShadow ZUERST options.getEntityShadows().
	 * Steht der auf Aus, macht die Mod bewusst gar nichts.
	 */
	private static boolean vanillaShadowsOff() {
		try {
			net.minecraft.client.MinecraftClient mc =
				net.minecraft.client.MinecraftClient.getInstance();
			return mc != null && mc.options != null
				&& !mc.options.getEntityShadows().getValue();
		} catch (Throwable t) {
			// Lieber kein Hinweis als ein Absturz im Hub.
			return false;
		}
	}
}
