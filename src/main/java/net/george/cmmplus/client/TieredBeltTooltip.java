package net.george.cmmplus.client;

import com.simibubi.create.foundation.item.TooltipModifier;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * The belt behaviour lines shown on a tier's belt connector item.
 *
 * <p>Uses the same mechanism as the tier machines: a Create {@link TooltipModifier} registered per
 * item, exactly like the {@code KineticStats} modifier {@code ModSetup} adds for the fans and
 * wheels (and like Create More Machines' {@code CMMTierTooltip}, whose per-tier lines this
 * mirrors).  It is registered from {@code ModSetup#register}, so it is one line per tier there.
 *
 * <p>Numbers come from {@link CMMPlusTier}, the same fields {@code TieredBeltBlockEntity} and
 * {@code TieredBeltInventory} use: the belt's stress is {@code rpm x length x beltStressFactor} and
 * one block of it carries {@code beltCapacity} positions, where a position holds one item as
 * {@code 64 / maxStackSize} of a stack.  Only the lines that apply to the tier are added - the
 * mixed-load line exists for Beyond alone.
 *
 * <p>Despite living in the client package the class is safe on a dedicated server: it touches no
 * client-only type ({@code ItemTooltipEvent}, {@code Component} and the Create tooltip registry are
 * all common), which is what lets {@code ModSetup} register it without a side check.
 */
public class TieredBeltTooltip implements TooltipModifier {
    private final CMMPlusTier tier;

    public TieredBeltTooltip(CMMPlusTier tier) {
        this.tier = tier;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> tooltip = context.getToolTip();

        // Every tier: what its belt costs, and how much one belt block carries.  The tier's name is
        // not repeated on these lines - the item itself is already called "<tier> Belt", and a
        // trailing "(Brass)" read as noise.
        tooltip.add(Component.translatable("tooltip.cmmplus.belt.stress", tier.beltStressFactor)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.cmmplus.belt.capacity", tier.beltCapacity)
                .withStyle(ChatFormatting.GOLD));

        // Beyond is the only tier whose belt block may hold several item types at once.
        if (tier.mixedBeltLoad()) {
            tooltip.add(Component.translatable("tooltip.cmmplus.belt.mixed", tier.beltCapacity)
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
