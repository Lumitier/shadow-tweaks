package com.lumitier.shadowtweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod-weiter Zustand + Config.
 *
 * Statische Felder, damit die Renderer-Mixin im Hot-Path ohne Lookup und ohne
 * Allokation darauf zugreifen kann. Die YACL-Oberflaeche schreibt direkt
 * hinein (Live-Wirkung), save()/load() persistiert als JSON.
 *
 * HINWEIS FUER LUMILIB: Die Abschnitte "Clamp-Helfer" und "Persistenz" sind
 * in allen Mods wortgleich und wandern spaeter unveraendert in die Library.
 */
public final class ShadowTweaks {

	public static final String MOD_ID = "shadowtweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private ShadowTweaks() {}

	// ------------------------------------------------------------------
	// Config-Felder
	//
	// Alle Defaults entsprechen exakt Vanilla - frisch installiert aendert
	// sich nichts am Bild.
	// ------------------------------------------------------------------

	/** Mod global an/aus. */
	public static volatile boolean enabled = true;

	/**
	 * Vom Server abgeschaltet (LumiLibControlPlugin ueber LumiLib).
	 *
	 * Bewusst ein EIGENES Feld und nicht "enabled = false": Das hier gilt
	 * nur fuer die aktuelle Verbindung und wird nie gespeichert. Sonst
	 * waere die Mod nach dem Besuch eines einzigen Servers dauerhaft aus.
	 */
	public static volatile boolean serverDisabled = false;

	/** Wirkt die Mod gerade? Eigener Schalter UND der Server muessen es erlauben. */
	public static boolean active() {
		return enabled && !serverDisabled;
	}

	/**
	 * Groessenfaktor fuer alle Entity-Schatten. 1.0 = Vanilla.
	 *
	 * 0.0 blendet Schatten aus: Vanilla prueft nach dem Skalieren auf
	 * {@code radius > 0} und ueberspringt dann die gesamte Schattenlogik
	 * inklusive der Block-Schleife. Aus ist also billiger als Vanilla.
	 *
	 * Nach oben deckelt Vanilla bei 32 Bloecken (Math.min im Renderer).
	 */
	public static volatile float scale = 1.0F;

	/** Deckkraft-Faktor. 1.0 = Vanilla, 0.0 = unsichtbar. */
	public static volatile float opacity = 1.0F;

	/**
	 * Sichtweite in Bloecken. Vanilla: 16.
	 *
	 * Vanilla rechnet {@code alpha = (1 - sqDistToCamera / 256.0) * opacity}.
	 * Die 256 ist 16 zum Quadrat - ab 16 Bloecken ist alpha <= 0 und der
	 * Schatten verschwindet komplett. Wir ersetzen die Konstante durch
	 * {@code distance * distance}.
	 */
	public static volatile int distance = 16;

	// ------------------------------------------------------------------
	// Kategorien
	//
	// state.entityType liegt im RenderState (erste Anweisung in
	// updateRenderState setzt es), also koennen wir im Hook nach Art des
	// Lebewesens unterscheiden - das kann Sodium Extras globaler Schalter
	// nicht.
	// ------------------------------------------------------------------

	public static volatile boolean showPlayers = true;
	public static volatile boolean showHostile = true;
	public static volatile boolean showPassive = true;
	public static volatile boolean showItems   = true;
	public static volatile boolean showOther   = true;

	public enum Category { PLAYER, HOSTILE, PASSIVE, ITEM, OTHER }

	/**
	 * Einordnung eines EntityType. Allokationsfrei - liefert nur vorhandene
	 * Enum-Konstanten zurueck.
	 *
	 * Modded Entities fallen ueber ihre SpawnGroup automatisch in die
	 * passende Kategorie; was keiner Mob-Gruppe angehoert, landet in OTHER
	 * (Boote, Loren, Ruestungsstaender, Projektile, TNT ...).
	 */
	public static Category categoryOf(EntityType<?> type) {
		if (type == EntityType.PLAYER) {
			return Category.PLAYER;
		}
		if (type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB) {
			return Category.ITEM;
		}
		SpawnGroup g = type.getSpawnGroup();
		if (g == SpawnGroup.MONSTER) {
			return Category.HOSTILE;
		}
		if (g == SpawnGroup.CREATURE
			|| g == SpawnGroup.AMBIENT
			|| g == SpawnGroup.AXOLOTLS
			|| g == SpawnGroup.WATER_CREATURE
			|| g == SpawnGroup.WATER_AMBIENT
			|| g == SpawnGroup.UNDERGROUND_WATER_CREATURE) {
			return Category.PASSIVE;
		}
		return Category.OTHER;
	}

	/** Soll dieser EntityType ueberhaupt einen Schatten bekommen? */
	public static boolean shows(EntityType<?> type) {
		if (type == null) {
			return true;
		}
		return switch (categoryOf(type)) {
			case PLAYER  -> showPlayers;
			case HOSTILE -> showHostile;
			case PASSIVE -> showPassive;
			case ITEM    -> showItems;
			case OTHER   -> showOther;
		};
	}

	// ------------------------------------------------------------------
	// Clamp-Helfer  >>> Kandidat fuer LumiLib <<<
	// ------------------------------------------------------------------

	public static float clamp(float v, float min, float max) {
		if (Float.isNaN(v)) {
			return min;
		}
		return v < min ? min : (v > max ? max : v);
	}

	public static int clampInt(int v, int min, int max) {
		return v < min ? min : (v > max ? max : v);
	}

	/** Alle Werte in gueltige Bereiche zwingen. */
	public static void sanitize() {
		scale    = clamp(scale,   0.0F, 3.0F);
		opacity  = clamp(opacity, 0.0F, 1.0F);
		distance = clampInt(distance, 4, 64);
	}

	// ------------------------------------------------------------------
	// Persistenz (GSON kommt mit Minecraft)  >>> Kandidat fuer LumiLib <<<
	// ------------------------------------------------------------------

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	/** Reines Daten-Transferobjekt fuer JSON. */
	private static final class Data {
		boolean enabled = true;
		float scale = 1.0F;
		float opacity = 1.0F;
		int distance = 16;
		boolean showPlayers = true;
		boolean showHostile = true;
		boolean showPassive = true;
		boolean showItems = true;
		boolean showOther = true;
	}

	public static void load() {
		Path p = configPath();
		try {
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p)) {
					Data d = GSON.fromJson(r, Data.class);
					if (d != null) {
						enabled     = d.enabled;
						scale       = d.scale;
						opacity     = d.opacity;
						distance    = d.distance;
						showPlayers = d.showPlayers;
						showHostile = d.showHostile;
						showPassive = d.showPassive;
						showItems   = d.showItems;
						showOther   = d.showOther;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("[Shadow Tweaks] Config konnte nicht geladen werden, nutze Defaults.", e);
		}
		sanitize();
		save();
	}

	public static void save() {
		sanitize();
		Path p = configPath();
		try {
			Files.createDirectories(p.getParent());
			Data d = new Data();
			d.enabled     = enabled;
			d.scale       = scale;
			d.opacity     = opacity;
			d.distance    = distance;
			d.showPlayers = showPlayers;
			d.showHostile = showHostile;
			d.showPassive = showPassive;
			d.showItems   = showItems;
			d.showOther   = showOther;
			try (Writer w = Files.newBufferedWriter(p)) {
				GSON.toJson(d, w);
			}
		} catch (Exception e) {
			LOGGER.warn("[Shadow Tweaks] Config konnte nicht gespeichert werden.", e);
		}
	}
}
