package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.enchantments.EnchantmentRegistry.ram
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.TargetPredicate
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.core.entity.directionVector
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.text.literal
import kotlin.random.Random

object RamEnchantment {
    fun initServer() {
        ServerTickEvents.END_WORLD_TICK.register {
            for (player in it.players) {
                if (player.isStupidRamming) {
                    val ram = ram.getLevel(player.activeItem)
                    if (ram != null) {
                        val attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) ?: continue
                        attribute.baseValue = (attribute.baseValue + 0.0005).coerceIn(
                            0.005,
                            0.5
                        )

                        player.ramOthers(ram)
                    } else {
                        player.isStupidRamming = false
                        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1
                    }
                }
            }
        }
        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            if (!player.world.isClient) {
                val ram = ram.getLevel(player.getStackInHand(hand)) ?: return@UseItemCallback TypedActionResult.pass(
                    ItemStack.EMPTY
                )
                player.isStupidRamming = true
            }
            return@UseItemCallback TypedActionResult.pass(ItemStack.EMPTY)
        })
    }

    var PlayerEntity.isStupidRamming: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:isStupidRamming") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:isStupidRamming", value)
        }

    private fun PlayerEntity.ramOthers(level: Int) {
        val serverWorld = this.world as? ServerWorld? ?: return

        // Finde alle attackierbaren Lebewesen in der Umgebung
        val list: List<LivingEntity> = serverWorld.getTargets(
            LivingEntity::class.java, TargetPredicate.createAttackable().setPredicate {
                it.y <= this.y
            }, this, this.boundingBox.expand(1.1)
        )

        // Iteriere über die gefundenen Lebewesen
        for (livingEntity in list) {
            val damageSource: DamageSource = serverWorld.damageSources.mobAttackNoAggro(this)

            // Schaden wird nicht beeinflusst, nur Knockback
            if (livingEntity.damage(damageSource, 0f)) {
                EnchantmentHelper.onTargetDamaged(serverWorld, livingEntity, damageSource)
            }

            // Berechnung der Geschwindigkeitseffekte für den Knockback
            val speedAmplifier = if (this.hasStatusEffect(StatusEffects.SPEED)) {
                this.getStatusEffect(StatusEffects.SPEED)!!.amplifier + 1
            } else 0

            val slownessAmplifier = if (this.hasStatusEffect(StatusEffects.SLOWNESS)) {
                this.getStatusEffect(StatusEffects.SLOWNESS)!!.amplifier + 1
            } else 0

            // Zusätzlicher Geschwindigkeitsschub durch Effekte
            val speedFactor = 0.25f * (speedAmplifier - slownessAmplifier).toFloat()

            // Geschwindigkeit des Spielers beeinflusst den Knockback
            val baseSpeed = MathHelper.clamp(this.movementSpeed * 5f, 0.2f, 10f) + speedFactor

            // Knockback berechnen, verstärkt durch die Geschwindigkeit
            val knockbackStrength = baseSpeed * 1.5 // Stärke des Knockbacks skalieren

            val direction = this.directionVector.normalize().multiply(knockbackStrength)

            // Knockback anwenden
            livingEntity.modifyVelocity(Vec3d(direction.x, 0.4 + knockbackStrength / 10, direction.z))

            // Sound-Effekte hinzufügen
            livingEntity.sound(SoundEvents.ENTITY_GOAT_RAM_IMPACT, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
        }
    }

}
