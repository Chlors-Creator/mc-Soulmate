package com.mcsoulmate.entity;

import java.util.List;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SoulmateInventory {
    private static final int FISHES_PER_STORAGE_BATCH = 10;

    private final FarmInventory farming = new FarmInventory();
    private final FarmInventory fishing = new FarmInventory();
    private final SoulmateEquipment equipment = new SoulmateEquipment();
    private final SoulmateWorkwear workwear = new SoulmateWorkwear();

    public FarmInventory farming() {
        return this.farming;
    }

    public FarmInventory fishing() {
        return this.fishing;
    }

    public SoulmateEquipment equipment() {
        return this.equipment;
    }

    public SoulmateWorkwear workwear() {
        return this.workwear;
    }

    public boolean hasFishingStorageBatchReady() {
        return this.fishing.countItems(ItemTags.FISHES) >= FISHES_PER_STORAGE_BATCH;
    }

    public void save(ValueOutput output) {
        output.store("FarmingInventory", ItemStack.CODEC.listOf(), this.farming.copyItems());
        output.store("FishingInventory", ItemStack.CODEC.listOf(), this.fishing.copyItems());
        this.equipment.save(output);
    }

    public void load(ValueInput input) {
        this.farming.replaceItems(input.read("FarmingInventory", ItemStack.CODEC.listOf()).orElseGet(List::of));
        this.fishing.replaceItems(input.read("FishingInventory", ItemStack.CODEC.listOf()).orElseGet(List::of));
        this.equipment.load(input);
        for (ItemStack fish : this.farming.removeItems(ItemTags.FISHES)) {
            this.fishing.addItem(fish);
        }
    }
}
