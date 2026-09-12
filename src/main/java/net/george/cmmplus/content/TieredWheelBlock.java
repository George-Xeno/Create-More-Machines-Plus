package net.george.cmmplus.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.registration.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A crushing wheel with its own block entity type.
 *
 * <p>A pair of Create crushing wheels gets its crushing logic from the hidden
 * {@code create:crushing_wheel_controller} block that is spawned between them.  Create's
 * {@code CrushingWheelBlock#updateControllers} only ever looks for <b>Create's own</b> wheel
 * ({@code AllBlocks.CRUSHING_WHEEL.has(state)}), so a pair of tier wheels would never create a
 * controller - the wheels would spin but nothing would ever be crushed.  The method below is
 * Create's own logic with that single check generalised to {@link #isCrushingWheel}, which keeps
 * vanilla wheels, tier wheels and mixed pairs all working.
 */
public class TieredWheelBlock extends CrushingWheelBlock {
    private final CMMPlusTier tier;

    public TieredWheelBlock(Properties properties, CMMPlusTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CMMPlusTier tier() {
        return tier;
    }

    /**
     * Names the machine "&lt;tier&gt; &lt;Create machine&gt;" with the tier in yellow, matching the
     * naming Create More Machines gives its tier machines.
     */
    @Override
    public String getDescriptionId() {
        return ChatFormatting.YELLOW + Component.translatable(tier.translationKey()).getString()
                + ChatFormatting.WHITE + Component.translatable("block.create.crushing_wheel").getString();
    }

    /** True for Create's crushing wheel and for every tiered crushing wheel. */
    public static boolean isCrushingWheel(BlockState state) {
        return state.getBlock() instanceof CrushingWheelBlock;
    }

    @Override
    public Class<CrushingWheelBlockEntity> getBlockEntityClass() {
        return CrushingWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CrushingWheelBlockEntity> getBlockEntityType() {
        return ModBlockEntities.wheel(tier);
    }

    /**
     * Adapted from Create 6.0.10 {@code CrushingWheelBlock#updateControllers} (MIT); the only
     * change is the "is this a crushing wheel" test now accepting tiered wheels as well.
     */
    @Override
    public void updateControllers(BlockState state, Level world, BlockPos pos, Direction side) {
        if (side.getAxis() == state.getValue(AXIS) || world == null) {
            return;
        }

        BlockPos controllerPos = pos.relative(side);
        BlockPos otherWheelPos = pos.relative(side, 2);
        BlockState controllerState = world.getBlockState(controllerPos);
        boolean controllerExists = AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(controllerState);
        boolean controllerIsValid = controllerExists && controllerState.getValue(CrushingWheelControllerBlock.VALID);
        Direction controllerOldDirection = controllerExists
                ? controllerState.getValue(CrushingWheelControllerBlock.FACING)
                : null;

        boolean controllerShouldExist = false;
        boolean controllerShouldBeValid = false;
        Direction controllerNewDirection = Direction.DOWN;
        BlockState otherState = world.getBlockState(otherWheelPos);

        if (isCrushingWheel(otherState)) {
            controllerShouldExist = true;

            if (world.getBlockEntity(pos) instanceof CrushingWheelBlockEntity wheel
                    && world.getBlockEntity(otherWheelPos) instanceof CrushingWheelBlockEntity otherWheel
                    && wheel.getSpeed() > 0 != otherWheel.getSpeed() > 0
                    && wheel.getSpeed() != 0 && otherWheel.getSpeed() != 0) {
                Axis wheelAxis = state.getValue(AXIS);
                Axis sideAxis = side.getAxis();
                int controllerADO = Math.round(Math.signum(wheel.getSpeed())) * side.getAxisDirection().getStep();
                Vec3 controllerDirVec = new Vec3(
                        wheelAxis == Axis.X ? 1 : 0, wheelAxis == Axis.Y ? 1 : 0, wheelAxis == Axis.Z ? 1 : 0)
                        .cross(new Vec3(sideAxis == Axis.X ? 1 : 0, sideAxis == Axis.Y ? 1 : 0, sideAxis == Axis.Z ? 1 : 0));
                controllerNewDirection = Direction.getNearest(controllerDirVec.x * controllerADO,
                        controllerDirVec.y * controllerADO, controllerDirVec.z * controllerADO);
                controllerShouldBeValid = true;
            }

            if (otherState.getValue(AXIS) != state.getValue(AXIS)) {
                controllerShouldExist = false;
            }
        }

        if (!controllerShouldExist) {
            if (controllerExists) {
                world.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
            }
            return;
        }

        if (!controllerExists) {
            if (!controllerState.canBeReplaced()) {
                return;
            }
            world.setBlockAndUpdate(controllerPos, AllBlocks.CRUSHING_WHEEL_CONTROLLER.get().defaultBlockState()
                    .setValue(CrushingWheelControllerBlock.VALID, controllerShouldBeValid)
                    .setValue(CrushingWheelControllerBlock.FACING, controllerNewDirection));
        } else if (controllerIsValid != controllerShouldBeValid || controllerOldDirection != controllerNewDirection) {
            world.setBlockAndUpdate(controllerPos, controllerState
                    .setValue(CrushingWheelControllerBlock.VALID, controllerShouldBeValid)
                    .setValue(CrushingWheelControllerBlock.FACING, controllerNewDirection));
        }

        // Create's own controller speed lookup only recognises create:crushing_wheel; the tier
        // speed is handed to the controller by TieredWheelBlockEntity#fixControllers.
        ((CrushingWheelControllerBlock) AllBlocks.CRUSHING_WHEEL_CONTROLLER.get())
                .updateSpeed(world.getBlockState(controllerPos), world, controllerPos);
    }
}
