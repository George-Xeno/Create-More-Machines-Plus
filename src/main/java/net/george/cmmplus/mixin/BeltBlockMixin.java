package net.george.cmmplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.george.cmmplus.content.TieredBeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops Create from destroying tier belts the moment they are placed or connected.
 *
 * <p>{@link BeltBlock#initBelt} walks the chain around the connector and then asks
 * {@code AllBlocks.BELT.has(state)} - a Registrate {@link BlockEntry#has(BlockState)}, which is a
 * bare reference comparison against {@code create:belt}.  A belt of a tier is a
 * {@link TieredBeltBlock}, registered under its own name, so every one of those checks answers
 * false on the first walk (the loop in {@code initBelt} treats the very first non-matching segment
 * as a broken chain) and Create immediately runs {@code world.destroyBlock(pos, true)} on the belt
 * it was just asked to initialise.  The same false answer disables item transport
 * ({@code canTransportObjects}), the belt chain ({@code getBeltChain}), entity riding
 * ({@code updateEntityAfterFallOn}) and teardown of the neighbouring segments ({@code onRemove}).
 *
 * <p>{@link BlockEntryMixin} widens the lookup itself, which would cover every call site at once,
 * but Registrate is loaded as a jar-in-jar <b>gamelibrary</b> in a modpack and game-library classes
 * are not transformable, so that mixin never takes effect there (it is kept because it is harmless
 * and still correct in a development environment).  Every fix therefore has to be applied on this
 * side, at the call sites.
 *
 * <p>All eleven checks in this class are widened with one {@code @ModifyExpressionValue} each, at
 * the very instruction Create asks the question - pinned by ordinal, because {@code ordinal} counts
 * every {@code BlockEntry#has} match in the method, including the ones asking about shafts and
 * casings.  The rule is always the same: <i>any</i> {@link BeltBlock} counts as a belt, so Create's
 * own belt keeps behaving exactly as before and only the tier belts start answering true
 * ({@code original || TieredBeltBlock.isBelt(state)} - a non-belt state keeps the original answer).
 * Nothing else about the enclosing method changes: no reordering, no other condition touched.
 *
 * <p>To widen the right value the handler needs the {@link BlockState} Create tested.  Where that
 * value can be re-read from the method's own parameters (the connector's position, the controller
 * position) the handler repeats the identical expression.  Where it is the running state of a chain
 * walk - {@code currentState}, {@code state} - it is captured with MixinExtras' {@code @Local}
 * instead of being rebuilt, because the local <i>is</i> the value Create passed to
 * {@code BlockEntry#has}.  A {@code @Local} name that stops resolving (or one that becomes
 * ambiguous) fails the injection counter and raises an {@code InjectionError} at load; it cannot
 * quietly leave the belt-destroying bug in place.
 */
@Mixin(value = BeltBlock.class, remap = false)
public class BeltBlockMixin {

    /**
     * {@code updateEntityAfterFallOn} tests the block the entity is standing in, then the one
     * below it, before letting the entity ride the belt.  Both are
     * {@code worldIn.getBlockState(entityPosition)} for the local {@code entityPosition}, which is
     * {@code entityIn.blockPosition()}, so the handlers rebuild that expression.
     */
    @ModifyExpressionValue(
            method = "updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$beltAtEntity(boolean original, BlockGetter worldIn, Entity entityIn) {
        return original || TieredBeltBlock.isBelt(worldIn.getBlockState(entityIn.blockPosition()));
    }

    /** The second {@code updateEntityAfterFallOn} check: the block directly below the entity. */
    @ModifyExpressionValue(
            method = "updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$beltBelowEntity(boolean original, BlockGetter worldIn, Entity entityIn) {
        return original || TieredBeltBlock.isBelt(worldIn.getBlockState(entityIn.blockPosition().below()));
    }

    /**
     * {@code canTransportObjects} is Create's "is this a belt I may put items on" test.  It gates
     * item insertion, entity riding and the belt's item handler capability, so a tier belt whose
     * state fails here is an inert decoration.  The tested state is the method's own parameter.
     */
    @ModifyExpressionValue(
            method = "canTransportObjects(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$canTransportTierBelts(boolean original, BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }

    /**
     * The entry check of {@code initBelt}: the block the connector was used on
     * ({@code state = world.getBlockState(pos)}).  This is the check that, left alone, makes Create
     * walk off the chain and destroy the freshly placed belt.
     */
    @ModifyExpressionValue(
            method = "initBelt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$initTierBelt(boolean original, Level world, BlockPos pos) {
        return original || TieredBeltBlock.isBelt(world.getBlockState(pos));
    }

    /**
     * The chain walk of {@code initBelt}: the state at the loop's running position
     * ({@code currentState = world.getBlockState(currentPos)}).  A tier belt that fails here ends
     * the walk immediately, which is what turns "the belt the player just built" into a
     * {@code destroyBlock} call.
     */
    @ModifyExpressionValue(
            method = "initBelt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$initTierBeltChainWalk(boolean original,
                                                        @Local(name = "currentState") BlockState currentState) {
        return original || TieredBeltBlock.isBelt(currentState);
    }

    /**
     * The per-segment loop of {@code initBelt} - the same test once per segment the chain walk
     * found.  Failing it makes Create destroy the belt and bail out of wiring the controller,
     * length and indices.
     */
    @ModifyExpressionValue(
            method = "initBelt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 2, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$initTierBeltSegment(boolean original,
                                                      @Local(name = "currentState") BlockState currentState) {
        return original || TieredBeltBlock.isBelt(currentState);
    }

    /**
     * {@code getBeltChain}'s entry check: the controller position, straight from the parameters
     * ({@code blockState = world.getBlockState(controllerPos)}).
     */
    @ModifyExpressionValue(
            method = "getBeltChain(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Ljava/util/List;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$getTierBeltChain(boolean original, LevelAccessor world, BlockPos controllerPos) {
        return original || TieredBeltBlock.isBelt(world.getBlockState(controllerPos));
    }

    /**
     * {@code getBeltChain}'s walk.  A tier belt that fails here looks like a one-segment chain, so
     * the belt never gets a controller, an inventory or a length.
     */
    @ModifyExpressionValue(
            method = "getBeltChain(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Ljava/util/List;",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1, remap = false),
            require = 1, remap = false)
    private static boolean cmmplus$getTierBeltChainWalk(boolean original,
                                                       @Local(name = "state") BlockState state) {
        return original || TieredBeltBlock.isBelt(state);
    }

    /**
     * {@code onRemove} looks at both neighbouring segments and turns each one it recognises into a
     * shaft (or air).  Left alone it would skip every tier belt, leaving orphaned segment block
     * entities behind.
     */
    @ModifyExpressionValue(
            method = "onRemove(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0, remap = false),
            require = 1, remap = false)
    private boolean cmmplus$onRemoveNeighbourSegment(boolean original,
                                                    @Local(name = "currentState") BlockState currentState) {
        return original || TieredBeltBlock.isBelt(currentState);
    }
}
