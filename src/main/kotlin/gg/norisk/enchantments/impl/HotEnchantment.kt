package gg.norisk.enchantments.impl

import gg.norisk.emote.network.EmoteNetworking.playEmote
import gg.norisk.enchantments.EnchantmentRegistry.hot
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.toId
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.EQUIPMENT_CHANGE
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.silkmc.silk.core.entity.directionVector

object HotEnchantment {
    private val support = listOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)
    fun initServer() {
        EQUIPMENT_CHANGE.register(ServerEntityEvents.EquipmentChange { livingEntity, equipmentSlot, previousStack, currentStack ->
            if (!support.contains(equipmentSlot)) return@EquipmentChange
            val hot = hot.getLevel(currentStack) ?: return@EquipmentChange
            val player = livingEntity as? ServerPlayerEntity ?: return@EquipmentChange
            livingEntity.world.playSoundFromEntity(
                null,
                livingEntity,
                SoundEvents.ENTITY_GENERIC_BURN,
                SoundCategory.PLAYERS,
                0.75f,
                2f
            )
            player.setOnFireForTicks(5)
            player.playEmote("emotes/hot.animation.json".toId())
            player.damage(player.world.damageSources.lava(), 1f)
            player.equipStack(equipmentSlot, ItemStack.EMPTY)
            player.dropItem(currentStack, false, true)
        })
    }
}
