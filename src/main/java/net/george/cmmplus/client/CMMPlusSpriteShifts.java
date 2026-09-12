package net.george.cmmplus.client;

import com.simibubi.create.Create;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;

import java.util.EnumMap;
import java.util.Map;

/**
 * The scrolling surface of a tier belt.
 *
 * <p>Built exactly the way Create builds its own belt shifts (adapted from Create 6.0.10,
 * {@code AllSpriteShifts} lines 86-88, MIT):
 *
 * <pre>
 *   BELT          = get("block/belt",          "block/belt_scroll");
 *   BELT_OFFSET   = get("block/belt_offset",   "block/belt_scroll");
 *   BELT_DIAGONAL = get("block/belt_diagonal", "block/belt_diagonal_scroll");
 * </pre>
 *
 * <p>Only the <b>targets</b> move to our own namespace ({@code cmmplus:block/belt/<tier>_scroll} and
 * {@code cmmplus:block/belt/<tier>_diagonal_scroll}, the two 16x32 belt sprites this addon ships).
 * The originals stay Create's sprites, which is what keeps the shift aligned with Create's belt
 * partial models - {@code create:block/belt} is the texture of the upper run
 * ({@code belt/start}, {@code belt/middle}, ...), {@code create:block/belt_offset} the one of the
 * lower run ({@code belt/start_bottom}, ...) and {@code create:block/belt_diagonal} the one of the
 * sloped run, so both runs of a straight belt share one target sprite exactly like Create's
 * {@code BELT} and {@code BELT_OFFSET} share {@code block/belt_scroll}.
 *
 * <p>{@link SpriteShifter#get} is the same registration Create's {@code AllSpriteShifts.get} does:
 * it hands back a cached {@link SpriteShiftEntry} whose two sprites are catnip {@code StitchedSprite}s.
 * Those add themselves to catnip's per-atlas list on construction, and catnip re-resolves every
 * entry in that list after each texture stitch ({@code StitchedSprite.onTextureStitchPost}, driven
 * from NeoForge's {@code TextureAtlasStitchedEvent}), so the entries are never stale after F3+T.
 * A sprite that is registered <i>after</i> a stitch has no atlas sprite until the next one, hence
 * {@link #init()} from client setup, before the first stitch.
 */
public final class CMMPlusSpriteShifts {
    /** Upper run of a straight belt - Create's {@code BELT}. */
    private static final Map<CMMPlusTier, SpriteShiftEntry> BELTS = new EnumMap<>(CMMPlusTier.class);
    /** Lower run of a straight belt - Create's {@code BELT_OFFSET}. */
    private static final Map<CMMPlusTier, SpriteShiftEntry> BELT_OFFSETS = new EnumMap<>(CMMPlusTier.class);
    /** Sloped (diagonal) run - Create's {@code BELT_DIAGONAL}. */
    private static final Map<CMMPlusTier, SpriteShiftEntry> BELT_DIAGONALS = new EnumMap<>(CMMPlusTier.class);

    static {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            BELTS.put(tier, get("block/belt", "block/belt/" + tier.id + "_scroll"));
            BELT_OFFSETS.put(tier, get("block/belt_offset", "block/belt/" + tier.id + "_scroll"));
            BELT_DIAGONALS.put(tier, get("block/belt_diagonal", "block/belt/" + tier.id + "_diagonal_scroll"));
        }
    }

    private CMMPlusSpriteShifts() {
    }

    /**
     * The tier's belt shift, mirroring Create's {@code BeltRenderer.getSpriteShiftEntry(color,
     * diagonal, bottom)} without the dyed variants - a tier belt is textured by its tier, not by a
     * dye (see {@code TieredBeltVisual}).
     */
    public static SpriteShiftEntry scroll(CMMPlusTier tier, boolean diagonal, boolean bottom) {
        if (diagonal) {
            // A sloped belt has a single run, so bottom is meaningless there - Create's helper
            // ignores it in the same way.
            return BELT_DIAGONALS.get(tier);
        }

        return (bottom ? BELT_OFFSETS : BELTS).get(tier);
    }

    /**
     * The tier's belt shift for the upper surface (the diagonal run has only this one).  Same
     * entries as the three argument {@link #scroll(CMMPlusTier, boolean, boolean)}, which is what
     * the visual uses.
     */
    public static SpriteShiftEntry scroll(CMMPlusTier tier, boolean diagonal) {
        return scroll(tier, diagonal, false);
    }

    /** Create's {@code AllSpriteShifts.get}: original from Create, target from this addon. */
    private static SpriteShiftEntry get(String originalLocation, String targetLocation) {
        return SpriteShifter.get(Create.asResource(originalLocation), CMMPlus.asResource(targetLocation));
    }

    /** Touches this class so its shifts are registered before the first texture stitch. */
    public static void init() {
    }
}
