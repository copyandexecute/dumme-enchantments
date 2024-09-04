package gg.norisk.enchantments.impl

import gg.norisk.enchantments.EnchantmentRegistry.squish
import gg.norisk.enchantments.EnchantmentUtils.blockPos
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.RenderUtils
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.utils.Animation
import gg.norisk.enchantments.utils.Vec3dSerializer
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.network.packet.s2cPacket
import kotlin.time.Duration.Companion.seconds

object SquishEnchantment {
    fun initClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities {
            for (entity in it.world().entities) {
                for (squishAnimation in entity.squishEntity.squishAnimations) {
                    applyAnvilRender(
                        entity,
                        it.tickCounter().getTickDelta(false),
                        it.matrixStack() ?: continue,
                        squishAnimation
                    )
                }
            }
        })
        squishAddAnimationS2C.receiveOnClient { packet, context ->
            mcCoroutineTask(sync = true, client = true) {
                val entity = context.client.world?.getEntityById(packet.entityId) ?: return@mcCoroutineTask
                entity.squishEntity.squishAnimations.add(packet.animation)
            }
        }
        squishSizeS2C.receiveOnClient { packet, context ->
            mcCoroutineTask(sync = true, client = true) {
                val entity = context.client.world?.getEntityById(packet.entityId) ?: return@mcCoroutineTask
                entity.squishEntity.squishSize = packet.size
            }
        }
    }

    interface SquishEntity {
        val squishAnimations: MutableList<Animation>
        var squishSize: Vec3d
    }

    fun Entity.onTick() {
        squishEntity.squishAnimations.removeIf { it.isDone }
    }

    @Serializable
    data class EntityAnimationPacket(val entityId: Int, val animation: Animation)

    @Serializable
    data class EntitySquishSizePacket(val entityId: Int, @Serializable(with = Vec3dSerializer::class) val size: Vec3d)

    val squishAddAnimationS2C = s2cPacket<EntityAnimationPacket>("squish-start".toId())
    val squishSizeS2C = s2cPacket<EntitySquishSizePacket>("squish-size".toId())

    val Entity.squishEntity get() = this as SquishEntity

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        val squishLevel = squish.getLevel(itemStack)
        if (squishLevel != null) {
            val duration = 0.5.seconds
            val animation = Animation(1f, 0f, duration, Animation.Easing.EXPO_IN_OUT)
            entity.squishEntity.squishAnimations.add(animation)
            squishAddAnimationS2C.sendToAll(EntityAnimationPacket(entity.id, animation))
            mcCoroutineTask(sync = true, client = false, delay = duration.div(2)) {
                world.playSoundFromEntity(
                    null,
                    entity,
                    SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.BLOCKS,
                    0.5f,
                    5f
                )

                var size = entity.squishEntity.squishSize
                entity.squishEntity.squishSize =
                    size.add(0.1 * squishLevel.toDouble(), -0.2 * squishLevel, 0.1 * squishLevel.toDouble())
                size = entity.squishEntity.squishSize
                entity.squishEntity.squishSize = Vec3d(size.x, Math.max(0.1, size.y), size.z)
                squishSizeS2C.sendToAll(EntitySquishSizePacket(entity.id, entity.squishEntity.squishSize))
            }
        }
    }

    fun applyAnvilRender(
        entity: Entity,
        f: Float,
        matrixStack: MatrixStack,
        animation: Animation
    ) {
        val pos = entity.getLerpedPos(f).add(-0.5, (entity.standingEyeHeight + 1.0) * animation.get(), -0.5)
        RenderUtils.renderBlock(matrixStack, pos, Blocks.ANVIL.defaultState, pos.blockPos)
    }

    fun <T : Entity> handleSquishRendering(livingEntity: T, matrixStack: MatrixStack) {
        val scale = livingEntity.squishEntity.squishSize
        matrixStack.scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
    }
}
