package com.mcsoulmate.entity;

import com.mcsoulmate.entity.ai.FarmingGoal;
import com.mcsoulmate.entity.ai.FishingGoal;
import com.mcsoulmate.entity.ai.SoulmateCombatGoals;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class SoulmateEntity extends TamableAnimal implements CrossbowAttackMob {
    private static final double PLAYER_WALK_MOVEMENT_SPEED = 0.1D;
    private static final double PLAYER_SPRINT_ATTRIBUTE_BONUS = 0.3D;
    private static final double PLAYER_SPRINT_MOVEMENT_SPEED =
            PLAYER_WALK_MOVEMENT_SPEED * (1.0D + PLAYER_SPRINT_ATTRIBUTE_BONUS);
    // Mob navigation applies speed as both forward input and movement speed, so compensate for the squared effect.
    public static final double PLAYER_SPRINT_NAVIGATION_SPEED =
            Math.sqrt(PLAYER_SPRINT_MOVEMENT_SPEED) / PLAYER_SPRINT_MOVEMENT_SPEED;
    private static final double MAX_HEALTH = 20.0D;
    private static final double ATTACK_DAMAGE = 2.0D;
    private static final double FOLLOW_RANGE = 32.0D;
    private static final float PLAYER_LOOK_DISTANCE = 8.0F;
    private static final float FOLLOW_OWNER_START_DISTANCE = 10.0F;
    private static final float FOLLOW_OWNER_STOP_DISTANCE = 2.0F;
    private static final int FLOAT_GOAL_PRIORITY = 0;
    private static final int ATTACK_GOAL_PRIORITY = 2;
    private static final int FARMING_GOAL_PRIORITY = 3;
    private static final int ONE_ITEM = 1;
    private static final int FOLLOW_OWNER_GOAL_PRIORITY = 4;
    private static final int FISHING_GOAL_PRIORITY = 5;
    private static final int WANDER_GOAL_PRIORITY = 6;
    private static final int LOOK_GOAL_PRIORITY = 7;
    private static final int RANDOM_LOOK_GOAL_PRIORITY = 8;
    private static final int OWNER_HURT_BY_TARGET_PRIORITY = 1;
    private static final int OWNER_HURT_TARGET_PRIORITY = 2;
    private static final int HURT_BY_TARGET_PRIORITY = 3;
    private static final int NO_FISHING_HOOK = -1;
    private static final int FISHES_PER_STORAGE_BATCH = 10;
    private static final EntityDataAccessor<Integer> FISHING_HOOK_ID =
            SynchedEntityData.defineId(SoulmateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW =
            SynchedEntityData.defineId(SoulmateEntity.class, EntityDataSerializers.BOOLEAN);

    private final FarmInventory farmingInventory = new FarmInventory();
    private final FarmInventory fishingInventory = new FarmInventory();

    public SoulmateEntity(EntityType<? extends SoulmateEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, PLAYER_WALK_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(FLOAT_GOAL_PRIORITY, new FloatGoal(this));
        this.goalSelector.addGoal(ATTACK_GOAL_PRIORITY, SoulmateCombatGoals.createBowAttackGoal(this));
        this.goalSelector.addGoal(ATTACK_GOAL_PRIORITY, SoulmateCombatGoals.createCrossbowAttackGoal(this));
        this.goalSelector.addGoal(ATTACK_GOAL_PRIORITY, SoulmateCombatGoals.createMeleeAttackGoal(this));
        this.goalSelector.addGoal(FARMING_GOAL_PRIORITY, new FarmingGoal(this));
        this.goalSelector.addGoal(FISHING_GOAL_PRIORITY, new FishingGoal(this));
        this.goalSelector.addGoal(
                FOLLOW_OWNER_GOAL_PRIORITY,
                new FollowOwnerGoal(
                        this,
                        PLAYER_SPRINT_NAVIGATION_SPEED,
                        FOLLOW_OWNER_START_DISTANCE,
                        FOLLOW_OWNER_STOP_DISTANCE
                )
        );
        this.goalSelector.addGoal(
                WANDER_GOAL_PRIORITY,
                new WaterAvoidingRandomStrollGoal(this, PLAYER_SPRINT_NAVIGATION_SPEED)
        );
        this.goalSelector.addGoal(LOOK_GOAL_PRIORITY, new LookAtPlayerGoal(this, Player.class, PLAYER_LOOK_DISTANCE));
        this.goalSelector.addGoal(RANDOM_LOOK_GOAL_PRIORITY, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(
                OWNER_HURT_BY_TARGET_PRIORITY,
                new OwnerHurtByTargetGoal(this)
        );
        this.targetSelector.addGoal(OWNER_HURT_TARGET_PRIORITY, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(HURT_BY_TARGET_PRIORITY, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FISHING_HOOK_ID, NO_FISHING_HOOK);
        builder.define(IS_CHARGING_CROSSBOW, false);
    }

    @Override
    public void travel(Vec3 input) {
        this.updateSprintingForNavigation();
        super.travel(input);
    }

    private void updateSprintingForNavigation() {
        if (this.level().isClientSide()) {
            return;
        }

        boolean navigating = this.getNavigation().isInProgress();
        if (this.isSprinting() != navigating) {
            this.setSprinting(navigating);
        }
    }

    public FarmInventory getFarmingInventory() {
        return this.farmingInventory;
    }

    public FarmInventory getFishingInventory() {
        return this.fishingInventory;
    }

    public boolean hasFishingStorageBatchReady() {
        return this.fishingInventory.countItems(ItemTags.FISHES) >= FISHES_PER_STORAGE_BATCH;
    }

    public int getFishingHookId() {
        return this.entityData.get(FISHING_HOOK_ID);
    }

    public boolean hasFishingHook() {
        return this.getFishingHookId() != NO_FISHING_HOOK;
    }

    public void setFishingHookId(int hookId) {
        this.entityData.set(FISHING_HOOK_ID, hookId);
    }

    public boolean isSafeForFarming() {
        return !this.isDeadOrDying() && this.getTarget() == null && !this.isPassenger();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("FarmingInventory", ItemStack.CODEC.listOf(), this.farmingInventory.copyItems());
        output.store("FishingInventory", ItemStack.CODEC.listOf(), this.fishingInventory.copyItems());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.farmingInventory.replaceItems(
                input.read("FarmingInventory", ItemStack.CODEC.listOf()).orElseGet(java.util.List::of)
        );
        this.fishingInventory.replaceItems(
                input.read("FishingInventory", ItemStack.CODEC.listOf()).orElseGet(java.util.List::of)
        );
        for (ItemStack fish : this.farmingInventory.removeItems(ItemTags.FISHES)) {
            this.fishingInventory.addItem(fish);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (this.isTame() && this.isPlantingItem(heldItem)) {
            return this.storePlantingItem(player, heldItem);
        }
        if (this.isTame() || !heldItem.is(Items.DIAMOND)) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide()) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(ONE_ITEM);
            }
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, this);
            }
        }
        return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private boolean isPlantingItem(ItemStack itemStack) {
        return !itemStack.isEmpty() && Block.byItem(itemStack.getItem()) instanceof CropBlock;
    }

    private InteractionResult storePlantingItem(Player player, ItemStack heldItem) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack plantingItem = heldItem.copy();
        plantingItem.setCount(ONE_ITEM);
        this.farmingInventory.addItem(plantingItem);
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(ONE_ITEM);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack itemStack) {
        if (!isPlayerOfferedEquipment(itemStack)) {
            return super.equipItemIfPossible(level, itemStack);
        }

        ItemStack oldItem = this.getMainHandItem();
        if (!oldItem.isEmpty()) {
            this.spawnAtLocation(level, oldItem.copy());
        }
        ItemStack equippedItem = itemStack.copyWithCount(ONE_ITEM);
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND, equippedItem);
        this.setPersistenceRequired();
        return equippedItem;
    }

    private static boolean isPlayerOfferedEquipment(ItemStack itemStack) {
        return itemStack.getItem() instanceof BowItem
                || itemStack.getItem() instanceof CrossbowItem
                || itemStack.getItem() instanceof FishingRodItem;
    }

    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        return weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem
                ? Items.ARROW.getDefaultInstance() : super.getProjectile(weapon);
    }

    @Override
    public void setChargingCrossbow(boolean isCharging) {
        this.entityData.set(IS_CHARGING_CROSSBOW, isCharging);
    }

    public boolean isChargingCrossbow() {
        return this.entityData.get(IS_CHARGING_CROSSBOW);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.setChargingCrossbow(false);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        SoulmateCombatGoals.performRangedAttack(this, target, pullProgress);
    }
}
