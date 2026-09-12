package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A belt funnel's shape depends on the belt below it: with a belt there the funnel is retracted,
 * without one it takes its perpendicular shape.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  The tested
 * state is {@code world.getBlockState(pos.below())}, rebuilt from the same two arguments, so a
 * tier belt makes the funnel retract exactly like Create's own belt does instead of keeping the
 * wrong shape (and the wrong collision/placement rules that follow from it).
 */
@Mixin(value = BeltFunnelBlock.class, remap = false)
public class BeltFunnelBlockMixin {

    @ModifyExpressionValue(
            method = "getShapeForPosition(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/Direction;Z)Lcom/simibubi/create/content/logistics/funnel/BeltFunnelBlock$Shape;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$funnelShapeOnTierBelt(boolean original, BlockGetter world, BlockPos pos) {
        return original || TieredBeltBlock.isBelt(world.getBlockState(pos.below()));
    }
}
