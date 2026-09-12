package net.george.cmmplus.mixin;

import com.simibubi.create.AllBlocks;
import net.george.cmmplus.content.TieredBeltBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Create accept tiered belts wherever it asks for its own belt block.
 *
 * <p>Create's belt code is full of {@code AllBlocks.BELT.has(state)} - roughly 37 call sites -
 * and Registrate's {@link BlockEntry#has(BlockState)} compares the entry's own block, so a belt
 * registered by an addon answers false everywhere.  The consequences are not cosmetic: without
 * this, {@code BeltBlockEntity#tick} never initialises the belt's item handling,
 * {@code BeltBlockEntity#hasPulley} is false (so the belt never connects to the kinetic network
 * and never draws its pulleys), {@code BeltBlock#initBelt} and {@code canTransportObjects} bail
 * out, and belt tunnels, funnels, deployers, presses, contraptions and schematics all ignore the
 * belt.
 *
 * <p>Rather than patching every call site, the family rule is applied once, at the lookup: if the
 * entry being queried is a belt entry and the state in question is any belt, the answer is yes.
 * Vanilla behaviour is untouched (Create's own belt still matches its own entry).
 */
@Mixin(value = BlockEntry.class, remap = false)
public class BlockEntryMixin {

    /**
     * BlockEntry declares exactly one overload - {@code has(BlockState)} - and it is a bare
     * reference comparison ({@code this.get() == state.getBlock()}), so this single injection
     * point covers every {@code AllBlocks.BELT.has(...)} call in Create.
     */
    @Inject(method = "has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void cmmplus$hasTierBeltState(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof TieredBeltBlock
                && ((BlockEntry<?>) (Object) this).get() == AllBlocks.BELT.get()) {
            cir.setReturnValue(true);
        }
    }
}
