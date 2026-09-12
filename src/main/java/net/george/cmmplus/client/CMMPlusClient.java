package net.george.cmmplus.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.george.cmmplus.content.TieredWheelBlockEntity;
import net.george.cmmplus.registration.ModBlockEntities;
import net.george.cmmplus.registration.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Client side wiring: Flywheel visuals plus the fan's fallback renderer, mirroring exactly what
 * Create registers for {@code create:encased_fan} and {@code create:crushing_wheel}
 * (see {@code AllBlockEntityTypes}).  Both are keyed by block entity type, so the tier machines
 * need their own entry - without it the wheels, which never render a block model
 * ({@code RenderShape.ENTITYBLOCK_ANIMATED}), would simply be invisible.
 */
@EventBusSubscriber(modid = CMMPlus.MOD_ID, value = Dist.CLIENT)
public class CMMPlusClient {
    private static final Set<ResourceLocation> REPORTED_FALLBACKS = new HashSet<>();

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            event.registerBlockEntityRenderer(ModBlockEntities.fan(tier),
                    context -> new TieredFanRenderer(context, CMMPlusPartialModels.propeller(tier)));

            // Create registers BeltRenderer for create:belt right next to its belt visual
            // (AllBlockEntityTypes.BELT: ".renderer(() -> BeltRenderer::new)"), and the belt
            // visualizer alone does not replace it: the visual draws the belt surface, the vanilla
            // renderer draws the items riding the belt.  A tier belt has its own block entity type,
            // so it needs its own entry or those items are simply not drawn.  BeltRenderer is
            // written against BeltBlockEntity and NeoForge's registration takes a
            // BlockEntityType<? extends T>, so Create's renderer can be reused as is.
            event.registerBlockEntityRenderer(ModBlockEntities.belt(tier), BeltRenderer::new);
        }
    }

    /**
     * Side-loaded models have to be announced to the model loader, otherwise they are never baked
     * and Flywheel has nothing to draw - the tier machines would be invisible while every log line
     * still looks healthy.  Flywheel does this for the partial models it already knows about, but
     * only at the moment the loader asks, so a pack where our class is first touched later (a long
     * mod list is enough) would silently miss it.  Registering them here, from the same event,
     * removes that dependency on ordering - creating the models in this handler guarantees they
     * exist before the bake finishes.
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        CMMPlusPartialModels.init();
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            event.register(ModelResourceLocation.standalone(CMMPlusPartialModels.wheel(tier).modelLocation()));
            event.register(ModelResourceLocation.standalone(CMMPlusPartialModels.propeller(tier).modelLocation()));
        }
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CMMPlusClient::registerVisuals);
    }

    /**
     * The machines are drawn from Flywheel partial models rather than from the block model, so a
     * model that failed to bake would only show up as a crash the first time a machine is
     * rendered.  Check right after the bake instead.
     */
    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        CMMPlusPartialModels.init();
        int missing = 0;
        for (CMMPlusTier tier : CMMPlusTier.values()) {
            missing += checkModel(event, CMMPlusPartialModels.wheel(tier));
            missing += checkModel(event, CMMPlusPartialModels.propeller(tier));
        }
        if (missing == 0) {
            CMMPlus.LOGGER.info("[client] all {} tier machine models baked", CMMPlusTier.values().length * 2);
        } else {
            CMMPlus.LOGGER.error("[client] {} tier machine model(s) failed to bake", missing);
        }
    }

    private static int checkModel(ModelEvent.BakingCompleted event, PartialModel partial) {
        BakedModel baked = event.getModels().get(ModelResourceLocation.standalone(partial.modelLocation()));
        if (baked == null) {
            CMMPlus.LOGGER.error("[client] model {} did not bake", partial.modelLocation());
            return 1;
        }
        return 0;
    }

    private static void registerVisuals() {
        CMMPlusPartialModels.init();
        // Touches the belt sprite shifts so catnip has them registered before the first texture
        // stitch - a shift created after a stitch would have no atlas sprite until the next one.
        CMMPlusSpriteShifts.init();

        for (CMMPlusTier tier : CMMPlusTier.values()) {
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.fan(tier))
                    .factory(TieredFanVisual.factory(CMMPlusPartialModels.propeller(tier)))
                    .skipVanillaRender(be -> true)
                    .apply();

            // Create's own model is used as a last resort, evaluated when the visual is built (after
            // the bake), so a machine is never invisible even if its model is unavailable.
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.wheel(tier))
                    .factory(TieredWheelVisual.factory(CMMPlusPartialModels.wheel(tier)))
                    .skipVanillaRender(be -> true)
                    .apply();

            // A belt's uncased block model is the empty particle model, so without a visual nothing
            // is drawn.  This is Create's BeltVisual with the tier's own scrolling surface (see
            // TieredBeltVisual / CMMPlusSpriteShifts); the tier comes from the block entity, so
            // there is no model to resolve here.
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.belt(tier))
                    .factory(TieredBeltVisual.factory())
                    // BeltBlockEntity#shouldRenderNormally is true for the piece whose vanilla
                    // renderer still has work to do - the controller, which draws the items riding
                    // the belt - so that one must not be skipped.  Create registers its belt visual
                    // with this exact predicate (AllBlockEntityTypes.BELT).
                    .skipVanillaRender(be -> !be.shouldRenderNormally())
                    .apply();

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.belt(tier).get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.fan(tier).get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.wheel(tier).get(), RenderType.cutoutMipped());
        }
    }

    /** The tier's own model, or Create's equivalent if it is somehow not available. */
    public static PartialModel usable(PartialModel preferred, PartialModel fallback) {
        if (preferred.get() != null) {
            return preferred;
        }
        if (REPORTED_FALLBACKS.add(preferred.modelLocation())) {
            CMMPlus.LOGGER.error("[client] partial model {} is not available, drawing {} instead",
                    preferred.modelLocation(), fallback.modelLocation());
        }
        return fallback;
    }
}
