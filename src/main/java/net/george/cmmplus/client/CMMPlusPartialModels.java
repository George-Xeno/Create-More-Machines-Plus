package net.george.cmmplus.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Flywheel partial models for the tier machines.
 *
 * <p>Follows Create's own layout: every machine owns a folder under {@code models/block} with a
 * {@code block.json} (the geometry), a {@code textures.json} (the material table the OBJ loader
 * and the propeller model resolve their {@code #texture} references through) and - for fans - a
 * {@code propeller.json} for the part that spins.
 *
 * <p>Partial models have to exist before the model bake, hence {@link #init()} from client setup.
 */
public final class CMMPlusPartialModels {
    private static final Map<CMMPlusTier, PartialModel> WHEELS = new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, PartialModel> PROPELLERS = new EnumMap<>(CMMPlusTier.class);

    static {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            WHEELS.put(tier, PartialModel.of(CMMPlus.asResource("block/crushing_wheel_" + tier.id + "/block")));
            PROPELLERS.put(tier, PartialModel.of(CMMPlus.asResource("block/encased_fan_" + tier.id + "/propeller")));
        }
    }

    private CMMPlusPartialModels() {
    }

    public static PartialModel wheel(CMMPlusTier tier) {
        return WHEELS.get(tier);
    }

    public static PartialModel propeller(CMMPlusTier tier) {
        return PROPELLERS.get(tier);
    }

    /** Touches this class so its models are known before {@code RegisterAdditional} runs. */
    public static void init() {
    }
}
