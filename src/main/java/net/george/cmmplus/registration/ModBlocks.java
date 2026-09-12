package net.george.cmmplus.registration;

import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.content.TieredBeltBlock;
import net.george.cmmplus.content.TieredFanBlock;
import net.george.cmmplus.content.TieredWheelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * The tiered machine blocks.
 *
 * <p>Properties mirror Create's own machines (see Create's {@code AllBlocks.ENCASED_FAN} and
 * {@code AllBlocks.CRUSHING_WHEEL}, both built from {@code SharedProperties.stone()} = andesite):
 * same hardness, same tool requirements, same map colours.  The blocks themselves only add the
 * tier plus their own block entity type, everything else is inherited from Create.
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CMMPlus.MOD_ID);

    private static final Map<CMMPlusTier, DeferredBlock<TieredFanBlock>> FANS = new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, DeferredBlock<TieredWheelBlock>> WHEELS = new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, DeferredBlock<TieredBeltBlock>> BELTS = new EnumMap<>(CMMPlusTier.class);

    static {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            BlockBehaviour.Properties fanProperties = BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE)
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.STONE);
            FANS.put(tier, BLOCKS.register("encased_fan_" + tier.id,
                    () -> new TieredFanBlock(fanProperties, tier)));

            BlockBehaviour.Properties wheelProperties = BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE)
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.STONE)
                    .noOcclusion();
            WHEELS.put(tier, BLOCKS.register("crushing_wheel_" + tier.id,
                    () -> new TieredWheelBlock(wheelProperties, tier)));

            // Create's own belt properties (see AllBlocks.BELT): the belt is not stone, it is a
            // wool-soft 0.8 strength block.  Everything else (waterlogging, shape, slope) comes
            // from BeltBlock itself.
            BlockBehaviour.Properties beltProperties = BlockBehaviour.Properties.of()
                    .sound(SoundType.WOOL)
                    .strength(0.8F)
                    .mapColor(MapColor.COLOR_GRAY);
            BELTS.put(tier, BLOCKS.register("belt_" + tier.id,
                    () -> new TieredBeltBlock(beltProperties, tier)));
        }
    }

    public static DeferredBlock<TieredBeltBlock> belt(CMMPlusTier tier) {
        return BELTS.get(tier);
    }

    public static DeferredBlock<TieredFanBlock> fan(CMMPlusTier tier) {
        return FANS.get(tier);
    }

    public static DeferredBlock<TieredWheelBlock> wheel(CMMPlusTier tier) {
        return WHEELS.get(tier);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
