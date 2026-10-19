package com.mcsoulmate.entity;

import com.mcsoulmate.McSoulmate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(McSoulmate.MOD_ID);
    public static final DeferredItem<SpawnEggItem> SOULMATE_SPAWN_EGG = ITEMS.registerItem(
            "soulmate_spawn_egg",
            properties -> new SpawnEggItem(
                    properties
                            .spawnEgg(ModEntityTypes.SOULMATE.get())
                            .rarity(Rarity.UNCOMMON)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    private ModItems() {
    }
}
