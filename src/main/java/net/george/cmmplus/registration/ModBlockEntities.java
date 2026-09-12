package net.george.cmmplus.registration;

import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.george.cmmplus.content.TieredWheelBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * One block entity type per machine, which is what Create 6 asks addons to do: every Create block
 * reports its block entity through {@code IBE#getBlockEntityType()}, so a machine only has to
 * override that method (see {@code TieredFanBlock}/{@code TieredWheelBlock}) and Create's own
 * tickers, stress, goggles, controllers and Flywheel visuals keep working.
 *
 * <p>The ids deliberately match the machine names ({@code cmmplus:encased_fan_brass}, ...), so
 * worlds that already contain these blocks keep their block entities.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CMMPlus.MOD_ID);

    private static final Map<CMMPlusTier, DeferredHolder<BlockEntityType<?>, BlockEntityType<TieredFanBlockEntity>>> FANS =
            new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, DeferredHolder<BlockEntityType<?>, BlockEntityType<TieredWheelBlockEntity>>> WHEELS =
            new EnumMap<>(CMMPlusTier.class);

    static {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            FANS.put(tier, TYPES.register("encased_fan_" + tier.id, () -> BlockEntityType.Builder.of(
                    (pos, state) -> new TieredFanBlockEntity(tier, FANS.get(tier).get(), pos, state),
                    ModBlocks.fan(tier).get()).build(null)));

            WHEELS.put(tier, TYPES.register("crushing_wheel_" + tier.id, () -> BlockEntityType.Builder.of(
                    (pos, state) -> new TieredWheelBlockEntity(tier, WHEELS.get(tier).get(), pos, state),
                    ModBlocks.wheel(tier).get()).build(null)));
        }
    }

    public static BlockEntityType<TieredFanBlockEntity> fan(CMMPlusTier tier) {
        return FANS.get(tier).get();
    }

    public static BlockEntityType<TieredWheelBlockEntity> wheel(CMMPlusTier tier) {
        return WHEELS.get(tier).get();
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
