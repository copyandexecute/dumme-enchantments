package gg.norisk.enchantments

import gg.norisk.enchantments.EnchantmentRegistry.fastFalling
import gg.norisk.enchantments.impl.SlotsEnchantment
import gg.norisk.enchantments.impl.SquishEnchantment
import gg.norisk.enchantments.impl.VerificationEnchantment
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
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

    fun RegistryKey<Enchantment>.getLevel(item: ItemStack?): Int? {
        if (item == null) return null
        val enchantments = EnchantmentHelper.getEnchantments(item)
        val entry = enchantments.enchantments.firstOrNull { it.matchesKey(this) } ?: return null
        return EnchantmentHelper.getLevel(entry, item)
    }

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        SquishEnchantment.applyTargetDamage(world, entity, damageSource, itemStack)
        SlotsEnchantment.applyTargetDamage(world, entity, damageSource, itemStack)
        VerificationEnchantment.applyTargetDamage(world, entity, damageSource, itemStack)
    }

    val Vec3d.blockPos get() = BlockPos(x.toInt(), y.toInt(), z.toInt())
}
