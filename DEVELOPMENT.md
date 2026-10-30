# Entwicklungsnotizen — Shadow Tweaks

Interne Notizen. Nicht fuer die Modrinth-Seite gedacht.

## Vanilla-Ablauf (1.21.11, Yarn 1.21.11+build.6)

```java
// EntityRenderer
public final S getAndUpdateRenderState(T entity, float tickDelta) {
    S state = createRenderState();
    updateRenderState(entity, state, tickDelta);
    updateShadow(entity, state);          // <- hier sitzen wir
    return state;
}

private void updateShadow(S state, MinecraftClient client, World world) {
    state.shadowPieces.clear();
    if (client.options.getEntityShadows().getValue() && !state.invisible) {
        float r = Math.min(this.getShadowRadius(state), 32.0F);
        state.shadowRadius = r;
        if (r > 0.0F) {
            float a = (float)((1.0 - state.squaredDistanceToCamera / 256.0)
                              * (double) this.getShadowOpacity(state));
            if (a > 0.0F) {
                // Bloecke im Radius durchgehen, je ein ShadowPiece bauen
            }
        }
    }
}
```

Alle drei Eingriffe der Mod sitzen in dieser einen Methode.

## Falle 1: getShadowRadius wird ueberschrieben

`getShadowRadius(S)` und `getShadowOpacity(S)` sind auf `EntityRenderer`
`protected` und trivial — sie geben ein Feld des Renderers zurueck. Mehrere
Vanilla-Renderer ueberschreiben sie aber:

```
SlimeEntityRenderer, StriderEntityRenderer, TurtleEntityRenderer,
VillagerEntityRenderer, ...
```

Eine Mixin direkt auf `EntityRenderer.getShadowRadius` haette bei genau diesen
Entities **nicht gegriffen**, weil die Subklasse die Basisimplementierung gar
nicht aufruft. Zusaetzlich erzeugt jedes Override zwei synthetische
Bridge-Methoden, der blosse Methodenname waere also dreifach mehrdeutig.

**Loesung:** `@ModifyExpressionValue` auf das `invokevirtual` *in*
`updateShadow`. Dort ist der Wert bereits polymorph aufgeloest. Deckt auch
Renderer aus fremden Mods ab.

Pruefbefehl:

```bash
javap -p -classpath <merged.jar> \
  net.minecraft.client.render.entity.SlimeEntityRenderer | grep getShadowRadius
```

## Falle 2: Farbe ist nicht machbar

`ShadowPiecesCommandRenderer.render` ruft `ColorHelper.getWhite(F)I` — sieht
nach einem perfekten Hook fuer eingefaerbte Schatten aus. Ist es nicht:

```
assets/minecraft/textures/misc/shadow.png
  64x64, colortype=4 (Grau+Alpha)
  Verschiedene RGB-Werte: 1 -> [(0, 0, 0)]
```

Die Textur ist **reines Schwarz** mit Alphaverlauf. Die Vertexfarbe wird mit
der Textur multipliziert, und alles mal Null bleibt Null. Der Hook greift
sauber und ist wirkungslos — dieselbe Klasse Fehler wie `renderStack` in Item
Drop Tweaks.

Machbar nur ueber Austausch der Textur. Das ist Resourcepack-Gebiet und bricht
bei jedem Nutzer mit eigenem Pack. Verworfen.

Pruefbefehl:

```python
from PIL import Image
im = Image.open('shadow.png').convert('RGBA')
print({px[:3] for px in im.getdata()})   # -> {(0, 0, 0)}
```

## Falle 3: state.shadowRadius in updateRenderState ist wirkungslos

Betrifft **Experience Orb Tweaks** und **Item Drop Tweaks**, nicht diese Mod.

Beide setzen `state.shadowRadius *= f` per `@Inject(at = @At("RETURN"))` auf
`updateRenderState`. Direkt danach laeuft `updateShadow` und ueberschreibt das
Feld bedingungslos (`state.shadowRadius = r`). Die Option "Schatten
mitskalieren" tut dort seit 1.0.0 nichts.

Fix fuer beide: derselbe Weg wie hier — in `updateShadow` an der
`getShadowRadius`-Aufrufstelle einhaken und ueber `state.entityType` auf
`EntityType.ITEM` bzw. `EntityType.EXPERIENCE_ORB` filtern. Eingeplant fuer
1.1.0.

## Kategorien

`state.entityType` liegt im RenderState — erste Anweisung in
`updateRenderState` ist `state.entityType = entity.getType()`. Die Einordnung
laeuft ueber `EntityType.getSpawnGroup()`:

| Kategorie | Kriterium |
|---|---|
| PLAYER  | `EntityType.PLAYER` |
| ITEM    | `EntityType.ITEM`, `EntityType.EXPERIENCE_ORB` |
| HOSTILE | `SpawnGroup.MONSTER` |
| PASSIVE | `CREATURE`, `AMBIENT`, `AXOLOTLS`, `WATER_CREATURE`, `WATER_AMBIENT`, `UNDERGROUND_WATER_CREATURE` |
| OTHER   | alles uebrige (Boote, Loren, Ruestungsstaender, Projektile, TNT) |

Modded Mobs landen ueber ihre SpawnGroup automatisch richtig.

Allokationsfrei: Die Methode gibt nur vorhandene Enum-Konstanten zurueck.

## Grenzen

- **Radius deckelt Vanilla bei 32** (`Math.min(r, 32.0F)`). Nicht umgangen,
  32 ist absurd gross.
- **Vanillas Entity-Shadows-Schalter bleibt oberstes Tor.** Ist er aus, laeuft
  `updateShadow` gar nicht erst in den Zweig. Der Config-Screen erkennt das
  und zeigt einen Hinweis — sonst kommen Bug-Reports.

## Kompatibilitaet

Kein einziger exklusiver Injektor. Nur `@ModifyExpressionValue`. Grund: Ein
`@Redirect`-Konflikt mit `npcstudio` hat frueher in einem 435-Mod-Pack zum
Blackscreen beim Start gefuehrt.
