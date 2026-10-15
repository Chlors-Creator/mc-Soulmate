package com.mcsoulmate.entity.ai;

import java.util.List;

import com.mcsoulmate.entity.FarmInventory;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

final class FarmActions {
    private static final int FIRST_CROP_AGE = 0;
    private static final int ONE_SEED = 1;
    private static final int NO_INVENTORY_SLOT = -1;

    private FarmActions() {
    }

    static boolean isRipeCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    static boolean canPlantAt(ServerLevel level, BlockPos position, CropBlock crop) {
        if (!level.isEmptyBlock(position) || !level.getBlockState(position.below()).is(Blocks.FARMLAND)) {
            return false;
        }
        return crop.getStateForAge(FIRST_CROP_AGE).canSurvive(level, position);
    }

    static CropBlock findPlantableCrop(SoulmateEntity soulmate) {
        FarmInventory inventory = soulmate.getFarmingInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            CropBlock crop = cropForItem(inventory.getItem(slot));
            if (crop != null) {
                return crop;
            }
        }
        return null;
    }

    static void harvest(SoulmateEntity soulmate, ServerLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }

        List<ItemStack> drops = Block.getDrops(
                state,
                level,
                position,
                level.getBlockEntity(position),
                soulmate,
                ItemStack.EMPTY
        );
        BlockState nextState = getHarvestedState(crop, drops, soulmate);
        level.setBlock(position, nextState, Block.UPDATE_ALL);
        storeDrops(soulmate, drops);
    }

    static void plant(SoulmateEntity soulmate, ServerLevel level, BlockPos position) {
        FarmInventory inventory = soulmate.getFarmingInventory();
        int seedSlot = findPlantableSeedSlot(inventory);
        if (seedSlot < 0) {
            return;
        }

        CropBlock crop = cropForItem(inventory.getItem(seedSlot));
        if (crop == null || !canPlantAt(level, position, crop)) {
            return;
        }
        if (level.setBlock(position, crop.getStateForAge(FIRST_CROP_AGE), Block.UPDATE_ALL)) {
            inventory.removeItem(seedSlot, ONE_SEED);
        }
    }

    private static BlockState getHarvestedState(CropBlock crop, List<ItemStack> drops, SoulmateEntity soulmate) {
        if (consumeReplantSeed(crop, drops, soulmate)) {
            return crop.getStateForAge(FIRST_CROP_AGE);
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean consumeReplantSeed(CropBlock crop, List<ItemStack> drops, SoulmateEntity soulmate) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || cropForItem(drop) != crop) {
                continue;
            }
            drop.shrink(ONE_SEED);
            return true;
        }

        FarmInventory inventory = soulmate.getFarmingInventory();
        int seedSlot = findSeedSlot(inventory, crop);
        if (seedSlot < 0) {
            return false;
        }
        inventory.removeItem(seedSlot, ONE_SEED);
        return true;
    }

    private static int findSeedSlot(FarmInventory inventory, CropBlock crop) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && cropForItem(stack) == crop) {
                return slot;
            }
        }
        return NO_INVENTORY_SLOT;
    }

    private static int findPlantableSeedSlot(FarmInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (cropForItem(inventory.getItem(slot)) != null) {
                return slot;
            }
        }
        return NO_INVENTORY_SLOT;
    }

    private static CropBlock cropForItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Block block = Block.byItem(stack.getItem());
        return block instanceof CropBlock crop ? crop : null;
    }

    private static void storeDrops(SoulmateEntity soulmate, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                soulmate.getFarmingInventory().addItem(drop);
            }
        }
    }
}
