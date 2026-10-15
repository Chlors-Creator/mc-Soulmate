package com.mcsoulmate;

import com.mcsoulmate.entity.ModEntityTypes;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(McSoulmate.MOD_ID)
public final class McSoulmate {
    public static final String MOD_ID = "mcsoulmate";

    public McSoulmate(IEventBus modEventBus) {
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(McSoulmate::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = SoulmateEntity.createAttributes().build();
        event.put(ModEntityTypes.SOULMATE.get(), attributes);
    }
}