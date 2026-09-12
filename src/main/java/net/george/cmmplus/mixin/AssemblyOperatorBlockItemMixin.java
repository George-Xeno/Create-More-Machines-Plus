package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Placing a depot, basin, press or mixer onto a belt.
 *
 * <p>{@code operatesOn} is what lets an assembly machine be placed on a belt (a horizontal one
 * only), so without this widening a machine cannot be put on a tier belt at all.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  The tested
 * state is the method's own third parameter.  The mixin only widens the belt branch: the depot and
 * weighted-ejector branches of the same expression are untouched.
 *
 * <p>Another mod in the pack (Create: More Machines) injects into this same method at
 * {@code @At("RETURN")}; a value modifier on an inner call does not conflict with that.
 */
@Mixin(value = AssemblyOperatorBlockItem.class, remap = false)
public class AssemblyOperatorBlockItemMixin {

    @ModifyExpressionValue(
            method = "operatesOn(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$operateOnTierBelt(boolean original, LevelReader world, BlockPos pos,
                                             BlockState placedOnState) {
        return original || TieredBeltBlock.isBelt(placedOnState);
    }
}
