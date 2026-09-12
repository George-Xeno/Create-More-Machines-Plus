package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Whether a belt may be picked up by a contraption.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  The state
 * tested here is the method's own first argument, so it is passed straight to the handler: a tier
 * belt now counts as movable by a bearing, piston or gantry like Create's own belt, instead of
 * falling through to the piston reaction test at the end of the chain.
 */
@Mixin(value = BlockMovementChecksImpl.class, remap = false)
public class BlockMovementChecksImplMixin {

    @ModifyExpressionValue(
            method = "isMovementAllowedFallback(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$tierBeltIsMovable(boolean original, BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }
}
