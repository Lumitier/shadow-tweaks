package com.lumitier.shadowtweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lumitier.shadowtweaks.ShadowTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Eingriff in Vanillas Schattenberechnung.
 *
 * Verifiziert gegen das gemergte 1.21.11-Jar (Yarn 1.21.11+build.6) per javap.
 * Der Vanilla-Code in EntityRenderer:
 *
 *   private void updateShadow(S state, MinecraftClient client, World world) {
 *       state.shadowPieces.clear();
 *       if (client.options.getEntityShadows().getValue() && !state.invisible) {
 *           float r = Math.min(this.getShadowRadius(state), 32.0F);
 *           state.shadowRadius = r;
 *           if (r > 0.0F) {
 *               float a = (float)((1.0 - state.squaredDistanceToCamera / 256.0)
 *                                 * (double) this.getShadowOpacity(state));
 *               if (a > 0.0F) {
 *                   // Bloecke im Radius durchgehen, je ein ShadowPiece bauen
 *               }
 *           }
 *       }
 *   }
 *
 * Alle drei Eingriffe sitzen in dieser einen Methode. Das deckt JEDE Entity
 * ab, auch die aus fremden Mods.
 *
 * WARUM DIE AUFRUFSTELLEN UND NICHT DIE METHODEN:
 * getShadowRadius(S) und getShadowOpacity(S) werden von mehreren Vanilla-
 * Renderern ueberschrieben - nachgewiesen u.a. bei Slime, Strider, Turtle und
 * Villager. Eine Mixin direkt auf EntityRenderer.getShadowRadius wuerde bei
 * genau diesen Entities nicht greifen, weil die Subklasse die Basis-
 * implementierung gar nicht erst aufruft. Zusaetzlich bringt jedes Override
 * zwei synthetische Bridge-Methoden mit, der blosse Methodenname waere also
 * mehrdeutig.
 *
 * Am invokevirtual in updateShadow ist der Wert dagegen bereits polymorph
 * aufgeloest: Wir sehen, was die konkrete Subklasse geliefert hat.
 *
 * VANILLAS EIGENER SCHALTER BLEIBT OBERSTES TOR. Steht "Entity Shadows" in
 * den Grafikoptionen (oder in Sodium Extra) auf Aus, laeuft diese Methode gar
 * nicht erst in den Zweig - die Mod tut dann bewusst nichts. Der Config-
 * Screen weist darauf hin, wenn er diesen Zustand erkennt.
 *
 * KOMPATIBILITAET: @ModifyExpressionValue aus MixinExtras ist stapelbar.
 * Mehrere Mods duerfen denselben Wert nacheinander veraendern. Exklusive
 * Injektoren (@Redirect, @ModifyConstant, @ModifyArgs) kommen in diesem
 * Projekt nicht vor - sie haben in der Vergangenheit bei fremden Mods zum
 * Blackscreen beim Start gefuehrt.
 *
 * KEINE ALLOKATION: Alle drei Handler lesen nur statische Felder und
 * vergleichen Enum-Konstanten. Im Hot-Path entsteht kein Objekt.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

	/**
	 * Groesse des Schattens und Ausblenden nach Kategorie.
	 *
	 * Rueckgabe 0 heisst fuer Vanilla "kein Schatten": die Pruefung r > 0
	 * schlaegt fehl und die komplette Block-Schleife entfaellt. Ausblenden
	 * ist damit guenstiger als Vanilla, nicht teurer.
	 */
	@ModifyExpressionValue(
		method = "updateShadow(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderer;getShadowRadius(Lnet/minecraft/client/render/entity/state/EntityRenderState;)F"
		)
	)
	private float shadowtweaks$radius(float original, EntityRenderState state,
	                                  MinecraftClient client, World world) {
		if (!ShadowTweaks.enabled) {
			return original;
		}
		if (!ShadowTweaks.shows(state.entityType)) {
			return 0.0F;
		}
		float s = ShadowTweaks.clamp(ShadowTweaks.scale, 0.0F, 3.0F);
		if (s == 1.0F) {
			return original;
		}
		return original * s;
	}

	/** Deckkraft des Schattens. */
	@ModifyExpressionValue(
		method = "updateShadow(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderer;getShadowOpacity(Lnet/minecraft/client/render/entity/state/EntityRenderState;)F"
		)
	)
	private float shadowtweaks$opacity(float original, EntityRenderState state,
	                                   MinecraftClient client, World world) {
		if (!ShadowTweaks.enabled) {
			return original;
		}
		float o = ShadowTweaks.clamp(ShadowTweaks.opacity, 0.0F, 1.0F);
		if (o == 1.0F) {
			return original;
		}
		return original * o;
	}

	/**
	 * Sichtweite.
	 *
	 * Vanilla: alpha = (1 - squaredDistanceToCamera / 256.0) * opacity.
	 * 256 ist 16 zum Quadrat, also faellt der Schatten ab 16 Bloecken auf
	 * null. Wir setzen distance^2 ein; der Verlauf bleibt derselbe, nur
	 * gestreckt.
	 *
	 * Die Konstante 256.0 kommt in dieser Methode genau einmal vor (per
	 * javap geprueft), deshalb kein ordinal noetig.
	 *
	 * Kosten: Die Block-Schleife laeuft weiterhin nur ueber den Radius um die
	 * Entity, nicht ueber die Kameradistanz. Teurer wird es nur dadurch, dass
	 * mehr entfernte Entities ueberhaupt Schattenstuecke bekommen.
	 */
	@ModifyExpressionValue(
		method = "updateShadow(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V",
		at = @At(value = "CONSTANT", args = "doubleValue=256.0")
	)
	private double shadowtweaks$distance(double original, EntityRenderState state,
	                                     MinecraftClient client, World world) {
		if (!ShadowTweaks.enabled) {
			return original;
		}
		int d = ShadowTweaks.clampInt(ShadowTweaks.distance, 4, 64);
		if (d == 16) {
			return original;
		}
		return (double) d * (double) d;
	}
}
