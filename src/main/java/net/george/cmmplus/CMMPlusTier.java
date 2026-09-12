package net.george.cmmplus;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * The four machine tiers.
 *
 * <p>{@link #processingMultiplier} is how many processing steps the machine performs per game
 * tick.  A vanilla Encased Fan or Crushing Wheel does exactly one, so a x4 tier machine feeds,
 * crushes or smelts four times as many items in the same time while still drawing the stress of
 * one machine at the network's actual rotation speed.
 */
public enum CMMPlusTier {
    BRASS("brass", "Brass", 4, 1, 128),
    NETHERITE("netherite", "Netherite", 8, 2, 256),
    END("end", "End", 16, 4, 512),
    BEYOND("beyond", "Beyond", 32, 8, 1024);

    public final String id;
    public final String displayName;
    public final int processingMultiplier;

    /**
     * Stress a tiered belt draws, in SU per rpm <b>per belt block</b>: the belt's stress is
     * {@code rpm x length x beltStressFactor}, so a 10 block Beyond belt at 64 rpm draws
     * 64 x 10 x 8 = 5120 SU.
     */
    public final int beltStressFactor;

    /** How many items one belt block of this tier can carry. */
    public final int beltCapacity;

    CMMPlusTier(String id, String displayName, int processingMultiplier, int beltStressFactor, int beltCapacity) {
        this.id = id;
        this.displayName = displayName;
        this.processingMultiplier = processingMultiplier;
        this.beltStressFactor = beltStressFactor;
        this.beltCapacity = beltCapacity;
    }

    /**
     * A Beyond belt is the only one that may hold several different item types inside a single
     * belt block, sharing its {@link #beltCapacity} "positions" between them.
     */
    public boolean mixedBeltLoad() {
        return this == BEYOND;
    }

    /**
     * How many of a belt block's positions one item occupies: an item that stacks to 16 takes
     * four positions, an item that stacks to 64 takes one, an unstackable item takes 64.
     */
    /**
     * Create draws a belt surface through a per-dye scroll sprite; until the tier's own sprite is
     * wired in, each tier uses the closest Create dye so the four belts are clearly different.
     */
    public DyeColor dyeColor() {
        return switch (this) {
            case BRASS -> DyeColor.ORANGE;
            case NETHERITE -> DyeColor.BLACK;
            case END -> DyeColor.MAGENTA;
            case BEYOND -> DyeColor.YELLOW;
        };
    }

    public static int beltPositionsFor(ItemStack stack) {
        return 64 / Math.max(1, stack.getMaxStackSize());
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
