package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Placing a schematic's blocks: belts are set with flag 2 instead of 18 so their block entity is
 * not re-created from scratch as the chain is assembled.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  A tier belt
 * that fails this test is placed with the normal flags, so a schematic (or a contraption's saved
 * state) restores the belt as a bare block instead of a working belt.  The tested state is the
 * method's own second parameter.
 */
@Mixin(value = BlockHelper.class, remap = false)
public class BlockHelperMixin {

    @ModifyExpressionValue(
            method = "placeSchematicBlock(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$placeTierBelt(boolean original, Level world, BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }
}
