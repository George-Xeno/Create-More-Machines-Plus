package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A deployer on a contraption that is replaying a schematic: belt blocks come out of the schematic
 * world with their shafts and casings instead of being placed as plain blocks.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  A tier belt
 * that fails this test is placed as a bare block by the following {@code launchBlock} path, so the
 * printed belt loses its block entity, pulleys and casing.
 *
 * <p>The state tested lives in the local {@code blockState}, read from a {@code SchematicLevel}
 * that is itself looked up from the method's arguments; the lookup is not repeated here (it is a
 * cached-instance lookup with its own guard clauses), the local is captured with {@code @Local}.
 */
@Mixin(value = DeployerMovementBehaviour.class, remap = false)
public class DeployerMovementBehaviourMixin {

    @ModifyExpressionValue(
            method = "activateAsSchematicPrinter("
                    + "Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;"
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$printedTierBelt(boolean original,
                                           @Local(name = "blockState") BlockState blockState) {
        return original || TieredBeltBlock.isBelt(blockState);
    }
}
