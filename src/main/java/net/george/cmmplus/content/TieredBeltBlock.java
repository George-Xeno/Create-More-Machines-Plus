package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.registration.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A belt of one of the four tiers.
 *
 * <p>Everything about placement, shape, slope, casings and item movement is Create's own
 * {@link BeltBlock} behaviour; this class only carries the tier and wires the block to its own
 * block entity type.  The tier's throughput and stress live in {@link TieredBeltBlockEntity} -
 * the belt deliberately runs at <b>Create's speed</b>, the tier changes how much it carries and
 * what it costs, not how fast it moves.
 *
 * <p>Create's own machinery mostly tests {@code AllBlocks.BELT.has(state)}, which is false for a
 * subclass.  {@code RegistrateBlockEntryMixin} makes that lookup accept tiered belts as well, so
 * belt tunnels, funnels, deployers, contraptions and Create's own belt ticking all keep working.
 */
public class TieredBeltBlock extends BeltBlock {
    private final CMMPlusTier tier;

    public TieredBeltBlock(Properties properties, CMMPlusTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CMMPlusTier tier() {
        return tier;
    }

    /**
     * Names the belt "&lt;tier&gt; belt" with the tier in yellow, matching the naming Create More
     * Machines gives its tier machines.
     */
    @Override
    public String getDescriptionId() {
        return ChatFormatting.YELLOW + Component.translatable(tier.translationKey()).getString()
                + ChatFormatting.WHITE + Component.translatable("block.create.belt").getString();
    }

    @Override
    public Class<BeltBlockEntity> getBlockEntityClass() {
        return BeltBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BeltBlockEntity> getBlockEntityType() {
        return ModBlockEntities.belt(tier);
    }

    /**
     * Diagnostic: records what a tier belt block is replaced by.  A tier belt turning into air
     * means something destroyed it (Create's chain initialisation does that when it decides a block
     * is not a belt); a tier belt turning into another tier's belt means a chain was converted.
     */
    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            net.george.cmmplus.CMMPlus.LOGGER.info("[belt] {} at {} replaced by {}", this.tier().id, pos.toShortString(),
                    newState.isAir() ? "AIR (destroyed)" : newState.getBlock());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Create's own BeltBlock#getCollisionShape returns an empty shape unless the state's block is
     * {@code this} - i.e. unless it is Create's own belt block - so a tier belt is not a collision
     * surface at all and entities only ever interact with it through the landing path.  Handing back
     * the real shape makes a tier belt behave like a floor exactly as Create's belt does.
     */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** True for Create's belt and for every tiered belt. */
    public static boolean isBelt(BlockState state) {
        return state.getBlock() instanceof BeltBlock;
    }
}
