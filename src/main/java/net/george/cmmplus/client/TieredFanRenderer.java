package net.george.cmmplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Create's encased fan renderer for the tier machines.
 *
 * <p>Like Create's own renderer this is the fallback used when Flywheel visuals are not being
 * drawn for a block entity - most visibly for fans that are part of a moving contraption - so the
 * tier fan keeps its spinning shaft and propeller there as well, with the tier's blade texture.
 */
public class TieredFanRenderer extends KineticBlockEntityRenderer<TieredFanBlockEntity> {
    private final PartialModel propeller;

    public TieredFanRenderer(Context context, PartialModel propeller) {
        super(context);
        this.propeller = propeller;
    }

    @Override
    protected void renderSafe(TieredFanBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        Direction direction = be.getBlockState().getValue(BlockStateProperties.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        int lightBehind = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction.getOpposite()));
        int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));
        SuperByteBuffer shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(),
                direction.getOpposite());
        PartialModel blades = CMMPlusClient.usable(propeller, AllPartialModels.ENCASED_FAN_INNER);
        SuperByteBuffer fanInner = CachedBuffers.partialFacing(blades, be.getBlockState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float speed = be.getSpeed() * 5.0f;
        if (speed > 0) {
            speed = Mth.clamp(speed, 80, 1280);
        }
        if (speed < 0) {
            speed = Mth.clamp(speed, -1280, -80);
        }
        float angle = time * speed * 3.0f / 10.0f % 360.0f;
        angle = angle / 180.0f * (float) Math.PI;

        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
        kineticRotationTransform(fanInner, be, direction.getAxis(), angle, lightInFront).renderInto(ms, vb);
    }
}
