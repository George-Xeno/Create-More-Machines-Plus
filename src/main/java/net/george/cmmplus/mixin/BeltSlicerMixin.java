package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wrench and belt-connector interactions with a tier belt.
 *
 * <p>See {@link BeltBlockMixin} for why every {@code AllBlocks.BELT.has(...)} has to be widened on
 * this side: Registrate arrives as an untransformable jar-in-jar gamelibrary in a modpack, so the
 * single widening of {@code BlockEntry#has} in {@link BlockEntryMixin} never takes effect there.
 *
 * <p>The states tested in {@code useWrench}/{@code useConnector} are built in locals
 * ({@code replacedState}, {@code other}, {@code nextState}, {@code blockState}) from a mix of other
 * locals - the belt vector, the hovered end, a {@code Vec3} projection - so they are not derivable
 * from the parameter list.  Rather than re-deriving that arithmetic inside a handler, the tested
 * local is captured with MixinExtras' {@code @Local}, which reads the exact {@link BlockState} the
 * call site passed to {@code BlockEntry#has}; a name that stops resolving is an
 * {@code InjectionError} at load, not a silent no-op.
 *
 * <p>Every injection point is pinned by ordinal <i>and</i> by receiver: Create's methods call
 * {@code BlockEntry#has} for other entries (shafts, casings) in the same method, and an ordinal
 * counts all of them.
 */
@Mixin(value = BeltSlicer.class, remap = false)
public class BeltSlicerMixin {

    /**
     * {@code useWrench} - removing the last segment: {@code replacedState} is the state the removed
     * segment is replaced with.  When it is a pulley, the player gets a shaft back.
     */
    @ModifyExpressionValue(
            method = "useWrench(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lcom/simibubi/create/content/kinetics/belt/BeltSlicer$Feedback;)Lnet/minecraft/world/ItemInteractionResult;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$useWrenchReplacedState(boolean original,
                                                         @Local(name = "replacedState") BlockState replacedState) {
        return original || TieredBeltBlock.isBelt(replacedState);
    }

    /**
     * {@code useWrench} - shortening the belt: {@code other} is the neighbouring segment, and a
     * middle segment there means one more shaft is owed to the player.
     */
    @ModifyExpressionValue(
            method = "useWrench(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lcom/simibubi/create/content/kinetics/belt/BeltSlicer$Feedback;)Lnet/minecraft/world/ItemInteractionResult;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$useWrenchNeighbourSegment(boolean original,
                                                            @Local(name = "other") BlockState other) {
        return original || TieredBeltBlock.isBelt(other);
    }

    /**
     * {@code useConnector} - extending the belt: {@code nextState} is the block in front of the
     * hovered end.  A tier belt there means "merge with that belt" instead of "place a new one".
     */
    @ModifyExpressionValue(
            method = "useConnector(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lcom/simibubi/create/content/kinetics/belt/BeltSlicer$Feedback;)Lnet/minecraft/world/ItemInteractionResult;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$useConnectorNextState(boolean original,
                                                        @Local(name = "nextState") BlockState nextState) {
        return original || TieredBeltBlock.isBelt(nextState);
    }

    /**
     * {@code useConnector} - the loop that walks the merged belt back to its start.  A tier belt
     * that fails this test reads as "the chain ends here", so the loop stops early and the merged
     * run keeps the wrong controller.
     */
    @ModifyExpressionValue(
            method = "useConnector(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;"
                    + "Lcom/simibubi/create/content/kinetics/belt/BeltSlicer$Feedback;)Lnet/minecraft/world/ItemInteractionResult;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$useConnectorChainWalk(boolean original,
                                                        @Local(name = "blockState") BlockState blockState) {
        return original || TieredBeltBlock.isBelt(blockState);
    }
}
