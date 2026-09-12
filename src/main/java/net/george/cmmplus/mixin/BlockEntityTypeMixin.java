package net.george.cmmplus.mixin;

import com.simibubi.create.AllBlockEntityTypes;
import net.george.cmmplus.content.TieredFanBlock;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.george.cmmplus.content.TieredWheelBlock;
import net.george.cmmplus.content.TieredWheelBlockEntity;
import net.george.cmmplus.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Upgrade path for worlds saved by an older build of this mod.
 *
 * <p>Tier machines have their own block entity types now (see {@code ModBlockEntities}), which is
 * how Create expects addon machines to be built: {@code IBE#newBlockEntity} asks the block for its
 * type.  Earlier builds instead reused Create's own block entity types, so those chunks store
 * {@code create:encased_fan} / {@code create:crushing_wheel} at tier machine positions and
 * Minecraft would hand such a machine a plain Create block entity - it would keep working, but only
 * at the vanilla tier.
 *
 * <p>Every block entity that comes out of saved data is built by {@code BlockEntityType#create}
 * ({@code BlockEntity#loadStatic} calls it, and that is what chunk loading uses), so this is the
 * one place where a stored Create type can be turned back into the tier machine it belongs to.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private void cmmplus$restoreTierMachine(BlockPos pos, BlockState state,
                                            CallbackInfoReturnable<BlockEntity> cir) {
        BlockEntityType<?> type = (BlockEntityType<?>) (Object) this;
        Block block = state.getBlock();

        if (block instanceof TieredFanBlock fan && type == AllBlockEntityTypes.ENCASED_FAN.get()) {
            cir.setReturnValue(new TieredFanBlockEntity(fan.tier(), ModBlockEntities.fan(fan.tier()), pos, state));
        } else if (block instanceof TieredWheelBlock wheel && type == AllBlockEntityTypes.CRUSHING_WHEEL.get()) {
            cir.setReturnValue(new TieredWheelBlockEntity(wheel.tier(), ModBlockEntities.wheel(wheel.tier()), pos, state));
        }
    }

    /**
     * Safety net for the same situation on any code path that does not create the block entity
     * itself: Create's type must not refuse our block, or the machine's block entity would be
     * dropped and the machine left dead.  Only these two Create types are widened.
     */
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void cmmplus$acceptLegacyTierMachines(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        BlockEntityType<?> type = (BlockEntityType<?>) (Object) this;
        Block block = state.getBlock();

        if (block instanceof TieredFanBlock && type == AllBlockEntityTypes.ENCASED_FAN.get()) {
            cir.setReturnValue(true);
        } else if (block instanceof TieredWheelBlock && type == AllBlockEntityTypes.CRUSHING_WHEEL.get()) {
            cir.setReturnValue(true);
        }
    }
}
