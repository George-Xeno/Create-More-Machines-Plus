package net.george.cmmplus.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Consumer;

/**
 * Create's fan visual with the tier's own propeller texture.  Same structure as
 * {@code FanVisual}: the casing comes from the block model, the shaft and propeller spin here.
 */
public class TieredFanVisual extends KineticBlockEntityVisual<TieredFanBlockEntity> {
    protected final RotatingInstance shaft;
    protected final RotatingInstance fan;
    private final Direction direction = blockState.getValue(BlockStateProperties.FACING);
    private final Direction opposite = direction.getOpposite();

    public TieredFanVisual(VisualizationContext context, TieredFanBlockEntity blockEntity, float partialTick,
                           PartialModel propeller) {
        super(context, blockEntity, partialTick);
        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();
        fan = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(propeller))
                .createInstance();
        shaft.setup(blockEntity).setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, opposite).setChanged();
        fan.setup(blockEntity, getFanSpeed()).setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, opposite)
                .setChanged();
    }

    private float getFanSpeed() {
        float speed = blockEntity.getSpeed() * 5.0f;
        if (speed > 0) {
            speed = Mth.clamp(speed, 80, 1280);
        }
        if (speed < 0) {
            speed = Mth.clamp(speed, -1280, -80);
        }
        return speed;
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
        fan.setup(blockEntity, getFanSpeed()).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);
        BlockPos inFront = pos.relative(direction);
        relight(inFront, fan);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        fan.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(fan);
    }

    public static Factory<TieredFanBlockEntity> factory(PartialModel propeller) {
        // Resolved when the visual is created, i.e. after the model bake, so a missing tier model
        // falls back to Create's propeller instead of throwing.
        return (context, blockEntity, partialTick) -> new TieredFanVisual(context, blockEntity, partialTick,
                CMMPlusClient.usable(propeller, AllPartialModels.ENCASED_FAN_INNER));
    }
}
