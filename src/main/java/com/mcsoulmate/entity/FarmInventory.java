package com.mcsoulmate.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

public final class FarmInventory {
    private final List<ItemStack> items = new ArrayList<>();

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public int size() {
        return this.items.size();
    }

    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack stored = this.items.get(slot);
        int removedCount = Math.min(amount, stored.getCount());
        ItemStack removed = stored.copy();
        removed.setCount(removedCount);
        stored.shrink(removedCount);
        if (stored.isEmpty()) {
            this.items.remove(slot);
        }
        return removed;
    }

    public void addItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }

        ItemStack remaining = itemStack.copy();
        this.mergeIntoExistingStacks(remaining);
        this.storeInNewStacks(remaining);
    }

    public List<ItemStack> copyItems() {
        return this.items.stream().map(ItemStack::copy).toList();
    }

    public void replaceItems(List<ItemStack> restoredItems) {
        this.items.clear();
        for (ItemStack item : restoredItems) {
            this.addItem(item);
        }
    }

    private void mergeIntoExistingStacks(ItemStack incoming) {
        for (ItemStack existing : this.items) {
            if (incoming.isEmpty()) {
                return;
            }
            if (!ItemStack.isSameItemSameComponents(existing, incoming)) {
                continue;
            }

            int maximumCount = Math.min(existing.getMaxStackSize(), incoming.getMaxStackSize());
            int freeSpace = maximumCount - existing.getCount();
            int transferCount = Math.min(freeSpace, incoming.getCount());
            existing.grow(transferCount);
            incoming.shrink(transferCount);
        }
    }

    private void storeInNewStacks(ItemStack remaining) {
        while (!remaining.isEmpty()) {
            int stackSize = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack stored = remaining.copy();
            stored.setCount(stackSize);
            this.items.add(stored);
            remaining.shrink(stackSize);
        }
    }
}
