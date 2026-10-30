package com.lumitier.shadowtweaks.config;

import com.lumitier.shadowtweaks.ShadowTweaks;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Baut den YACL-Options-Screen fuer Shadow Tweaks.
 *
 * Alle Texte laufen ueber Uebersetzungsschluessel (assets/shadowtweaks/lang/).
 * Vorhanden: en_us, de_de. Der Screen folgt der Spielsprache.
 *
 * AUFBAU FUER LUMILIB:
 * Die Helfer key/desc/toggle/slider/intSlider sind in allen Mods wortgleich
 * und wandern spaeter unveraendert in die Library. Was NICHT wandert, ist
 * create(): welche Tabs es gibt, welche Gruppen, welche Option an welcher
 * Stelle steht. Diese Entscheidungen bleiben pro Mod, damit ein Fehler in der
 * Library das Layout nicht in allen Mods gleichzeitig kaputt machen kann.
 */
public final class ShadowTweaksScreen {

	private ShadowTweaksScreen() {}

	// ------------------------------------------------------------------
	// Bausteine  >>> Kandidaten fuer LumiLib <<<
	// ------------------------------------------------------------------

	private static Text key(String id) {
		return Text.translatable("shadowtweaks.option." + id);
	}

	private static OptionDescription desc(String id) {
		return OptionDescription.createBuilder()
			.text(Text.translatable("shadowtweaks.option." + id + ".desc"))
			.build();
	}

	private static Option<Boolean> toggle(String id, boolean def,
	                                      java.util.function.Supplier<Boolean> getter,
	                                      java.util.function.Consumer<Boolean> setter) {
		return Option.<Boolean>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true))
			.build();
	}

	private static Option<Float> slider(String id,
	                                    float def, float min, float max, float step,
	                                    java.util.function.Supplier<Float> getter,
	                                    java.util.function.Consumer<Float> setter) {
		return Option.<Float>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step))
			.build();
	}

	private static Option<Integer> intSlider(String id,
	                                         int def, int min, int max, int step,
	                                         java.util.function.Supplier<Integer> getter,
	                                         java.util.function.Consumer<Integer> setter) {
		return Option.<Integer>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(step))
			.build();
	}

	// ------------------------------------------------------------------
	// Hinweis, wenn Vanillas eigener Schatten-Schalter aus ist.
	//
	// Vanilla prueft in updateShadow ZUERST options.getEntityShadows(). Steht
	// der auf Aus - und Sodium Extra spiegelt genau diesen Schalter - macht
	// die Mod bewusst gar nichts. Ohne Hinweis sieht das nach einem Bug aus.
	// ------------------------------------------------------------------

	private static boolean vanillaShadowsOff() {
		try {
			MinecraftClient mc = MinecraftClient.getInstance();
			return mc != null && mc.options != null
				&& !mc.options.getEntityShadows().getValue();
		} catch (Exception e) {
			// Lieber keinen Hinweis als einen Absturz im Optionsmenue.
			return false;
		}
	}

	// ------------------------------------------------------------------
	// Layout  >>> bleibt pro Mod <<<
	// ------------------------------------------------------------------

	public static Screen create(Screen parent) {
		return YetAnotherConfigLib.createBuilder()
			.title(Text.translatable("shadowtweaks.title"))

			// ================= Tab 1: General =================
			.category(ConfigCategory.createBuilder()
				.name(Text.translatable("shadowtweaks.category.general"))
				// Nur sichtbar, wenn Vanillas Schatten-Schalter aus ist.
				.optionIf(vanillaShadowsOff(), LabelOption.create(
					Text.translatable("shadowtweaks.warning.shadows_off")
						.formatted(Formatting.RED)))
				.option(toggle("mod_enabled", true,
					() -> ShadowTweaks.enabled, v -> ShadowTweaks.enabled = v))
				.build())

			// ================= Tab 2: Shadows =================
			.category(ConfigCategory.createBuilder()
				.name(Text.translatable("shadowtweaks.category.shadows"))

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("shadowtweaks.group.appearance"))
					.option(slider("scale", 1.0F, 0.0F, 3.0F, 0.05F,
						() -> ShadowTweaks.scale, v -> ShadowTweaks.scale = v))
					.option(slider("opacity", 1.0F, 0.0F, 1.0F, 0.05F,
						() -> ShadowTweaks.opacity, v -> ShadowTweaks.opacity = v))
					.option(intSlider("distance", 16, 4, 64, 1,
						() -> ShadowTweaks.distance, v -> ShadowTweaks.distance = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("shadowtweaks.group.categories"))
					.option(toggle("show_players", true,
						() -> ShadowTweaks.showPlayers, v -> ShadowTweaks.showPlayers = v))
					.option(toggle("show_hostile", true,
						() -> ShadowTweaks.showHostile, v -> ShadowTweaks.showHostile = v))
					.option(toggle("show_passive", true,
						() -> ShadowTweaks.showPassive, v -> ShadowTweaks.showPassive = v))
					.option(toggle("show_items", true,
						() -> ShadowTweaks.showItems, v -> ShadowTweaks.showItems = v))
					.option(toggle("show_other", true,
						() -> ShadowTweaks.showOther, v -> ShadowTweaks.showOther = v))
					.build())

				.build())

			.save(ShadowTweaks::save)
			.build()
			.generateScreen(parent);
	}
}
