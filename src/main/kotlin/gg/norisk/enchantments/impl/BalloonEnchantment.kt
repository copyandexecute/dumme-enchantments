package gg.norisk.enchantments.impl

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import gg.norisk.datatracker.entity.*
import gg.norisk.enchantments.EnchantmentRegistry.balloon
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.utils.Animation
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.model.SinglePartEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.network.packet.s2cPacket
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

object BalloonEnchantment {
    interface BallonModel {
        var stupid_ballonAnimation: Animation?
    }

    interface BallonModelPart {
        var stupid_ballonAnimation: Animation?
    }

    fun initServer() {
        (registeredTypes as MutableMap<Any, Any>).put(
            Animation::class,
            s2cPacket<Pair<EntityWrapper, Animation>>("entity-stupid-animation-sync".toId())
        )
    }

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        balloon.getLevel(itemStack) ?: return
        val livingEntity = entity as? LivingEntity? ?: return
        val duration = 4.seconds
        livingEntity.addStatusEffect(
            StatusEffectInstance(
                StatusEffects.LEVITATION,
                (duration.inWholeMilliseconds / 50).toInt(), 1, false, false
            )
        )
        livingEntity.setStupidBallooning(Animation(1f, 5f, duration, Animation.Easing.LINEAR))
        livingEntity.sound(SoundRegistry.BALLOON_BLOW_UP)
        mcCoroutineTask(sync = true, client = false, delay = duration) {
            world.spawnParticles(ParticleTypes.EXPLOSION,livingEntity.x,livingEntity.eyePos.y,livingEntity.z,1,0.0,0.0,0.0,0.0)
            livingEntity.sound(SoundRegistry.BALLOON_POP)
        }
        // livingEntity.stupid_ballonAnimation = Animation(1f, 5f, 1.seconds, Animation.Easing.EXPO_IN)
    }

    fun Entity.getStupidBallooning(): Animation? {
        return this.getSyncedData("$MOD_ID:isStupidBallooning")
    }

    fun Entity.setStupidBallooning(animation: Animation?) {
        if (animation == null) {
            this.unsetSyncedData("$MOD_ID:isStupidBallooning")
        } else {
            this.setSyncedData("$MOD_ID:isStupidBallooning", animation)
        }
    }

    fun EntityModel<*>.handleHead(
        instance: MutableIterable<ModelPart>,
        consumer: Consumer<ModelPart>,
        original: Operation<Void>,
        matrixStack: MatrixStack
    ) {
        val dummy = this as BallonModel
        val animation = stupid_ballonAnimation
        var renderFlag = true
        instance.forEach {
            (it as BallonModelPart).stupid_ballonAnimation = animation
        }
        if (animation != null) {
            renderFlag = !animation.isDone
        }
        if (renderFlag) {
            original.call(instance, consumer)
        }
    }

    fun SinglePartEntityModel<*>.handleSinglePartHead(
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer
    ) {
        val dummy = this as BallonModel
        val animation = stupid_ballonAnimation
        this.getChild("head").ifPresent {
            (it as BallonModelPart).stupid_ballonAnimation = animation
            if (animation != null && animation.isDone) {
                (it as BallonModelPart).stupid_ballonAnimation = animation.apply {
                    this.start = 0f
                    this.end  = 0f
                }
            }
        }
    }
}
