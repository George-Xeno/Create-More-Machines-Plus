package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Belt tunnels standing on a tier belt.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  All three
 * tests here ask "is the block under (or beside) the tunnel a belt of mine?", and all three read
 * the state from a position that is derived from the method's own arguments
 * ({@code worldIn.getBlockState(pos.below())} and {@code world.getBlockState(pos.relative(side))}),
 * so the handlers re-read the identical expression and only ever widen its answer.
 *
 * <p>Without this a tunnel cannot be placed on a tier belt, and one already placed never updates
 * its shape or accepts items from the belt.
 */
@Mixin(value = BeltTunnelBlock.class, remap = false)
public class BeltTunnelBlockMixin {

    /** Placement: the block below has to be a belt. */
    @ModifyExpressionValue(
            method = "isValidPositionForPlacement(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$placeTunnelOnTierBelt(boolean original, BlockState state, LevelReader worldIn, BlockPos pos) {
        return original || TieredBeltBlock.isBelt(worldIn.getBlockState(pos.below()));
    }

    /**
     * The tunnel's shape follows the axis of the belt underneath; a tier belt that fails here makes
     * the tunnel fall back to its default axis.
     */
    @ModifyExpressionValue(
            method = "getTunnelState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)"
                    + "Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$tunnelShapeFromTierBelt(boolean original, BlockGetter reader, BlockPos pos) {
        return original || TieredBeltBlock.isBelt(reader.getBlockState(pos.below()));
    }

    /**
     * Whether the tunnel may push sideways into the block beside it; a belt there has to be
     * recognised, otherwise the tunnel is drawn and behaves as if that side were a dead end.
     */
    @ModifyExpressionValue(
            method = "hasValidOutput(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/Direction;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$tunnelOutputToTierBelt(boolean original, BlockGetter world, BlockPos pos, Direction side) {
        return original || TieredBeltBlock.isBelt(world.getBlockState(pos.relative(side)));
    }
}
