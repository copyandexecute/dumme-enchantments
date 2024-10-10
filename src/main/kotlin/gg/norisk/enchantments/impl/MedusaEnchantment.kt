package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.enchantments.EnchantmentRegistry.medusa
import gg.norisk.enchantments.EnchantmentUtils
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Blocks
import net.minecraft.client.world.ClientWorld
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundEvents
import net.silkmc.silk.commands.command

object MedusaEnchantment {
    val texture = "textures/medusa_overlay.png".toId()

    interface MedusaEntity {
        var stupid_animationProgress: Float?
        var stupid_bodyYaw: Float?
        var stupid_headYaw: Float?
        var stupid_pitch: Float?
        var stupid_limbSpeed: Float?
        var stupid_limbPos: Float?
    }

    fun initServer() {
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:isStupidMedusa") return@listen
            val world = event.entity.world
            val entity = event.entity as MedusaEntity
            entity.stupid_animationProgress = null
            entity.stupid_bodyYaw = null
            entity.stupid_headYaw = null
            entity.stupid_pitch = null
            entity.stupid_limbPos = null
            entity.stupid_limbSpeed = null
            if (world.isClient) {
                event.entity.spawnMedusaTransformationParticles()
            }
        }

        ServerTickEvents.END_WORLD_TICK.register { world ->
            for (player in world.players.filter { medusa.getLevel(it.getEquippedStack(EquipmentSlot.HEAD)) == 2 }) {
                val entity = EnchantmentUtils.raycastEntity(player, 32)
                if (entity.isPresent && entity.get() is LivingEntity && !entity.get().isStupidMedusa) {
                    entity.get().activateMedusa()
                }
            }
        }

        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        command("stupidenchantments") {
            literal("medusa") {
                argument("target", EntityArgumentType.entities()) { entities ->
                    runs {
                        for (entity in entities().getEntities(this.source)) {
                            entity.activateMedusa()
                        }
                    }
                }
            }
        }
    }

    private fun Entity.spawnMedusaTransformationParticles() {
        repeat(20) {
            (world as ClientWorld).addParticle(
                BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultState),
                this.getParticleX(0.5),
                this.randomBodyY,
                this.getParticleZ(0.5),
                1.0,
                1.0,
                1.0
            )
        }
    }

    fun Entity.activateMedusa() {
        isStupidMedusa = !isStupidMedusa
        if (isStupidMedusa) {
            sound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 0.4f)
        }
        if (this is MobEntity) {
            this.isAiDisabled = isStupidMedusa
        }
    }

    var Entity.isStupidMedusa: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:isStupidMedusa") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:isStupidMedusa", value)
        }
}
