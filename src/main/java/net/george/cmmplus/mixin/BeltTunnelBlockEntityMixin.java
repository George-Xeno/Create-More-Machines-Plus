package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The tunnel's item handler capability: it is taken from the block below, but only if that block is
 * a belt.  A tunnel on a tier belt therefore exposes no item handler at all, so nothing can be
 * inserted into the belt through it.
 *
 * <p>See {@link BeltBlockMixin} for why this widening has to exist on the Create side.
 *
 * <p>The call site is inside the lambda registered by {@code registerCapabilities}, which javac
 * compiles into the private static synthetic method {@code lambda$registerCapabilities$0}.  It is
 * targeted by that exact compiler-generated name and descriptor (read out of Create 6.0.10's
 * bytecode, not guessed); {@code require = 1} makes a jar whose lambda numbering differs fail loudly
 * at load instead of silently keeping the bug.
 *
 * <p>What is tested is {@code be.level.getBlockState(be.worldPosition.below())}; the handler
 * re-reads it through the public {@code getLevel()}/{@code getBlockPos()} accessors, which are
 * exactly those two fields.
 */
@Mixin(value = BeltTunnelBlockEntity.class, remap = false)
public class BeltTunnelBlockEntityMixin {

    @ModifyExpressionValue(
            method = "lambda$registerCapabilities$0(Lcom/simibubi/create/content/logistics/tunnel/BeltTunnelBlockEntity;"
                    + "Lnet/minecraft/core/Direction;)Lnet/neoforged/neoforge/items/IItemHandler;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$tunnelCapFromTierBelt(boolean original, BeltTunnelBlockEntity be,
                                                        Direction context) {
        Level level = be.getLevel();
        return original
                || (level != null && TieredBeltBlock.isBelt(level.getBlockState(be.getBlockPos().below())));
    }
}
