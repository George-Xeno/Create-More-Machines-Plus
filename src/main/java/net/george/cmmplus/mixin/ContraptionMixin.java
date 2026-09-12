package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.Contraption;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Assembling a contraption: a belt found in the frontier is handed to {@code moveBelt}.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  Without it
 * a tier belt inside the assembly is left behind as a normal block while the contraption moves off,
 * which leaves the belt (and its block entities) orphaned in the world.
 *
 * <p>The state tested is the local {@code state}, read from the position popped off the frontier
 * queue - the queue is a parameter, but re-reading it would consume it, so the local is captured
 * with {@code @Local} instead of being rebuilt.
 */
@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {

    @ModifyExpressionValue(
            method = "moveBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Direction;"
                    + "Ljava/util/Queue;Ljava/util/Set;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$moveTierBelt(boolean original, @Local(name = "state") BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }
}
