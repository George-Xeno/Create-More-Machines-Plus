package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import net.george.cmmplus.content.TieredBeltBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The non-Flywheel render path of a belt.
 *
 * <p>{@code renderSafe} is the guard that returns early for anything that is not Create's own belt,
 * so without this widening a tier belt renders nothing when Flywheel's instancing is unavailable
 * (a fallback that is reached on some drivers and in the self-test client).
 *
 * <p>What is tested is {@code be.getBlockState()}, re-read from the block entity argument, so the
 * value widened is the value Create tested.
 *
 * <p>{@code BeltRenderer} is a client class: it never loads on a dedicated server, and this mixin is
 * therefore listed in the {@code client} section of {@code cmmplus.mixins.json}.
 */
@Mixin(value = BeltRenderer.class, remap = false)
public class BeltRendererMixin {

    @ModifyExpressionValue(
            method = "renderSafe(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$renderTierBelt(boolean original, BeltBlockEntity be) {
        return original || TieredBeltBlock.isBelt(be.getBlockState());
    }
}
