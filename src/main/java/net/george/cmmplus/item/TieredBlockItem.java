package net.george.cmmplus.item;

import net.george.cmmplus.CMMPlusTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** BlockItem that shows the tier's processing speed as a tooltip line. */
public class TieredBlockItem extends BlockItem {
    private final CMMPlusTier tier;

    public TieredBlockItem(Block block, Item.Properties properties, CMMPlusTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.cmmplus.speed", tier.speedText())
                .withStyle(ChatFormatting.GOLD));
    }
}
