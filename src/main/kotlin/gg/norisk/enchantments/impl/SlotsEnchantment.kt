package gg.norisk.enchantments.impl

import gg.norisk.enchantments.EnchantmentRegistry.slots
import gg.norisk.enchantments.EnchantmentUtils.blockPos
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.silkmc.silk.core.task.mcCoroutineTask
import kotlin.random.Random

object SlotsEnchantment {
    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        slots.getLevel(itemStack) ?: return
        var prevEntity: Entity? = null
        if (entity is PlayerEntity) return
        entity.discard()
        mcCoroutineTask(sync = true, client = false, howOften = 20) {
            val randomEntity = getRandomEntityType(world)
            prevEntity?.discard()
            prevEntity = randomEntity.create(world)
            prevEntity?.setPosition(entity.pos)
            world.spawnEntity(prevEntity)
            world.playSound(
                null,
                entity.pos.blockPos,
                SoundEvents.BLOCK_NOTE_BLOCK_HAT.comp_349(),
                SoundCategory.BLOCKS,
                0.5f,
                1f
            )

            if (it.counterDownToZero == 0L) {
                world.playSound(
                    null,
                    entity.pos.blockPos,
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.comp_349(),
                    SoundCategory.BLOCKS,
                    0.5f,
                    1f
                )
            }
        }
    }

    private fun getRandomEntityType(world: ServerWorld): EntityType<*> {
        val randomEntity = Registries.ENTITY_TYPE.get(Random.nextInt(Registries.ENTITY_TYPE.size()))
        return if (randomEntity.isSummonable && randomEntity.create(world) is LivingEntity) {
            randomEntity
        } else {
            getRandomEntityType(world)
        }
    }
}
