package com.mcsoulmate.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SoulmateEquipment {
    private static final double MELEE_RANGE_SQUARED = 400.0D;
    private static final long SWITCH_COOLDOWN_TICKS = 100L;
    private static final long COMBAT_EXIT_DELAY_TICKS = 600L;

    private ItemStack fishingRod = ItemStack.EMPTY;
    private ItemStack rangedWeapon = ItemStack.EMPTY;
    private ItemStack meleeWeapon = ItemStack.EMPTY;
    private long lastCombatGameTime = -1L;
    private long nextWeaponSwitchGameTime;

    public static WeaponType getWeaponType(ItemStack stack) {
        if (stack.getItem() instanceof FishingRodItem) {
            return WeaponType.FISHING_ROD;
        }
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
            return WeaponType.RANGED;
        }
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
            return WeaponType.MELEE;
        }
        return null;
    }

    public static boolean isSupportedItem(SoulmateEntity soulmate, ItemStack stack) {
        EquipmentSlot slot = soulmate.getEquipmentSlotForItem(stack);
        return getWeaponType(stack) != null
                || slot.isArmor()
                || slot == EquipmentSlot.OFFHAND && stack.is(Items.SHIELD);
    }

    public boolean shouldPickUp(SoulmateEntity soulmate, ItemStack stack) {
        if (!isSupportedItem(soulmate, stack)) {
            return false;
        }
        WeaponType type = getWeaponType(stack);
        if (type == null) {
            EquipmentSlot slot = soulmate.getEquipmentSlotForItem(stack);
            return SoulmateEquipmentQuality.isBetter(stack, soulmate.getItemBySlot(slot), slot);
        }
        ItemStack incumbent = getWeaponType(soulmate.getMainHandItem()) == type
                ? soulmate.getMainHandItem()
                : this.getStored(type);
        return SoulmateEquipmentQuality.isBetter(stack, incumbent, EquipmentSlot.MAINHAND);
    }

    public ItemStack equipOfferedItem(SoulmateEntity soulmate, ServerLevel level, ItemStack offered) {
        WeaponType type = getWeaponType(offered);
        if (type != null) {
            return this.equipWeapon(soulmate, level, type, offered);
        }
        EquipmentSlot slot = soulmate.getEquipmentSlotForItem(offered);
        if (!slot.isArmor() && !(slot == EquipmentSlot.OFFHAND && offered.is(Items.SHIELD))) {
            return ItemStack.EMPTY;
        }
        return this.equipArmor(soulmate, level, slot, offered);
    }

    public boolean isInCombatMode(SoulmateEntity soulmate) {
        LivingEntity target = soulmate.getTarget();
        return target != null && target.isAlive()
                || this.lastCombatGameTime >= 0L
                && soulmate.level().getGameTime() - this.lastCombatGameTime < COMBAT_EXIT_DELAY_TICKS;
    }

    public void updateActiveWeapon(SoulmateEntity soulmate) {
        if (soulmate.level().isClientSide()) {
            return;
        }
        long gameTime = soulmate.level().getGameTime();
        LivingEntity target = soulmate.getTarget();
        if (target != null && target.isAlive()) {
            this.lastCombatGameTime = gameTime;
            this.updateCombatWeapon(soulmate, target, gameTime);
            return;
        }
        if (!this.isInCombatMode(soulmate)) {
            this.switchTo(soulmate, WeaponType.FISHING_ROD, gameTime);
        }
    }

    public void dropStoredWeapons(SoulmateEntity soulmate, ServerLevel level) {
        for (ItemStack stack : this.takeAll()) {
            this.dropItem(soulmate, level, stack);
        }
    }

    public void save(ValueOutput output) {
        this.saveStack(output, "StoredFishingRod", this.fishingRod);
        this.saveStack(output, "StoredRangedWeapon", this.rangedWeapon);
        this.saveStack(output, "StoredMeleeWeapon", this.meleeWeapon);
        output.putLong("LastCombatGameTime", this.lastCombatGameTime);
        output.putLong("NextWeaponSwitchGameTime", this.nextWeaponSwitchGameTime);
    }

    public void load(ValueInput input) {
        this.fishingRod = this.loadStack(input, "StoredFishingRod");
        this.rangedWeapon = this.loadStack(input, "StoredRangedWeapon");
        this.meleeWeapon = this.loadStack(input, "StoredMeleeWeapon");
        this.lastCombatGameTime = input.getLongOr("LastCombatGameTime", -1L);
        this.nextWeaponSwitchGameTime = input.getLongOr("NextWeaponSwitchGameTime", 0L);
    }

    private ItemStack equipWeapon(
            SoulmateEntity soulmate,
            ServerLevel level,
            WeaponType type,
            ItemStack offered
    ) {
        ItemStack newWeapon = offered.copyWithCount(1);
        WeaponType currentType = getWeaponType(soulmate.getMainHandItem());
        ItemStack incumbent = currentType == type ? soulmate.getMainHandItem() : this.getStored(type);
        if (!SoulmateEquipmentQuality.isBetter(newWeapon, incumbent, EquipmentSlot.MAINHAND)) {
            return ItemStack.EMPTY;
        }
        if (currentType == null || currentType == type) {
            this.dropStored(soulmate, level, type);
            this.dropItem(soulmate, level, soulmate.getMainHandItem().copy());
            soulmate.setItemSlot(EquipmentSlot.MAINHAND, newWeapon);
        } else {
            this.dropItem(soulmate, level, this.replace(type, newWeapon));
        }
        soulmate.setPersistenceRequired();
        return newWeapon;
    }

    private ItemStack equipArmor(
            SoulmateEntity soulmate,
            ServerLevel level,
            EquipmentSlot slot,
            ItemStack offered
    ) {
        ItemStack previous = soulmate.getItemBySlot(slot);
        ItemStack equipped = offered.copyWithCount(1);
        if (!SoulmateEquipmentQuality.isBetter(equipped, previous, slot)) {
            return ItemStack.EMPTY;
        }
        this.dropItem(soulmate, level, previous.copy());
        soulmate.setItemSlot(slot, equipped);
        soulmate.setPersistenceRequired();
        return equipped;
    }

    private void updateCombatWeapon(SoulmateEntity soulmate, LivingEntity target, long gameTime) {
        WeaponType preferred = soulmate.distanceToSqr(target) > MELEE_RANGE_SQUARED
                ? WeaponType.RANGED
                : WeaponType.MELEE;
        WeaponType alternative = preferred == WeaponType.RANGED ? WeaponType.MELEE : WeaponType.RANGED;
        if (this.hasHeldOrStored(soulmate, preferred)) {
            this.switchTo(soulmate, preferred, gameTime);
        } else if (this.hasHeldOrStored(soulmate, alternative)) {
            this.switchTo(soulmate, alternative, gameTime);
        }
    }

    private boolean hasHeldOrStored(SoulmateEntity soulmate, WeaponType type) {
        return getWeaponType(soulmate.getMainHandItem()) == type || !this.getStored(type).isEmpty();
    }

    private void switchTo(SoulmateEntity soulmate, WeaponType type, long gameTime) {
        if (getWeaponType(soulmate.getMainHandItem()) == type || gameTime < this.nextWeaponSwitchGameTime) {
            return;
        }
        ItemStack desired = this.take(type);
        if (desired.isEmpty()) {
            return;
        }
        WeaponType currentType = getWeaponType(soulmate.getMainHandItem());
        if (currentType != null) {
            this.dropItem(soulmate, (ServerLevel) soulmate.level(), this.replace(currentType, soulmate.getMainHandItem()));
        } else {
            this.dropItem(soulmate, (ServerLevel) soulmate.level(), soulmate.getMainHandItem().copy());
        }
        soulmate.setItemSlot(EquipmentSlot.MAINHAND, desired);
        this.nextWeaponSwitchGameTime = gameTime + SWITCH_COOLDOWN_TICKS;
    }

    private ItemStack replace(WeaponType type, ItemStack stack) {
        ItemStack previous = this.getStored(type);
        this.setStored(type, stack.copyWithCount(1));
        return previous;
    }

    private ItemStack take(WeaponType type) {
        ItemStack stack = this.getStored(type);
        this.setStored(type, ItemStack.EMPTY);
        return stack;
    }

    private void dropStored(SoulmateEntity soulmate, ServerLevel level, WeaponType type) {
        this.dropItem(soulmate, level, this.take(type));
    }

    private void dropItem(SoulmateEntity soulmate, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = soulmate.spawnAtLocation(level, stack);
        if (item != null) {
            item.setThrower(soulmate);
        }
    }

    private ItemStack getStored(WeaponType type) {
        return switch (type) {
            case FISHING_ROD -> this.fishingRod;
            case RANGED -> this.rangedWeapon;
            case MELEE -> this.meleeWeapon;
        };
    }

    private void setStored(WeaponType type, ItemStack stack) {
        switch (type) {
            case FISHING_ROD -> this.fishingRod = stack;
            case RANGED -> this.rangedWeapon = stack;
            case MELEE -> this.meleeWeapon = stack;
        }
    }

    private List<ItemStack> takeAll() {
        List<ItemStack> stacks = new ArrayList<>();
        for (WeaponType type : WeaponType.values()) {
            ItemStack stack = this.take(type);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private void saveStack(ValueOutput output, String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            output.store(key, ItemStack.CODEC, stack);
        }
    }

    private ItemStack loadStack(ValueInput input, String key) {
        return input.read(key, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    public enum WeaponType {
        FISHING_ROD,
        RANGED,
        MELEE
    }
}
