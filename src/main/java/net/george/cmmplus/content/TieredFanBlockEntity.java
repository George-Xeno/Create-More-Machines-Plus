package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fan that processes {@link CMMPlusTier#processingMultiplier} times per tick.  Everything else
 * (stress, rotation, airflow reach, goggles, chute interaction, rendering) is Create's own
 * behaviour, because this class is Create's fan block entity with a faster air current.
 */
public class TieredFanBlockEntity extends EncasedFanBlockEntity {
    private final CMMPlusTier tier;

    public TieredFanBlockEntity(CMMPlusTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
        // Replace the current the base class created in its field initialiser.
        this.airCurrent = new TieredAirCurrent(this, tier.processingMultiplier);
    }

    public CMMPlusTier getTier() {
        return tier;
    }
}
