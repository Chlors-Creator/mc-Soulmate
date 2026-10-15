package com.mcsoulmate.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class SoulmateEntity extends PathfinderMob implements RangedAttackMob {
    private static final double PLAYER_SPRINT_BASE_SPEED = 0.13D;
    private static final double MAX_HEALTH = 20.0D;
    private static final double ATTACK_DAMAGE = 2.0D;
    private static final double FOLLOW_RANGE = 32.0D;
    private static final double GOAL_SPEED = 1.0D;
    private static final double ARROW_TARGET_HEIGHT_FACTOR = 1.0D / 3.0D;
    private static final double ARROW_ARC_FACTOR = 0.2D;
    private static final int RANGED_ATTACK_INTERVAL_TICKS = 20;
    private static final float RANGED_ATTACK_RANGE = 15.0F;
    private static final float ARROW_VELOCITY = 1.6F;
    private static final float ARROW_INACCURACY = 1.0F;
    private static final float PLAYER_LOOK_DISTANCE = 8.0F;
    private static final int FLOAT_GOAL_PRIORITY = 0;
    private static final int ATTACK_GOAL_PRIORITY = 2;
    private static final int WANDER_GOAL_PRIORITY = 5;
    private static final int LOOK_GOAL_PRIORITY = 6;
    private static final int RANDOM_LOOK_GOAL_PRIORITY = 7;
    private static final int TARGET_GOAL_PRIORITY = 1;

    public SoulmateEntity(EntityType<? extends SoulmateEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, PLAYER_SPRINT_BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(FLOAT_GOAL_PRIORITY, new FloatGoal(this));
        this.goalSelector.addGoal(ATTACK_GOAL_PRIORITY, createRangedAttackGoal());
        this.goalSelector.addGoal(ATTACK_GOAL_PRIORITY, createMeleeAttackGoal());
        this.goalSelector.addGoal(WANDER_GOAL_PRIORITY, new WaterAvoidingRandomStrollGoal(this, GOAL_SPEED));
        this.goalSelector.addGoal(LOOK_GOAL_PRIORITY, new LookAtPlayerGoal(this, Player.class, PLAYER_LOOK_DISTANCE));
        this.goalSelector.addGoal(RANDOM_LOOK_GOAL_PRIORITY, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(
                TARGET_GOAL_PRIORITY,
                new NearestAttackableTargetGoal<>(this, Monster.class, true)
        );
    }

    private RangedAttackGoal createRangedAttackGoal() {
        return new RangedAttackGoal(this, GOAL_SPEED, RANGED_ATTACK_INTERVAL_TICKS, RANGED_ATTACK_RANGE) {
            @Override
            public boolean canUse() {
                return SoulmateEntity.this.holdsBow() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SoulmateEntity.this.holdsBow() && super.canContinueToUse();
            }

            @Override
            public void tick() {
                if (SoulmateEntity.this.getTarget() != null && !SoulmateEntity.this.isUsingItem()) {
                    SoulmateEntity.this.startUsingItem(InteractionHand.MAIN_HAND);
                }
                super.tick();
            }

            @Override
            public void stop() {
                super.stop();
                SoulmateEntity.this.stopUsingItem();
            }
        };
    }

    private MeleeAttackGoal createMeleeAttackGoal() {
        return new MeleeAttackGoal(this, GOAL_SPEED, true) {
            @Override
            public boolean canUse() {
                return !SoulmateEntity.this.holdsBow() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !SoulmateEntity.this.holdsBow() && super.canContinueToUse();
            }
        };
    }

    private boolean holdsBow() {
        return this.getMainHandItem().getItem() instanceof BowItem;
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.holdsBow()) {
            return;
        }
        this.stopUsingItem();
        AbstractArrow arrow = ((ArrowItem) Items.ARROW).createArrow(
                this.level(),
                Items.ARROW.getDefaultInstance(),
                this,
                this.getMainHandItem()
        );
        double xDistance = target.getX() - this.getX();
        double zDistance = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);
        double yDistance = target.getY(ARROW_TARGET_HEIGHT_FACTOR) - arrow.getY()
                + horizontalDistance * ARROW_ARC_FACTOR;
        arrow.shoot(xDistance, yDistance, zDistance, ARROW_VELOCITY * pullProgress, ARROW_INACCURACY);
        this.level().addFreshEntity(arrow);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
    }
}