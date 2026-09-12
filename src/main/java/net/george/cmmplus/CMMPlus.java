package net.george.cmmplus;

import net.george.cmmplus.registration.ModBlockEntities;
import net.george.cmmplus.registration.ModBlocks;
import net.george.cmmplus.registration.ModCreativeTabs;
import net.george.cmmplus.registration.ModItems;
import net.george.cmmplus.registration.ModSetup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CMMPlus.MOD_ID)
public class CMMPlus {
    public static final String MOD_ID = "cmmplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CMMPlus(IEventBus modBus, ModContainer container) {
        // Tier stress impacts are startup values, as in Create More Machines (Type.STARTUP).
        // The listener has to be added *before* registering the spec: a startup config is read
        // right away, so the load event fires during the registerConfig call (CMM does the same
        // in CreateMoreMachines#CreateMoreMachines).
        modBus.addListener(CMMPlusConfig::onLoad);
        container.registerConfig(ModConfig.Type.STARTUP, CMMPlusConfig.SPEC);

        // Blocks first: the block entity types below need the registered blocks for their
        // valid-block sets, and the items need the blocks for their block items.
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ModSetup::register));
        modBus.addListener(ModSetup::registerCapabilities);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
