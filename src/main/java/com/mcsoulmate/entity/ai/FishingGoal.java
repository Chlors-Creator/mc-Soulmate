package com.mcsoulmate.entity.ai;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class FishingGoal extends Goal {
    private static final long SEARCH_INTERVAL_TICKS = 40L;
    private static final double INTERACTION_DISTANCE_SQUARED = 4.0D;
    private static final double SHORE_TARGET_OFFSET = 0.5D;
    private static final double CATCH_CAPTURE_RADIUS = 2.0D;
    private static final int NO_FISHING_HOOK = -1;
    private static final float YAW_DIRECTION_OFFSET_DEGREES = 90.0F;
    private static final String FAKE_PLAYER_NAME = "Soulmate";

    private final SoulmateEntity soulmate;
    private FishingSpotSearch.FishingSpot fishingSpot;
    private FakePlayer fakePlayer;
    private long nextSearchTick;

    public FishingGoal(SoulmateEntity soulmate) {
        this.soulmate = soulmate;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!canFish(this.soulmate) || !(this.soulmate.level() instanceof ServerLevel level)) {
            return false;
        }
        long gameTime = level.getGameTime();
        if (gameTime < this.nextSearchTick) {
            return false;
        }
        this.nextSearchTick = gameTime + SEARCH_INTERVAL_TICKS;
        Optional<FishingSpotSearch.FishingSpot> spot = FishingSpotSearch.findNearest(level, this.soulmate);
        this.fishingSpot = spot.orElse(null);
        return this.fishingSpot != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canFish(this.soulmate)
                && this.fishingSpot != null
                && this.fakePlayer != null
                && (this.isAtShore() || !this.soulmate.getNavigation().isDone());
    }

    @Override
    public void start() {
        if (!(this.soulmate.level() instanceof ServerLevel level)) {
            return;
        }
        GameProfile profile = new GameProfile(this.soulmate.getUUID(), FAKE_PLAYER_NAME);
        this.fakePlayer = FakePlayerFactory.get(level, profile);
        this.soulmate.setWorkActivity(SoulmateEntity.WorkActivity.FISHING);
        this.navigateToShore();
    }

    @Override
    public void tick() {
        if (!(this.soulmate.level() instanceof ServerLevel level) || this.fishingSpot == null) {
            return;
        }
        if (!this.isAtShore()) {
            this.retryPathIfNeeded();
            return;
        }
        this.soulmate.getNavigation().stop();
        this.faceWater();
        this.syncFakePlayer();
        this.manageFishingRod(level);
    }

    @Override
    public void stop() {
        this.soulmate.getNavigation().stop();
        this.soulmate.clearWorkActivity(SoulmateEntity.WorkActivity.FISHING);
        this.discardFishingHook();
        this.fakePlayer = null;
        this.fishingSpot = null;
    }

    private static boolean canFish(SoulmateEntity soulmate) {
        return soulmate.isSafeForFarming()
                && !soulmate.hasFishingStorageBatchReady()
                && soulmate.getMainHandItem().getItem() instanceof FishingRodItem;
    }

    private void manageFishingRod(ServerLevel level) {
        FishingHook hook = this.fakePlayer.fishing;
        if (hook == null || hook.isRemoved()) {
            this.fakePlayer.fishing = null;
            this.castFishingRod(level);
            return;
        }
        this.soulmate.setFishingHookId(hook.getId());
        if (hook.getEntityData().get(FishingHook.DATA_BITING)) {
            this.reelFishingRod(level, hook);
        }
    }

    private void castFishingRod(ServerLevel level) {
        ItemStack rodStack = this.soulmate.getMainHandItem();
        if (!(rodStack.getItem() instanceof FishingRodItem fishingRod)) {
            return;
        }
        this.fakePlayer.getInventory().setSelectedItem(rodStack);
        fishingRod.use(level, this.fakePlayer, InteractionHand.MAIN_HAND);
        FishingHook hook = this.fakePlayer.fishing;
        if (hook == null) {
            return;
        }
        this.soulmate.setFishingHookId(hook.getId());
        this.soulmate.swing(InteractionHand.MAIN_HAND);
    }

    private void reelFishingRod(ServerLevel level, FishingHook hook) {
        ItemStack rodStack = this.soulmate.getMainHandItem();
        if (!(rodStack.getItem() instanceof FishingRodItem fishingRod)) {
            return;
        }
        AABB catchArea = hook.getBoundingBox().inflate(CATCH_CAPTURE_RADIUS);
        Set<UUID> previousItems = this.findExistingItemIds(level, catchArea);
        this.fakePlayer.getInventory().setSelectedItem(rodStack);
        fishingRod.use(level, this.fakePlayer, InteractionHand.MAIN_HAND);
        this.soulmate.setItemSlot(EquipmentSlot.MAINHAND, this.fakePlayer.getMainHandItem());
        this.storeNewFishingDrops(level, catchArea, previousItems);
        this.fakePlayer.fishing = null;
        this.soulmate.setFishingHookId(NO_FISHING_HOOK);
        this.soulmate.swing(InteractionHand.MAIN_HAND);
    }

    private Set<UUID> findExistingItemIds(ServerLevel level, AABB area) {
        Set<UUID> itemIds = new HashSet<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            itemIds.add(item.getUUID());
        }
        return itemIds;
    }

    private void storeNewFishingDrops(ServerLevel level, AABB area, Set<UUID> previousItems) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (previousItems.contains(item.getUUID())) {
                continue;
            }
            this.soulmate.getFishingInventory().addItem(item.getItem());
            item.discard();
        }
    }

    private void syncFakePlayer() {
        this.fakePlayer.setPos(this.soulmate.getX(), this.soulmate.getY(), this.soulmate.getZ());
        this.fakePlayer.setYRot(this.soulmate.getYHeadRot());
        this.fakePlayer.setYHeadRot(this.soulmate.getYHeadRot());
        this.fakePlayer.setXRot(this.soulmate.getXRot());
        this.fakePlayer.setYHeadRot(this.soulmate.getYHeadRot());
        this.fakePlayer.getInventory().setSelectedItem(this.soulmate.getMainHandItem());
    }

    private void faceWater() {
        BlockPos water = this.fishingSpot.waterPosition();
        double xDistance = water.getX() + SHORE_TARGET_OFFSET - this.soulmate.getX();
        double yDistance = water.getY() + SHORE_TARGET_OFFSET - this.soulmate.getEyeY();
        double zDistance = water.getZ() + SHORE_TARGET_OFFSET - this.soulmate.getZ();
        double horizontalDistance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);
        float yaw = (float) Math.toDegrees(Math.atan2(zDistance, xDistance)) - YAW_DIRECTION_OFFSET_DEGREES;
        float pitch = (float) -Math.toDegrees(Math.atan2(yDistance, horizontalDistance));
        this.soulmate.setYRot(yaw);
        this.soulmate.setYHeadRot(yaw);
        this.soulmate.setYBodyRot(yaw);
        this.soulmate.setXRot(pitch);
    }

    private void navigateToShore() {
        BlockPos shore = this.fishingSpot.shorePosition();
        this.soulmate.getNavigation().moveTo(
                shore.getX() + SHORE_TARGET_OFFSET,
                shore.getY(),
                shore.getZ() + SHORE_TARGET_OFFSET,
                SoulmateEntity.PLAYER_SPRINT_NAVIGATION_SPEED
        );
    }

    private void retryPathIfNeeded() {
        if (this.soulmate.getNavigation().isDone()) {
            this.navigateToShore();
        }
    }

    private boolean isAtShore() {
        BlockPos shore = this.fishingSpot.shorePosition();
        return this.soulmate.distanceToSqr(
                shore.getX() + SHORE_TARGET_OFFSET,
                shore.getY(),
                shore.getZ() + SHORE_TARGET_OFFSET
        ) <= INTERACTION_DISTANCE_SQUARED;
    }

    private void discardFishingHook() {
        if (this.fakePlayer == null) {
            return;
        }
        FishingHook hook = this.fakePlayer.fishing;
        if (hook != null && !hook.isRemoved()) {
            hook.discard();
        }
        this.fakePlayer.fishing = null;
        this.soulmate.setFishingHookId(NO_FISHING_HOOK);
    }
}
