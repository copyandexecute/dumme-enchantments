package gg.norisk.enchantments

import gg.norisk.enchantments.EnchantmentRegistry.fastFalling
import gg.norisk.enchantments.impl.*
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*
import java.util.function.Predicate

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
        BalloonEnchantment.applyTargetDamage(world, entity, damageSource, itemStack)
    }

    fun Entity.sound(soundEvent: SoundEvent, volume: Number = 1f, pitch: Number = 1f) {
        world.playSoundFromEntity(null, this, soundEvent, this.soundCategory, volume.toFloat(), pitch.toFloat())
    }

    val Vec3d.blockPos get() = BlockPos(x.toInt(), y.toInt(), z.toInt())

    fun raycastEntity(entity: Entity, i: Int): Optional<Entity> {
        return run {
            val vec3d = entity.eyePos
            val vec3d2 = entity.getRotationVec(1.0f).multiply(i.toDouble())
            val vec3d3 = vec3d.add(vec3d2)
            val box = entity.boundingBox.stretch(vec3d2).expand(1.0)
            val j = i * i
            val predicate =
                Predicate { entityx: Entity -> !entityx.isSpectator && entityx.canHit() }
            val entityHitResult = ProjectileUtil.raycast(entity, vec3d, vec3d3, box, predicate, j.toDouble())
            if (entityHitResult == null) {
                Optional.empty()
            } else {
                if (vec3d.squaredDistanceTo(entityHitResult.pos) > j.toDouble()) Optional.empty() else Optional.of(
                    entityHitResult.entity
                )
            }
        }
    }

    fun raycastThickEntity(entity: Entity, i: Int, thickness: Double): Optional<Entity> {
        val vec3d = entity.eyePos
        val vec3d2 = entity.getRotationVec(1.0f).multiply(i.toDouble())
        val vec3d3 = vec3d.add(vec3d2)

        // Basis-Bounding Box um den Ray erweitern
        val box = entity.boundingBox.stretch(vec3d2).expand(thickness)

        val predicate = Predicate { entityx: Entity -> !entityx.isSpectator && entityx.canHit() }
        val j = i * i

        // Liste der möglichen Treffer für parallele Raycasts
        var closestEntity: Entity? = null
        var closestDistance = Double.MAX_VALUE

        // Führe mehrere Raycasts in einem kleinen Gitter um den Strahl herum aus
        for (dx in -1..1) {
            for (dz in -1..1) {
                val offsetVec = vec3d2.add(dx * thickness, 0.0, dz * thickness)
                val entityHitResult = ProjectileUtil.raycast(entity, vec3d, vec3d.add(offsetVec), box, predicate, j.toDouble())

                if (entityHitResult != null) {
                    val distance = vec3d.squaredDistanceTo(entityHitResult.pos)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestEntity = entityHitResult.entity
                    }
                }
            }
        }

        return if (closestEntity != null) Optional.of(closestEntity) else Optional.empty()
    }

    fun applyProjectileSpawned(
        serverWorld: ServerWorld,
        itemStack: ItemStack,
        persistentProjectileEntity: PersistentProjectileEntity
    ) {
        AimBotEnchantment.applyProjectileSpawned(serverWorld, itemStack, persistentProjectileEntity)
    }
}
