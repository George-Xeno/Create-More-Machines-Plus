package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The schematicannon launching a belt: the belt has to be sent as a whole belt (its segments,
 * casings and pulleys), not as a plain block per position.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  Both tests
 * read {@code blockState}, which is the method's own third parameter - and in the second test the
 * parameter has just been reassigned by {@code stripBeltIfNotLast}, so capturing the argument reads
 * exactly the state that Create tested in each case.
 */
@Mixin(value = SchematicannonBlockEntity.class, remap = false)
public class SchematicannonBlockEntityMixin {

    /**
     * {@code shouldIgnoreBlockState} skips the parts of a belt that the belt launcher covers: every
     * segment except the last one.  A tier belt that fails here gets every middle segment queued as
     * a separate block launch.
     */
    @ModifyExpressionValue(
            method = "shouldIgnoreBlockState(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/entity/BlockEntity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$ignoreTierBeltSegments(boolean original, BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }

    /**
     * The entry test of {@code launchBlockOrBelt}: a belt takes the specialised launch path.
     */
    @ModifyExpressionValue(
            method = "launchBlockOrBelt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$launchTierBelt(boolean original, BlockPos target, ItemStack icon,
                                          BlockState blockState) {
        return original || TieredBeltBlock.isBelt(blockState);
    }

    /**
     * The second test, after {@code stripBeltIfNotLast} turned a middle segment into air: only a
     * belt that survived that still has a block entity worth reading casings from.  The captured
     * argument is the stripped state, which is what Create tested here.
     */
    @ModifyExpressionValue(
            method = "launchBlockOrBelt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$launchStrippedTierBelt(boolean original, BlockPos target, ItemStack icon,
                                                  BlockState blockState) {
        return original || TieredBeltBlock.isBelt(blockState);
    }
}
