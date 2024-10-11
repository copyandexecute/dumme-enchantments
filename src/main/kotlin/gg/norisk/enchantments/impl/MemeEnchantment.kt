package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.enchantments.EnchantmentRegistry.meme
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.owoplus.entity.OwOEntity
import gg.norisk.owoplus.hack.dimensional.componentHolder
import gg.norisk.owoplus.hack.dimensional.thirdDimensional
import gg.norisk.owoplus.registry.EntityRegistry
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.Sizing
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld

object MemeEnchantment {
    fun initClient() {
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:isStupidMeme") return@listen
            val entity = event.entity as? LivingEntity? ?: return@listen
            val world = entity.world as? ClientWorld ?: return@listen
            val owo = EntityRegistry.OWO.create(world) ?: return@listen
            owo.componentHolder.component = Containers.horizontalFlow(Sizing.content(), Sizing.content()).apply {
                this.thirdDimensional.isEnabled = true
                //this.thirdDimensional.lookAtPlayer = false
                //this.thirdDimensional.dimensionalScale = 0.03f
                val width = 629 / 4
                val height = 21 / 4
                child(Components.texture("textures/meme.png".toId(), 0, 0, width, height, width, height))
            }
            owo.addStatusEffect(StatusEffectInstance(StatusEffects.INVISIBILITY, Int.MAX_VALUE))
            owo.componentHolder.owo_target = entity.uuid
            world.addEntity(owo)
        }

        ClientTickEvents.END_WORLD_TICK.register { world ->
            world.entities.filterIsInstance<OwOEntity>().forEach { owo ->
                val targetId = owo.componentHolder.owo_target ?: return@forEach
                val target = world.entities.find { it.uuid == targetId }
                if (target == null || !target.isAlive) {
                    owo.discard()
                }
            }
        }
    }

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        if (entity.type == EntityType.PLAYER) return
        meme.getLevel(itemStack) ?: return
        entity.isStupidMeme = true
    }

    var Entity.isStupidMeme: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:isStupidMeme") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:isStupidMeme", value)
        }
}
