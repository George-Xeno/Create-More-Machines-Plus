package net.george.cmmplus.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import net.george.cmmplus.content.TieredWheelBlockEntity;

import java.util.function.Consumer;

/**
 * Create's single-axis rotating visual, with one difference: the wheel is drawn at the speed the
 * network actually turns it ({@code getTheoreticalSpeed()}), not at the tier's working speed.
 *
 * <p>{@link TieredWheelBlockEntity#getSpeed()} reports the tier multiplier, because that is what the
 * crushing controller, entity collisions and the goggles need - but the wheel is not physically
 * spinning 32 times faster, and using that value for the visual made every tier wheel look like it
 * was running at maximum rpm no matter what the input was.
 */
public class TieredWheelVisual extends KineticBlockEntityVisual<TieredWheelBlockEntity>
        implements SimpleTickableVisual {
    protected final RotatingInstance rotatingModel;

    public TieredWheelVisual(VisualizationContext context, TieredWheelBlockEntity blockEntity, float partialTick,
                             Model model) {
        super(context, blockEntity, partialTick);
        rotatingModel = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, model)
                .createInstance()
                .rotateToFace(rotationAxis())
                .setup(blockEntity, blockEntity.getTheoreticalSpeed())
                .setPosition(getVisualPosition());
        rotatingModel.setChanged();
    }

    @Override
    public void update(float partialTick) {
        rotatingModel.setup(blockEntity, blockEntity.getTheoreticalSpeed()).setChanged();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        applyOverstressEffect(blockEntity, rotatingModel);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotatingModel);
    }

    @Override
    protected void _delete() {
        rotatingModel.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotatingModel);
    }

    public static Factory<TieredWheelBlockEntity> factory(PartialModel wheel) {
        // Resolved when the visual is created, i.e. after the model bake, so a missing tier model
        // falls back to Create's wheel instead of throwing.
        return (context, blockEntity, partialTick) -> new TieredWheelVisual(context, blockEntity, partialTick,
                Models.partial(CMMPlusClient.usable(wheel, AllPartialModels.CRUSHING_WHEEL)));
    }
}
