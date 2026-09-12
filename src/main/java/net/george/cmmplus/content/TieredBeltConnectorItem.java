package net.george.cmmplus.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.math.VecHelper;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedList;
import java.util.List;

/**
 * The item used to build a tiered belt.
 *
 * <p>Create's {@code BeltConnectorItem} builds a belt from a <b>static</b> {@code createBelts} that
 * hardcodes {@code AllBlocks.BELT.getDefaultState()}, so no subclass can decide which block gets
 * placed.  This item therefore carries Create's own placement path - {@code useOn} plus a copy of
 * the chain computation and the placement loop - and the only difference is the block state it
 * places: {@link #beltBlock()}, the tier's own {@link TieredBeltBlock}.
 *
 * <p>Placing the tier block from the start is what makes the belt work: the chain is registered by
 * {@code BeltBlock#initBelt} and by {@code BeltBlockEntity}'s own ticking, both of which look the
 * belt up through {@code AllBlocks.BELT.has(...)} - an addon belt answers that through
 * {@code BlockEntryMixin}.  Replacing the vanilla belt afterwards would instead re-run
 * {@code setBlockAndUpdate}, which re-shapes and re-initialises the chain that was just built.
 */
public class TieredBeltConnectorItem extends BeltConnectorItem {
    private final CMMPlusTier tier;

    public TieredBeltConnectorItem(Properties properties, CMMPlusTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CMMPlusTier tier() {
        return tier;
    }

    public Block beltBlock() {
        return ModBlocks.belt(tier).get();
    }

    /**
     * Adapted from Create 6.0.10 {@code BeltConnectorItem#useOn} (MIT); the only change is that the
     * belt is built by {@link #createTierBelts} below, which places this tier's belt block.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player playerEntity = context.getPlayer();
        ItemStack heldStack = context.getItemInHand();
        if (playerEntity != null && playerEntity.isShiftKeyDown()) {
            heldStack.remove(AllDataComponents.BELT_FIRST_SHAFT);
            return InteractionResult.SUCCESS;
        }

        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean validAxis = validateAxis(world, pos);
        if (world.isClientSide) {
            return validAxis ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        BlockPos firstPulley = null;
        if (heldStack.has(AllDataComponents.BELT_FIRST_SHAFT)) {
            firstPulley = heldStack.get(AllDataComponents.BELT_FIRST_SHAFT);
            if (!validateAxis(world, firstPulley) || !firstPulley.closerThan(pos, maxLength() * 2)) {
                heldStack.remove(AllDataComponents.BELT_FIRST_SHAFT);
            }
        }

        if (!validAxis || playerEntity == null) {
            return InteractionResult.FAIL;
        } else if (!heldStack.has(AllDataComponents.BELT_FIRST_SHAFT)) {
            heldStack.set(AllDataComponents.BELT_FIRST_SHAFT, pos);
            playerEntity.getCooldowns().addCooldown(this, 5);
            return InteractionResult.SUCCESS;
        } else if (!canConnect(world, firstPulley, pos)) {
            return InteractionResult.FAIL;
        } else {
            if (firstPulley != null && !firstPulley.equals(pos)) {
                createTierBelts(world, firstPulley, pos);
                AllAdvancements.BELT.awardTo(playerEntity);
                if (!playerEntity.isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }

            if (!context.getItemInHand().isEmpty()) {
                heldStack.remove(AllDataComponents.BELT_FIRST_SHAFT);
                playerEntity.getCooldowns().addCooldown(this, 5);
            }

            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Adapted from Create 6.0.10 {@code BeltConnectorItem#createBelts} (MIT); the only change is
     * the placed block state, which comes from {@link #beltBlock()} instead of
     * {@code AllBlocks.BELT.getDefaultState()}.
     *
     * <p>Create's method is static and takes no item, which is exactly why it cannot be reused
     * here, and why this copy carries a different name: an instance method may not have the same
     * signature as an inherited static one.
     */
    public void createTierBelts(Level world, BlockPos start, BlockPos end) {
        world.playSound(null, BlockPos.containing(VecHelper.getCenterOf(start.offset(end)).scale(0.5)),
                SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F);
        BeltSlope slope = getSlopeBetween(start, end);
        Direction facing = getFacingFromTo(start, end);
        BlockPos diff = end.subtract(start);
        if (diff.getX() == diff.getZ()) {
            facing = Direction.get(facing.getAxisDirection(),
                    world.getBlockState(start).getValue(BlockStateProperties.AXIS) == Axis.X ? Axis.Z : Axis.X);
        }

        List<BlockPos> beltsToCreate = getBeltChainBetween(start, end, slope, facing);
        BlockState beltBlock = beltBlock().defaultBlockState();
        boolean failed = false;

        for (BlockPos pos : beltsToCreate) {
            BlockState existingBlock = world.getBlockState(pos);
            if (existingBlock.getDestroySpeed(world, pos) == -1.0F) {
                failed = true;
                break;
            }

            BeltPart part = pos.equals(start) ? BeltPart.START
                    : (pos.equals(end) ? BeltPart.END : BeltPart.MIDDLE);
            BlockState shaftState = world.getBlockState(pos);
            boolean pulley = ShaftBlock.isShaft(shaftState);
            if (part == BeltPart.MIDDLE && pulley) {
                part = BeltPart.PULLEY;
            }

            if (pulley && shaftState.getValue(AbstractSimpleShaftBlock.AXIS) == Axis.Y) {
                slope = BeltSlope.SIDEWAYS;
            }

            if (!existingBlock.canBeReplaced()) {
                world.destroyBlock(pos, false);
            }

            KineticBlockEntity.switchToBlockState(
                    world,
                    pos,
                    ProperWaterloggedBlock.withWater(
                            world,
                            beltBlock.setValue(BeltBlock.SLOPE, slope)
                                    .setValue(BeltBlock.PART, part)
                                    .setValue(BeltBlock.HORIZONTAL_FACING, facing),
                            pos));
        }

        // Create's own cleanup test, kept as it is: it only ever sees the belts this method placed,
        // and a tier belt answers AllBlocks.BELT.has(...) through BlockEntryMixin.
        if (failed) {
            for (BlockPos pos : beltsToCreate) {
                if (TieredBeltBlock.isBelt(world.getBlockState(pos))) {
                    world.destroyBlock(pos, false);
                }
            }
        }
    }

    /** Adapted from Create 6.0.10 {@code BeltConnectorItem#getFacingFromTo} (MIT); unchanged. */
    private static Direction getFacingFromTo(BlockPos start, BlockPos end) {
        Axis beltAxis = start.getX() == end.getX() ? Axis.Z : Axis.X;
        BlockPos diff = end.subtract(start);
        AxisDirection axisDirection = AxisDirection.POSITIVE;
        if (diff.getX() == 0 && diff.getZ() == 0) {
            axisDirection = diff.getY() > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
        } else {
            axisDirection = beltAxis.choose(diff.getX(), 0, diff.getZ()) > 0
                    ? AxisDirection.POSITIVE
                    : AxisDirection.NEGATIVE;
        }

        return Direction.get(axisDirection, beltAxis);
    }

    /** Adapted from Create 6.0.10 {@code BeltConnectorItem#getSlopeBetween} (MIT); unchanged. */
    private static BeltSlope getSlopeBetween(BlockPos start, BlockPos end) {
        BlockPos diff = end.subtract(start);
        if (diff.getY() != 0) {
            if (diff.getZ() == 0 && diff.getX() == 0) {
                return BeltSlope.VERTICAL;
            }

            return diff.getY() > 0 ? BeltSlope.UPWARD : BeltSlope.DOWNWARD;
        }

        return BeltSlope.HORIZONTAL;
    }

    /** Adapted from Create 6.0.10 {@code BeltConnectorItem#getBeltChainBetween} (MIT); unchanged. */
    private static List<BlockPos> getBeltChainBetween(BlockPos start, BlockPos end, BeltSlope slope, Direction direction) {
        List<BlockPos> positions = new LinkedList<>();
        int limit = 1000;
        BlockPos current = start;

        do {
            positions.add(current);
            if (slope == BeltSlope.VERTICAL) {
                current = current.above(direction.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1);
            } else {
                current = current.relative(direction);
                if (slope != BeltSlope.HORIZONTAL) {
                    current = current.above(slope == BeltSlope.UPWARD ? 1 : -1);
                }
            }
        } while (!current.equals(end) && limit-- > 0);

        positions.add(end);
        return positions;
    }
}
