package gg.norisk.enchantments

import gg.norisk.enchantments.EnchantmentRegistry.fastFalling
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object EnchantmentUtils {
    fun LivingEntity.applyGravity(callback: CallbackInfoReturnable<Double>) {
        val level = fastFalling.getLevel(this.getEquippedStack(EquipmentSlot.FEET)) ?: return
        val gravity = callback.returnValue
        callback.returnValue = gravity * (level * 2)
    }

    fun LivingEntity.applyFallSound(strength: Int, callback: CallbackInfoReturnable<SoundEvent>) {
        if (fastFalling.getLevel(this.getEquippedStack(EquipmentSlot.FEET)) != null) {
            callback.returnValue = SoundEvents.BLOCK_ANVIL_LAND
        }
    }

    fun RegistryKey<Enchantment>.getLevel(item: ItemStack): Int? {
        val enchantments = EnchantmentHelper.getEnchantments(item)
        val entry = enchantments.enchantments.firstOrNull { it.matchesKey(this) } ?: return null
        return EnchantmentHelper.getLevel(entry, item)
    }
}
