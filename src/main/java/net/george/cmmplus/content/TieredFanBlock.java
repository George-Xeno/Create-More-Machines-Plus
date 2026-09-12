package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.registration.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A fan with its own block entity type.
 *
 * <p>Create's {@code IBE#newBlockEntity} simply calls {@code getBlockEntityType().create(...)},
 * so overriding that one method is all an addon needs to give a block its own machine - exactly
 * how Create More Machines builds its tiered saw/press/deployer.  Because the block entity type
 * knows its block, Minecraft's validity check passes and the machine is never dropped.
 */
public class TieredFanBlock extends EncasedFanBlock {
    private final CMMPlusTier tier;

    public TieredFanBlock(Properties properties, CMMPlusTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CMMPlusTier tier() {
        return tier;
    }

    /**
     * Names the machine "&lt;tier&gt; &lt;Create machine&gt;" with the tier in yellow, the same way
     * Create More Machines names its tier machines (see {@code CMMSawBlock#getDescriptionId}); both
     * halves are translated, so the name follows the client language.
     */
    @Override
    public String getDescriptionId() {
        return ChatFormatting.YELLOW + Component.translatable(tier.translationKey()).getString()
                + ChatFormatting.WHITE + Component.translatable("block.create.encased_fan").getString();
    }

    @Override
    public Class<EncasedFanBlockEntity> getBlockEntityClass() {
        // Deliberately the base class: block entity lookups then also accept a fan that was
        // loaded from a world saved by an older build of this mod (see BlockEntityTypeMixin).
        return EncasedFanBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EncasedFanBlockEntity> getBlockEntityType() {
        return ModBlockEntities.fan(tier);
    }
}
