package net.george.cmmplus.registration;

import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.content.TieredBeltConnectorItem;
import net.george.cmmplus.item.TieredBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** One BlockItem per block, registered explicitly so the item ids always exist. */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CMMPlus.MOD_ID);

    private static final Map<CMMPlusTier, DeferredItem<TieredBlockItem>> FANS = new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, DeferredItem<TieredBlockItem>> WHEELS = new EnumMap<>(CMMPlusTier.class);
    private static final Map<CMMPlusTier, DeferredItem<TieredBeltConnectorItem>> BELT_CONNECTORS =
            new EnumMap<>(CMMPlusTier.class);

    static {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            FANS.put(tier, ITEMS.registerItem("encased_fan_" + tier.id,
                    props -> new TieredBlockItem(ModBlocks.fan(tier).get(), props, tier)));
            WHEELS.put(tier, ITEMS.registerItem("crushing_wheel_" + tier.id,
                    props -> new TieredBlockItem(ModBlocks.wheel(tier).get(), props, tier)));
            BELT_CONNECTORS.put(tier, ITEMS.registerItem("belt_connector_" + tier.id,
                    props -> new TieredBeltConnectorItem(props, tier)));
        }
    }

    public static DeferredItem<TieredBeltConnectorItem> beltConnector(CMMPlusTier tier) {
        return BELT_CONNECTORS.get(tier);
    }

    public static Item fan(CMMPlusTier tier) {
        return FANS.get(tier).get();
    }

    public static Item wheel(CMMPlusTier tier) {
        return WHEELS.get(tier).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
