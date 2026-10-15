package com.mcsoulmate.entity.ai;

import java.util.Comparator;
import java.util.Optional;

import com.mcsoulmate.entity.FarmInventory;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

final class FarmStorage {
    private static final int CHEST_SEARCH_RADIUS = 20;
    private static final int CHEST_SEARCH_RADIUS_SQUARED = CHEST_SEARCH_RADIUS * CHEST_SEARCH_RADIUS;

    private FarmStorage() {
    }

    static Optional<BlockPos> findNearbyChest(SoulmateEntity soulmate) {
        if (!(soulmate.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        BlockPos center = soulmate.blockPosition();
        BlockPos minimum = center.offset(-CHEST_SEARCH_RADIUS, -CHEST_SEARCH_RADIUS, -CHEST_SEARCH_RADIUS);
        BlockPos maximum = center.offset(CHEST_SEARCH_RADIUS, CHEST_SEARCH_RADIUS, CHEST_SEARCH_RADIUS);
        return BlockPos.betweenClosedStream(minimum, maximum)
                .map(BlockPos::immutable)
                .filter(position -> isWithinRange(soulmate, position))
                .filter(level::hasChunkAt)
                .filter(position -> hasSpaceInChest(level, position, soulmate.getFarmingInventory()))
                .min(Comparator.comparingDouble(position -> distanceSquared(soulmate, position)));
    }

    static void depositInventory(SoulmateEntity soulmate, BlockPos chestPosition) {
        if (!(soulmate.level() instanceof ServerLevel level)) {
            return;
        }

        Container chest = getChestContainer(level, chestPosition);
        if (chest == null) {
            return;
        }

        boolean changed = moveInventoryItems(soulmate.getFarmingInventory(), chest);
        if (changed) {
            chest.setChanged();
        }
    }

    private static boolean isWithinRange(SoulmateEntity soulmate, BlockPos position) {
        return distanceSquared(soulmate, position) <= CHEST_SEARCH_RADIUS_SQUARED;
    }

    private static double distanceSquared(SoulmateEntity soulmate, BlockPos position) {
        return soulmate.distanceToSqr(
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D
        );
    }

    private static boolean hasSpaceInChest(ServerLevel level, BlockPos position, FarmInventory inventory) {
        Container chest = getChestContainer(level, position);
        if (chest == null) {
            return false;
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && hasRoom(chest, stack)) {
                return true;
            }
        }
        return false;
    }

    private static Container getChestContainer(ServerLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return null;
        }
        return ChestBlock.getContainer(chestBlock, state, level, position, false);
    }

    private static boolean hasRoom(Container chest, ItemStack stack) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (!chest.canPlaceItem(slot, stack)) {
                continue;
            }

            ItemStack existing = chest.getItem(slot);
            if (existing.isEmpty() || canMerge(existing, stack) && hasStackCapacity(chest, existing)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canMerge(ItemStack existing, ItemStack incoming) {
        return ItemStack.isSameItemSameComponents(existing, incoming);
    }

    private static boolean hasStackCapacity(Container chest, ItemStack stack) {
        int maximumCount = Math.min(chest.getMaxStackSize(), stack.getMaxStackSize());
        return stack.getCount() < maximumCount;
    }

    private static boolean moveInventoryItems(FarmInventory inventory, Container chest) {
        boolean changed = false;
        for (int slot = inventory.size() - 1; slot >= 0; slot--) {
            ItemStack stored = inventory.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }

            ItemStack remaining = insertStack(chest, stored.copy());
            int movedCount = stored.getCount() - remaining.getCount();
            if (movedCount == 0) {
                continue;
            }

            inventory.removeItem(slot, movedCount);
            changed = true;
        }
        return changed;
    }

    private static ItemStack insertStack(Container chest, ItemStack incoming) {
        mergeIntoExistingStacks(chest, incoming);
        fillEmptySlots(chest, incoming);
        return incoming;
    }

    private static void mergeIntoExistingStacks(Container chest, ItemStack incoming) {
        for (int slot = 0; slot < chest.getContainerSize() && !incoming.isEmpty(); slot++) {
            ItemStack existing = chest.getItem(slot);
            if (existing.isEmpty() || !chest.canPlaceItem(slot, incoming) || !canMerge(existing, incoming)) {
                continue;
            }
            transferIntoStack(chest, existing, incoming);
        }
    }

    private static void transferIntoStack(Container chest, ItemStack existing, ItemStack incoming) {
        int maximumCount = Math.min(chest.getMaxStackSize(), existing.getMaxStackSize());
        int freeSpace = maximumCount - existing.getCount();
        int transferCount = Math.min(freeSpace, incoming.getCount());
        if (transferCount <= 0) {
            return;
        }

        existing.grow(transferCount);
        incoming.shrink(transferCount);
    }

    private static void fillEmptySlots(Container chest, ItemStack incoming) {
        for (int slot = 0; slot < chest.getContainerSize() && !incoming.isEmpty(); slot++) {
            if (!chest.getItem(slot).isEmpty() || !chest.canPlaceItem(slot, incoming)) {
                continue;
            }
            placeInEmptySlot(chest, slot, incoming);
        }
    }

    private static void placeInEmptySlot(Container chest, int slot, ItemStack incoming) {
        int maximumCount = Math.min(chest.getMaxStackSize(), incoming.getMaxStackSize());
        int transferCount = Math.min(maximumCount, incoming.getCount());
        ItemStack inserted = incoming.copy();
        inserted.setCount(transferCount);
        chest.setItem(slot, inserted);
        incoming.shrink(transferCount);
    }
}
