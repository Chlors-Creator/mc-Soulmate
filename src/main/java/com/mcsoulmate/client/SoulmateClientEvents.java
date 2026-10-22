package com.mcsoulmate.client;

import com.mcsoulmate.McSoulmate;
import com.mcsoulmate.entity.ModEntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = McSoulmate.MOD_ID, value = Dist.CLIENT)
public final class SoulmateClientEvents {
    private SoulmateClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WorkHatLayer.LAYER_LOCATION, WorkHatLayer::createLayerDefinition);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.SOULMATE.get(), SoulmateRenderer::new);
    }
}
