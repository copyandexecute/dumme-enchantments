package gg.norisk.enchantments.impl

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentRegistry.slippery
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.utils.SlipperyBlockFeatureRenderer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.block.Block
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.silkmc.silk.core.item.itemStack

object SlipperyEnchantment {
    fun LivingEntity.apply(block: Block): Float {
        val level = slippery.getLevel(getEquippedStack(EquipmentSlot.FEET))
        if (level == null) {
            return block.slipperiness
        } else {
            val slippery = 1.0f + 0.1f * level
            //println("Slippery: $slippery")
            return slippery
        }
    }

    fun initServer() {
        ServerTickEvents.END_SERVER_TICK.register {
            for (world in it.worlds) {
                for (entity in world.iterateEntities()) {
                    val livingEntity = entity as? LivingEntity? ?: continue
                    livingEntity.equipStack(EquipmentSlot.FEET, itemStack(Items.DIAMOND_BOOTS) {
                        addEnchantment(EnchantmentRegistry.slippery.getEntry(world), 1)
                        addEnchantment(Enchantments.UNBREAKING.getEntry(world), 5)
                        addEnchantment(Enchantments.BINDING_CURSE.getEntry(world), 1)
                        addEnchantment(Enchantments.VANISHING_CURSE.getEntry(world), 1)
                    })
                }
            }
        }
        ///item replace entity @e armor.feet with minecraft:diamond_boots[enchantments={levels:{'enchantments:slippery':1}}]
    }

    fun initClient() {
            LivingEntityFeatureRendererRegistrationCallback.EVENT.register { entityType, entityRenderer, registrationHelper, context ->
                val modelPart = EntityModelLayers.getLayers().toList()
                    .firstOrNull { it.id.path == entityType.untranslatedName.lowercase() }
                if (modelPart != null) {
                    val model = context.getPart(modelPart)
                    registrationHelper.register(
                        SlipperyBlockFeatureRenderer(
                            entityRenderer as FeatureRendererContext<LivingEntityRenderState, EntityModel<LivingEntityRenderState>>,
                            model
                        )
                    )
                }
            }
    }

    fun applyStepSound(instance: Entity, soundEvent: SoundEvent, f: Float, g: Float, original: Operation<Void>): Boolean {
        if (instance is LivingEntity && slippery.getLevel(instance.getEquippedStack(EquipmentSlot.FEET)) != null) {
            original.call(instance, SoundEvents.ENTITY_BREEZE_SLIDE, f, 2f)
            return true
        }
        return false
    }
}
