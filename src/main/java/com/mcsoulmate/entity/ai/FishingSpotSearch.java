package com.mcsoulmate.entity.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

final class FishingSpotSearch {
    private static final int SEARCH_HORIZONTAL_RADIUS = 12;
    private static final int SEARCH_VERTICAL_RADIUS = 4;
    private static final double BLOCK_CENTER_OFFSET = 0.5D;
    private static final List<Direction> SHORE_DIRECTIONS =
            List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private FishingSpotSearch() {
    }

    static Optional<FishingSpot> findNearest(ServerLevel level, SoulmateEntity soulmate) {
        BlockPos center = soulmate.blockPosition();
        BlockPos minimum = center.offset(
                -SEARCH_HORIZONTAL_RADIUS,
                -SEARCH_VERTICAL_RADIUS,
                -SEARCH_HORIZONTAL_RADIUS
        );
        BlockPos maximum = center.offset(
                SEARCH_HORIZONTAL_RADIUS,
                SEARCH_VERTICAL_RADIUS,
                SEARCH_HORIZONTAL_RADIUS
        );
        return BlockPos.betweenClosedStream(minimum, maximum)
                .map(BlockPos::immutable)
                .filter(level::hasChunkAt)
                .filter(position -> isWaterSource(level, position))
                .flatMap(water -> nearbyShorePositions(level, water))
                .min(Comparator.comparingDouble(spot -> distanceSquared(soulmate, spot.shorePosition())));
    }

    private static boolean isWaterSource(ServerLevel level, BlockPos position) {
        return level.getFluidState(position).is(FluidTags.WATER)
                && level.getFluidState(position).isSource();
    }

    private static Stream<FishingSpot> nearbyShorePositions(ServerLevel level, BlockPos waterPosition) {
        return SHORE_DIRECTIONS.stream()
                .map(waterPosition::relative)
                .filter(shore -> isStandable(level, shore))
                .map(shore -> new FishingSpot(waterPosition, shore));
    }

    private static boolean isStandable(ServerLevel level, BlockPos position) {
        if (!hasEmptyCollision(level, position) || !hasEmptyCollision(level, position.above())) {
            return false;
        }
        BlockPos floorPosition = position.below();
        BlockState floor = level.getBlockState(floorPosition);
        return floor.isFaceSturdy(level, floorPosition, Direction.UP);
    }

    private static boolean hasEmptyCollision(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).getCollisionShape(level, position).isEmpty();
    }

    private static double distanceSquared(SoulmateEntity soulmate, BlockPos position) {
        return soulmate.distanceToSqr(
                position.getX() + BLOCK_CENTER_OFFSET,
                position.getY(),
                position.getZ() + BLOCK_CENTER_OFFSET
        );
    }

    record FishingSpot(BlockPos waterPosition, BlockPos shorePosition) {
    }
}
