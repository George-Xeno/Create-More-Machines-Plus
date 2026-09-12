package net.george.cmmplus;

/**
 * The four machine tiers.
 *
 * <p>{@link #processingMultiplier} is how many processing steps the machine performs per game
 * tick.  A vanilla Encased Fan or Crushing Wheel does exactly one, so a x4 tier machine feeds,
 * crushes or smelts four times as many items in the same time while still drawing the stress of
 * one machine at the network's actual rotation speed.
 */
public enum CMMPlusTier {
    BRASS("brass", "Brass", 4),
    NETHERITE("netherite", "Netherite", 8),
    END("end", "End", 16),
    BEYOND("beyond", "Beyond", 32);

    public final String id;
    public final String displayName;
    public final int processingMultiplier;

    CMMPlusTier(String id, String displayName, int processingMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.processingMultiplier = processingMultiplier;
    }

    /** The "x4" shown in tooltips and used as the processing multiplier. */
    public String speedText() {
        return "x" + processingMultiplier;
    }

    /**
     * Translation key of the tier's own name, mirroring Create More Machines' {@code tier.<mod>.<id>}
     * entries - their machines are named "&lt;tier&gt; &lt;create machine&gt;" (see
     * {@code CMMSawBlock#getDescriptionId}).
     */
    public String translationKey() {
        return "tier.cmmplus." + id;
    }
}
