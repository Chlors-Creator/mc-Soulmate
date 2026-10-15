package com.mcsoulmate.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FarmingGoal extends Goal {
    private static final int FARM_SEARCH_HORIZONTAL_RADIUS = 8;
    private static final int FARM_SEARCH_VERTICAL_RADIUS = 3;
    private static final int WORK_SEARCH_INTERVAL_TICKS = 40;
    private static final double FARMING_MOVEMENT_SPEED = 1.0D;
    private static final double INTERACTION_DISTANCE_SQUARED = 4.0D;
    private static final double ITEM_DROP_OFFSET = 0.5D;

    private final SoulmateEntity soulmate;
    private BlockPos workPosition;
    private WorkType workType;
    private long nextWorkSearchTick;

    public FarmingGoal(SoulmateEntity soulmate) {
        this.soulmate = soulmate;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.soulmate.isSafeForFarming() || !(this.soulmate.level() instanceof ServerLevel level)) {
            return false;
        }

        long gameTime = level.getGameTime();
        if (gameTime < this.nextWorkSearchTick) {
            return false;
        }
        this.nextWorkSearchTick = gameTime + WORK_SEARCH_INTERVAL_TICKS;
        return this.selectWork(level);
    }

    @Override
    public boolean canContinueToUse() {
        return this.workPosition != null && this.soulmate.isSafeForFarming();
    }

    @Override
    public void start() {
        if (this.workPosition != null) {
            this.navigateToWorkPosition();
        }
    }

    @Override
    public void tick() {
        if (this.workPosition == null) {
            return;
        }
        if (!this.isWithinInteractionRange()) {
            this.retryPathIfNeeded();
            return;
        }

        this.performWork();
        this.clearWorkPosition();
    }

    @Override
    public void stop() {
        this.soulmate.getNavigation().stop();
        this.clearWorkPosition();
    }

    private boolean selectWork(ServerLevel level) {
        Optional<BlockPos> ripeCrop = this.findNearestRipeCrop(level);
        if (ripeCrop.isPresent()) {
            return this.assignWork(WorkType.HARVEST, ripeCrop.get());
        }

        Optional<BlockPos> emptyFarmland = this.findNearestPlantableFarmland(level);
        if (emptyFarmland.isPresent()) {
            return this.assignWork(WorkType.PLANT, emptyFarmland.get());
        }

        if (!this.soulmate.getFarmingInventory().isEmpty()) {
            Optional<BlockPos> chest = FarmStorage.findNearbyChest(this.soulmate);
            return chest.isPresent() && this.assignWork(WorkType.DEPOSIT, chest.get());
        }
        return false;
    }

    private boolean assignWork(WorkType type, BlockPos position) {
        this.workType = type;
        this.workPosition = position;
        return true;
    }

    private Optional<BlockPos> findNearestRipeCrop(ServerLevel level) {
        return this.farmPositions()
                .filter(level::hasChunkAt)
                .filter(position -> this.isRipeCrop(level.getBlockState(position)))
                .min(this.distanceFromSoulmate());
    }

    private boolean isRipeCrop(BlockState state) {
        return FarmActions.isRipeCrop(state);
    }

    private Optional<BlockPos> findNearestPlantableFarmland(ServerLevel level) {
        CropBlock crop = FarmActions.findPlantableCrop(this.soulmate);
        if (crop == null) {
            return Optional.empty();
        }

        return this.farmPositions()
                .filter(level::hasChunkAt)
                .filter(position -> FarmActions.canPlantAt(level, position, crop))
                .min(this.distanceFromSoulmate());
    }

    private Stream<BlockPos> farmPositions() {
        BlockPos center = this.soulmate.blockPosition();
        BlockPos minimum = center.offset(
                -FARM_SEARCH_HORIZONTAL_RADIUS,
                -FARM_SEARCH_VERTICAL_RADIUS,
                -FARM_SEARCH_HORIZONTAL_RADIUS
        );
        BlockPos maximum = center.offset(
                FARM_SEARCH_HORIZONTAL_RADIUS,
                FARM_SEARCH_VERTICAL_RADIUS,
                FARM_SEARCH_HORIZONTAL_RADIUS
        );
        return BlockPos.betweenClosedStream(minimum, maximum).map(BlockPos::immutable);
    }

    private Comparator<BlockPos> distanceFromSoulmate() {
        return Comparator.comparingDouble(position -> this.soulmate.distanceToSqr(
                position.getX() + ITEM_DROP_OFFSET,
                position.getY() + ITEM_DROP_OFFSET,
                position.getZ() + ITEM_DROP_OFFSET
        ));
    }

    private void navigateToWorkPosition() {
        this.soulmate.getNavigation().moveTo(
                this.workPosition.getX() + ITEM_DROP_OFFSET,
                this.workPosition.getY() + ITEM_DROP_OFFSET,
                this.workPosition.getZ() + ITEM_DROP_OFFSET,
                FARMING_MOVEMENT_SPEED
        );
    }

    private boolean isWithinInteractionRange() {
        return this.soulmate.distanceToSqr(
                this.workPosition.getX() + ITEM_DROP_OFFSET,
                this.workPosition.getY() + ITEM_DROP_OFFSET,
                this.workPosition.getZ() + ITEM_DROP_OFFSET
        ) <= INTERACTION_DISTANCE_SQUARED;
    }

    private void retryPathIfNeeded() {
        if (this.soulmate.getNavigation().isDone()) {
            this.clearWorkPosition();
        }
    }

    private void performWork() {
        if (!(this.soulmate.level() instanceof ServerLevel level)) {
            return;
        }
        switch (this.workType) {
            case DEPOSIT -> FarmStorage.depositInventory(this.soulmate, this.workPosition);
            case HARVEST -> FarmActions.harvest(this.soulmate, level, this.workPosition);
            case PLANT -> FarmActions.plant(this.soulmate, level, this.workPosition);
        }
    }

    private void clearWorkPosition() {
        this.workPosition = null;
        this.workType = null;
    }

    private enum WorkType {
        DEPOSIT,
        HARVEST,
        PLANT
    }
}
