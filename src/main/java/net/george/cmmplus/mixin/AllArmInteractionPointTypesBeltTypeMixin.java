package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The mechanical arm's belt interaction point.
 *
 * <p>{@code canCreatePoint} is what decides whether an arm may reach a given block as a belt at
 * all, so without this widening a tier belt is invisible to every arm in the world.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.  The tested
 * state is the method's own third parameter.  The target is the nested
 * {@code AllArmInteractionPointTypes.BeltType} class, where this call site actually lives.
 */
@Mixin(value = AllArmInteractionPointTypes.BeltType.class, remap = false)
public class AllArmInteractionPointTypesBeltTypeMixin {

    @ModifyExpressionValue(
            method = "canCreatePoint(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$armReachesTierBelt(boolean original, Level level, BlockPos pos, BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }
}
