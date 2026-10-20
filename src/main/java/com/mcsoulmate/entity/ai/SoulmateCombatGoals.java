package com.mcsoulmate.entity.ai;

import com.mcsoulmate.entity.SoulmateEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;

public final class SoulmateCombatGoals {
    private static final double ARROW_TARGET_HEIGHT_FACTOR = 1.0D / 3.0D;
    private static final double ARROW_ARC_FACTOR = 0.2D;
    private static final int RANGED_ATTACK_INTERVAL_TICKS = 20;
    private static final float RANGED_ATTACK_RANGE = 32.0F;
    private static final float ARROW_VELOCITY = 1.6F;
    private static final float ARROW_INACCURACY = 1.0F;

    private SoulmateCombatGoals() {
    }

    public static Goal createBowAttackGoal(SoulmateEntity soulmate) {
        return new RangedAttackGoal(
                soulmate,
                SoulmateEntity.PLAYER_SPRINT_NAVIGATION_SPEED,
                RANGED_ATTACK_INTERVAL_TICKS,
                RANGED_ATTACK_RANGE
        ) {
            @Override
            public boolean canUse() {
                return isHoldingBow(soulmate) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isHoldingBow(soulmate) && super.canContinueToUse();
            }

            @Override
            public void tick() {
                if (soulmate.getTarget() != null && !soulmate.isUsingItem()) {
                    soulmate.startUsingItem(InteractionHand.MAIN_HAND);
                }
                super.tick();
            }

            @Override
            public void stop() {
                super.stop();
                soulmate.stopUsingItem();
            }
        };
    }

    public static RangedCrossbowAttackGoal<SoulmateEntity> createCrossbowAttackGoal(SoulmateEntity soulmate) {
        return new RangedCrossbowAttackGoal<>(
                soulmate,
                SoulmateEntity.PLAYER_SPRINT_NAVIGATION_SPEED,
                RANGED_ATTACK_RANGE
        );
    }

    public static Goal createMeleeAttackGoal(SoulmateEntity soulmate) {
        return new MeleeAttackGoal(soulmate, SoulmateEntity.PLAYER_SPRINT_NAVIGATION_SPEED, true) {
            @Override
            public boolean canUse() {
                return soulmate.isHoldingMeleeWeapon() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return soulmate.isHoldingMeleeWeapon() && super.canContinueToUse();
            }
        };
    }

    public static void performRangedAttack(SoulmateEntity soulmate, LivingEntity target, float pullProgress) {
        if (isHoldingCrossbow(soulmate)) {
            soulmate.performCrossbowAttack(soulmate, pullProgress);
            return;
        }
        if (!isHoldingBow(soulmate)) {
            return;
        }

        soulmate.stopUsingItem();
        AbstractArrow arrow = ((ArrowItem) Items.ARROW).createArrow(
                soulmate.level(),
                Items.ARROW.getDefaultInstance(),
                soulmate,
                soulmate.getMainHandItem()
        );
        double xDistance = target.getX() - soulmate.getX();
        double zDistance = target.getZ() - soulmate.getZ();
        double horizontalDistance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);
        double yDistance = target.getY(ARROW_TARGET_HEIGHT_FACTOR) - arrow.getY()
                + horizontalDistance * ARROW_ARC_FACTOR;
        arrow.shoot(xDistance, yDistance, zDistance, ARROW_VELOCITY * pullProgress, ARROW_INACCURACY);
        soulmate.level().addFreshEntity(arrow);
        soulmate.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
    }

    private static boolean isHoldingBow(SoulmateEntity soulmate) {
        return soulmate.getMainHandItem().getItem() instanceof BowItem;
    }

    private static boolean isHoldingCrossbow(SoulmateEntity soulmate) {
        return soulmate.getMainHandItem().getItem() instanceof CrossbowItem;
    }

}
