package net.george.cmmplus;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Per-tier stress impacts, modelled on Create More Machines' tier settings.
 *
 * <p>CMM gives every tier its own stress impact for every machine it adds - see its
 * {@code TierSettings.BrassTier} section, where the mechanical press costs 32.0 instead of Create's
 * 8.0 and the saw 8.0 instead of 4.0, scaling up to 2048.0 / 512.0 for the beyond tier - and
 * registers it per block through {@code CMMBlockStressValues.setImpact}.  A tier machine is
 * therefore not a free upgrade: the stress grows with the tier's throughput.
 *
 * <p>The same relation is used here, with one extra factor of two: Create's own values
 * ({@code AllBlocks}: encased fan 2.0, crushing wheel 8.0) are multiplied by the tier's processing
 * multiplier and then doubled, so a machine costs twice what its throughput would suggest - the
 * same relation CMM uses for its press (8.0 -> 32.0 at x4) taken one step further:
 *
 * <pre>
 *              fan        crushing wheel (per wheel)
 *   Create      2.0         8.0
 *   Brass      16.0        64.0   (x4,  twice)
 *   Netherite  32.0       128.0   (x8,  twice)
 *   End        64.0       256.0   (x16, twice)
 *   Beyond    128.0       512.0   (x32, twice)
 * </pre>
 *
 * These are startup config values, so a pack can retune them in
 * {@code config/cmmplus-startup.toml} without a code change, just like CMM's tier settings.
 */
public final class CMMPlusConfig {
    /** Create's own impacts: {@code AllBlocks} registers the fan with 2.0 and the wheel with 8.0. */
    private static final double CREATE_FAN_IMPACT = 2.0;
    private static final double CREATE_WHEEL_IMPACT = 8.0;
    /** Every tier is charged twice its processing multiplier's worth of stress. */
    private static final double TIER_COST_FACTOR = 2.0;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Map<CMMPlusTier, TierStress> TIERS = new EnumMap<>(CMMPlusTier.class);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.translation(key("tier_settings")).push("TierSettings");
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            TIERS.put(tier, new TierStress(tier));
        }
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private CMMPlusConfig() {
    }

    /** Stress impact registered for the tier's encased fan, in SU per rpm. */
    public static double fanImpact(CMMPlusTier tier) {
        return TIERS.get(tier).fanImpact;
    }

    /** Stress impact registered for the tier's crushing wheel, in SU per rpm. */
    public static double wheelImpact(CMMPlusTier tier) {
        return TIERS.get(tier).wheelImpact;
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            TIERS.values().forEach(TierStress::read);
            if (CMMPlus.LOGGER.isDebugEnabled()) {
                CMMPlus.LOGGER.debug("[config] tier impacts loaded: brass {}/{}, netherite {}/{}, end {}/{}, beyond {}/{}",
                        fanImpact(CMMPlusTier.BRASS), wheelImpact(CMMPlusTier.BRASS),
                        fanImpact(CMMPlusTier.NETHERITE), wheelImpact(CMMPlusTier.NETHERITE),
                        fanImpact(CMMPlusTier.END), wheelImpact(CMMPlusTier.END),
                        fanImpact(CMMPlusTier.BEYOND), wheelImpact(CMMPlusTier.BEYOND));
            }
        }
    }

    private static String key(String... parts) {
        StringBuilder builder = new StringBuilder("config.cmmplus");
        for (String part : parts) {
            builder.append('.').append(part);
        }
        return builder.toString();
    }

    /** One {@code [TierSettings.<Tier>Tier]} section, like CMM's {@code TierConfigBase}. */
    private static final class TierStress {
        private final ModConfigSpec.DoubleValue fanImpactValue;
        private final ModConfigSpec.DoubleValue wheelImpactValue;

        // Seeded with the defaults so a query before the config was read can never end up as 0 SU.
        private double fanImpact;
        private double wheelImpact;

        private TierStress(CMMPlusTier tier) {
            double fanDefault = CREATE_FAN_IMPACT * tier.processingMultiplier * TIER_COST_FACTOR;
            double wheelDefault = CREATE_WHEEL_IMPACT * tier.processingMultiplier * TIER_COST_FACTOR;
            this.fanImpact = fanDefault;
            this.wheelImpact = wheelDefault;

            BUILDER.translation(key(tier.id + "_tier")).push(sectionName(tier));
            this.fanImpactValue = BUILDER.translation(key("encased_fan_impact"))
                    .comment("Impact needed for the " + tier.id + " tier encased fan[default:"
                            + number(fanDefault) + "]")
                    .defineInRange(tier.id + "_encased_fan_impact", fanDefault, 1.0, Double.MAX_VALUE);
            this.wheelImpactValue = BUILDER.translation(key("crushing_wheel_impact"))
                    .comment("Impact needed for the " + tier.id + " tier crushing wheel, per wheel[default:"
                            + number(wheelDefault) + "]")
                    .defineInRange(tier.id + "_crushing_wheel_impact", wheelDefault, 1.0, Double.MAX_VALUE);
            BUILDER.pop();
        }

        private void read() {
            this.fanImpact = fanImpactValue.get();
            this.wheelImpact = wheelImpactValue.get();
        }

        private static String sectionName(CMMPlusTier tier) {
            String id = tier.id;
            return Character.toUpperCase(id.charAt(0)) + id.substring(1) + "Tier";
        }

        private static String number(double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
    }
}
