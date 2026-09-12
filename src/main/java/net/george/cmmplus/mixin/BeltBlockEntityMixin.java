package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The two {@code AllBlocks.BELT.has(...)} checks inside Create's belt block entity.
 *
 * <p>{@link BeltBlockEntity#tick} guards its item handling behind that test - without it a tier
 * belt never builds its {@code ItemHandlerBeltSegment}, so nothing can be inserted, no item is
 * ever ticked along and the belt looks alive while doing nothing.
 *
 * <p>{@link BeltBlockEntity#hasPulley} is the shaft-connection test: {@code BeltBlock} asks it from
 * {@code hasShaftTowards}, and the kinetic network asks the block for its shaft faces.  A tier belt
 * whose pulley is denied has no kinetic network and applies no stress at all.
 *
 * <p>Both target methods are instance methods with no parameters, so there is nothing to rebuild
 * the tested state from; the handler instead reaches the target object the ordinary mixin way
 * (the handler is merged into {@link BeltBlockEntity}, so {@code this} <i>is</i> the belt) and
 * re-reads the very same block state the guard read.  Both re-reads are the identical expression
 * the original code used, so no value is interpreted differently, and only {@code false} answers
 * for a belt block are turned into {@code true}.
 */
@Mixin(value = BeltBlockEntity.class, remap = false)
public class BeltBlockEntityMixin {

    /**
     * The guard around {@code initializeItemHandler} and {@code getInventory().tick()} in
     * {@code tick()}.  Create reads {@code this.level.getBlockState(this.worldPosition)} here,
     * {@code getLevel()}/{@code getBlockPos()} are exactly those two fields.
     */
    @ModifyExpressionValue(
            method = "tick()V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$tickTierBelt(boolean original) {
        if (original) {
            return true;
        }

        BeltBlockEntity self = (BeltBlockEntity) (Object) this;
        Level level = self.getLevel();
        return level != null && TieredBeltBlock.isBelt(level.getBlockState(self.getBlockPos()));
    }

    /**
     * {@code hasPulley()} tests {@code this.getBlockState()}, which is re-read here verbatim.
     */
    @ModifyExpressionValue(
            method = "hasPulley()Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$hasPulleyTierBelt(boolean original) {
        return original || TieredBeltBlock.isBelt(((BeltBlockEntity) (Object) this).getBlockState());
    }
}
