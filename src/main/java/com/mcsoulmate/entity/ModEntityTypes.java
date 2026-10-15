package com.mcsoulmate.entity;

import java.util.function.Supplier;

import com.mcsoulmate.McSoulmate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    private static final float SOULMATE_WIDTH = 0.6F;
    private static final float SOULMATE_HEIGHT = 1.8F;
    private static final float SOULMATE_EYE_HEIGHT = 1.62F;
    private static final int CLIENT_TRACKING_RANGE = 8;

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(McSoulmate.MOD_ID);

    public static final Supplier<EntityType<SoulmateEntity>> SOULMATE =
            ENTITY_TYPES.registerEntityType(
                    "soulmate",
                    SoulmateEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder
                            .sized(SOULMATE_WIDTH, SOULMATE_HEIGHT)
                            .eyeHeight(SOULMATE_EYE_HEIGHT)
                            .clientTrackingRange(CLIENT_TRACKING_RANGE)
            );

    private ModEntityTypes() {
    }
}