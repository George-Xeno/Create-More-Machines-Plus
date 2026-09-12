package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/**
 * The air current of a tiered fan.
 *
 * <p>A Create fan's throughput does <b>not</b> depend on its rotation speed: in
 * {@code FanProcessing}, every tick the air current decrements the processing timer of each item
 * in front of it exactly once, and the fan's speed only decides how far the current reaches and
 * how hard it pushes.  Turning the reported speed up would therefore not process anything faster,
 * and it would corrupt the kinetic network (the rotation propagator reads a block's speed back
 * into its neighbours, see {@code RotationPropagator#propagateNewSource}).
 *
 * <p>Instead this current performs the processing half of a tick {@code multiplier} times; the
 * push and the flow limit stay at the vanilla rate, so a tier changes throughput only - which is
 * exactly what {@code tooltip.cmmplus.speed} promises.
 */
public class TieredAirCurrent extends AirCurrent {
    private final int multiplier;

    public TieredAirCurrent(IAirCurrentSource source, int multiplier) {
        super(source);
        this.multiplier = multiplier;
    }

    @Override
    public void tick() {
        // One ordinary tick: rebuild-free, pushes entities once and processes once.
        super.tick();

        int extraSteps = multiplier - 1;
        if (extraSteps <= 0 || direction == null || maxDistance < 0.25f) {
            return;
        }

        Level world = source.getAirCurrentWorld();
        for (int step = 0; step < extraSteps; step++) {
            // Items on belts, depots and other transported-item handlers.
            tickAffectedHandlers();
            // Items caught in the stream; on the client Create only draws particles from this,
            // so the extra steps are server-side work.
            if (world != null && !world.isClientSide) {
                processCaughtItems(world);
            }
        }
    }

    /**
     * The processing part of {@code AirCurrent#tickAffectedEntities}, minus the entity
     * acceleration - the same checks Create performs, just without pushing the items a second time.
     */
    private void processCaughtItems(Level world) {
        for (Entity entity : caughtEntities) {
            if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                continue;
            }
            if (!itemEntity.getBoundingBox().intersects(bounds)) {
                continue;
            }

            double distance = VecHelper.alignedDistanceToFace(itemEntity.position(),
                    source.getAirCurrentPos(), direction);
            FanProcessingType type = getTypeAt((float) distance);
            if (type != null && FanProcessing.canProcess(itemEntity, type)) {
                FanProcessing.applyProcessing(itemEntity, type);
            }
        }
    }
}
