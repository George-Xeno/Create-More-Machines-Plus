package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Riding a belt: the test that decides whether an entity has moved past the ending slope.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.
 *
 * <p>The tested states are re-read from the method's own arguments - {@code world} is
 * {@code beltBE.getLevel()} and the positions are {@code entityIn.blockPosition()} and the block
 * below it, which is exactly what the call sites read - so the handler widens the same values
 * Create tested, and nothing else in {@code transportEntity} changes.
 */
@Mixin(value = BeltMovementHandler.class, remap = false)
public class BeltMovementHandlerMixin {

    /**
     * The block the entity is standing in.  A tier belt that answers false here makes the entity
     * drop off the end of the belt instead of being carried past the slope.
     */
    @ModifyExpressionValue(
            method = "transportEntity(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;"
                    + "Lnet/minecraft/world/entity/Entity;"
                    + "Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler$TransportedEntityInfo;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$movedPastEndingSlopeHere(boolean original, BeltBlockEntity beltBE,
                                                           Entity entityIn) {
        Level level = beltBE.getLevel();
        return original || (level != null && TieredBeltBlock.isBelt(level.getBlockState(entityIn.blockPosition())));
    }

    /** The same test one block lower, for the slope segment the entity has just left. */
    @ModifyExpressionValue(
            method = "transportEntity(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;"
                    + "Lnet/minecraft/world/entity/Entity;"
                    + "Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler$TransportedEntityInfo;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$movedPastEndingSlopeBelow(boolean original, BeltBlockEntity beltBE,
                                                            Entity entityIn) {
        Level level = beltBE.getLevel();
        return original
                || (level != null && TieredBeltBlock.isBelt(level.getBlockState(entityIn.blockPosition().below())));
    }
}
