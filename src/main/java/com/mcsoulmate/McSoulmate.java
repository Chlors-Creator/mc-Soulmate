package com.mcsoulmate;

import com.mcsoulmate.entity.ModEntityTypes;
import com.mcsoulmate.entity.ModItems;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(McSoulmate.MOD_ID)
public final class McSoulmate {
    public static final String MOD_ID = "mcsoulmate";

    public McSoulmate(IEventBus modEventBus) {
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(McSoulmate::registerEntityAttributes);
        modEventBus.addListener(McSoulmate::addSpawnEggToCreativeTab);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = SoulmateEntity.createAttributes().build();
        event.put(ModEntityTypes.SOULMATE.get(), attributes);
    }

    private static void addSpawnEggToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(ModItems.SOULMATE_SPAWN_EGG.get());
        }
    }
}
