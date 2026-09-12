package net.george.cmmplus.registration;

import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CMMPlus.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cmmplus"))
                    .icon(() -> new ItemStack(ModBlocks.wheel(CMMPlusTier.BRASS).get()))
                    .displayItems((params, output) -> {
                        for (CMMPlusTier tier : CMMPlusTier.values()) {
                            output.accept(ModBlocks.fan(tier).get());
                            output.accept(ModBlocks.wheel(tier).get());
                        }
                    })
                    .build());

    public static void register(IEventBus modBus) { TABS.register(modBus); }
}