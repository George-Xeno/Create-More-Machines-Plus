package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The hovered belt readout, which is what turns the wrench/connector highlights on in the first
 * place.
 *
 * <p>{@code BeltSlicer} itself is a common class, but the method widened here carries
 * {@code @OnlyIn(Dist.CLIENT)} - NeoForge's {@code RuntimeDistCleaner} <b>removes</b> such methods
 * from the class on a dedicated server, so this mixin is listed in the {@code client} section of
 * {@code cmmplus.mixins.json} and is never applied on a server (where the method does not exist).
 *
 * <p>Without the widening, pointing at a tier belt with a wrench or connector in hand shows no
 * feedback at all.
 */
@Mixin(value = BeltSlicer.class, remap = false)
public class BeltSlicerClientMixin {

    @ModifyExpressionValue(
            method = "tickHoveringInformation()V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$hoveringTierBelt(boolean original,
                                                   @Local(name = "state") BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }
}
