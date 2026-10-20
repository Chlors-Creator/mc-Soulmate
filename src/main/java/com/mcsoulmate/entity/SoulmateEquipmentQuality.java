package com.mcsoulmate.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class SoulmateEquipmentQuality {
    private SoulmateEquipmentQuality() {
    }

    public static boolean isBetter(ItemStack candidate, ItemStack incumbent, EquipmentSlot slot) {
        return incumbent.isEmpty() || quality(candidate, slot).compareTo(quality(incumbent, slot)) > 0;
    }

    private static Quality quality(ItemStack stack, EquipmentSlot slot) {
        boolean defenseSlot = slot.isArmor() || slot == EquipmentSlot.OFFHAND;
        Holder<Attribute> primary = defenseSlot ? Attributes.ARMOR : Attributes.ATTACK_DAMAGE;
        Holder<Attribute> secondary = defenseSlot ? Attributes.ARMOR_TOUGHNESS : Attributes.ATTACK_SPEED;
        double tertiary = defenseSlot
                ? attributeValue(stack, slot, Attributes.KNOCKBACK_RESISTANCE)
                : 0.0D;
        int maxDurability = stack.getMaxDamage();
        int remainingDurability = maxDurability - stack.getDamageValue();
        int enchantmentLevel = EnchantmentHelper.getEnchantmentsForCrafting(stack)
                .entrySet()
                .stream()
                .mapToInt(entry -> entry.getIntValue())
                .sum();
        return new Quality(
                attributeValue(stack, slot, primary),
                attributeValue(stack, slot, secondary),
                tertiary,
                enchantmentLevel,
                maxDurability,
                remainingDurability
        );
    }

    private static double attributeValue(
            ItemStack stack,
            EquipmentSlot slot,
            Holder<Attribute> wantedAttribute
    ) {
        double[] total = {0.0D};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(wantedAttribute)) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private record Quality(
            double primary,
            double secondary,
            double tertiary,
            int enchantmentLevel,
            int maxDurability,
            int remainingDurability
    ) implements Comparable<Quality> {
        @Override
        public int compareTo(Quality other) {
            int comparison = Double.compare(this.primary, other.primary);
            if (comparison != 0) return comparison;
            comparison = Double.compare(this.secondary, other.secondary);
            if (comparison != 0) return comparison;
            comparison = Double.compare(this.tertiary, other.tertiary);
            if (comparison != 0) return comparison;
            comparison = Integer.compare(this.enchantmentLevel, other.enchantmentLevel);
            if (comparison != 0) return comparison;
            comparison = Integer.compare(this.maxDurability, other.maxDurability);
            return comparison != 0 ? comparison : Integer.compare(this.remainingDurability, other.remainingDurability);
        }
    }
}
