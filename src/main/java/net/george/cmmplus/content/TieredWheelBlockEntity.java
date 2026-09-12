package net.george.cmmplus.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Iterate;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A crushing wheel that turns {@link CMMPlusTier#processingMultiplier} times faster than the
 * rotation it is given.
 *
 * <p>The multiplier is applied in {@link #getSpeed()} - the speed this machine <i>reports</i> and
 * works at - and deliberately <b>not</b> in {@code getTheoreticalSpeed()}, which is the value
 * Create's rotation propagator uses to decide what its neighbours run at.  Scaling the theoretical
 * speed instead makes the propagator treat the machine as a stronger source, push that speed back
 * into the shaft it is mounted on, and finally destroy the block
 * ({@code RotationPropagator#propagateNewSource} calls {@code world.destroyBlock} when a neighbour
 * would end up faster than the network allows).  Keeping the two apart means the network keeps its
 * real speed and stress, while the crushing controller, entity collisions and goggles all see the
 * tier's speed.
 */
public class TieredWheelBlockEntity extends CrushingWheelBlockEntity {
    private final CMMPlusTier tier;

    public TieredWheelBlockEntity(CMMPlusTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
    }

    public CMMPlusTier getTier() {
        return tier;
    }

    @Override
    public float getSpeed() {
        float speed = super.getSpeed();
        return speed == 0.0f ? 0.0f : speed * tier.processingMultiplier;
    }

    @Override
    public void fixControllers() {
        super.fixControllers();
        feedController();
    }

    /**
     * Create's {@code CrushingWheelControllerBlock#updateSpeed} reads the wheel's speed itself, but
     * only when the neighbouring wheel is {@code create:crushing_wheel}.  For tier wheels the
     * controller is therefore handed its crushing speed directly, which is what makes the pair
     * crush at the tier's rate.
     */
    private void feedController() {
        if (level == null || level.isClientSide) {
            return;
        }

        float crushingSpeed = Math.abs(getSpeed() / 50.0f);
        Axis wheelAxis = getBlockState().getValue(BlockStateProperties.AXIS);
        for (Direction direction : Iterate.directions) {
            // Same side test Create's own controller uses: a wheel is only seen from the side.
            if (direction.getAxis() == wheelAxis) {
                continue;
            }
            BlockPos controllerPos = worldPosition.relative(direction);
            BlockState state = level.getBlockState(controllerPos);
            if (!AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(state)) {
                continue;
            }
            if (!state.getValue(CrushingWheelControllerBlock.VALID)) {
                continue;
            }
            if (!(level.getBlockEntity(controllerPos) instanceof CrushingWheelControllerBlockEntity controller)) {
                continue;
            }

            if (controller.crushingspeed != crushingSpeed) {
                controller.crushingspeed = crushingSpeed;
                controller.sendData();
                // Create hands out its crushing wheel advancements from exactly this spot, so a
                // tier pair behaves like the machine it is built from.
                award(AllAdvancements.CRUSHING_WHEEL);
                if (Math.abs(getTheoreticalSpeed()) > AllConfigs.server().kinetics.maxRotationSpeed.get() - 1) {
                    award(AllAdvancements.CRUSHER_MAXED);
                }
            }
        }
    }
}
