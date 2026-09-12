package net.george.cmmplus.registration;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.george.cmmplus.CMMPlusConfig;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

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
    /**
     * Create registers its belt's item capability against its own block entity type only, so
     * tiered belts have to expose theirs here - otherwise funnels, hoppers and pipes see nothing
     * at a tiered belt block at all.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.belt(tier),
                    (be, context) -> be.segmentItemHandler());
        }
    }

    public static void register() {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            BlockStressValues.IMPACTS.register(ModBlocks.fan(tier).get(), () -> CMMPlusConfig.fanImpact(tier));
            BlockStressValues.IMPACTS.register(ModBlocks.wheel(tier).get(), () -> CMMPlusConfig.wheelImpact(tier));

            addKineticStatsTooltip(ModItems.fan(tier));
            addKineticStatsTooltip(ModItems.wheel(tier));
            // Belts draw a length-dependent stress, which Create's registry-driven item tooltip
            // cannot express (the in-world goggles read calculateStressApplied() directly and are
            // already correct), so the belt connector item explains the formula itself.
            TooltipModifier.REGISTRY.register(ModItems.beltConnector(tier).get(),
                    new net.george.cmmplus.client.TieredBeltTooltip(tier));
        }
    }

    private static void addKineticStatsTooltip(Item item) {
        KineticStats stats = KineticStats.create(item);
        if (stats != null) {
            TooltipModifier.REGISTRY.register(item, stats);
        }
    }
}
