package net.george.cmmplus.registration;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.george.cmmplus.CMMPlusConfig;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.world.item.Item;

/**
 * Create looks stress impacts up per block ({@code BlockStressValues.IMPACTS}); a block that is not
 * registered simply consumes nothing, so the tier machines would have been free to run.
 *
 * <p>Registering the value per tier mirrors Create More Machines, which does the same for every
 * tier machine it adds ({@code CMMBlockStressValues.setImpact}) - see {@link CMMPlusConfig} for the
 * numbers and why they scale with the tier.  The supplier is read live, so editing
 * {@code config/cmmplus-startup.toml} and reloading applies without a restart.
 *
 * <p>The item tooltips get Create's own {@link KineticStats} modifier, again exactly like CMM:
 * that is the line showing the stress impact with its bar and, while wearing goggles, the numeric
 * "x RPM" value.
 */
public class ModSetup {
    public static void register() {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            BlockStressValues.IMPACTS.register(ModBlocks.fan(tier).get(), () -> CMMPlusConfig.fanImpact(tier));
            BlockStressValues.IMPACTS.register(ModBlocks.wheel(tier).get(), () -> CMMPlusConfig.wheelImpact(tier));

            addKineticStatsTooltip(ModItems.fan(tier));
            addKineticStatsTooltip(ModItems.wheel(tier));
        }
    }

    private static void addKineticStatsTooltip(Item item) {
        KineticStats stats = KineticStats.create(item);
        if (stats != null) {
            TooltipModifier.REGISTRY.register(item, stats);
        }
    }
}
