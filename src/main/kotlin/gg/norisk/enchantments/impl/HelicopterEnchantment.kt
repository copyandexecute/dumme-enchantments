package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.enchantments.EnchantmentRegistry.helicopter
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.sound.HelicopterSoundInstance
import gg.norisk.enchantments.utils.Animation
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.core.entity.modifyVelocity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object HelicopterEnchantment {
    fun initServer() {
        PlayerBlockBreakEvents.AFTER.register(PlayerBlockBreakEvents.After { world, player, pos, state, blockEntity ->
            helicopter.getLevel(player.mainHandStack) ?: return@After
            val fallingBlock = FallingBlockEntity.spawnFromBlock(world, pos, state)
            fallingBlock.modifyVelocity(Vec3d(0.0, Random.nextDouble(0.1, 1.0), 0.0))
            fallingBlock.isHelicopter = true
        })
        UseEntityCallback.EVENT.register(UseEntityCallback { player, world, hand, entity, hitResult ->
            if (!world.isClient && entity.isHelicopter) {
                player.startRiding(entity, true)
            }
            return@UseEntityCallback ActionResult.PASS
        })
    }

    fun initClient() {
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:IsHelicopter") return@listen
            if (!event.entity.world.isClient) return@listen
            if (event.entity.isHelicopter) {
                MinecraftClient.getInstance().soundManager.play(HelicopterSoundInstance(event.entity))
            }
        }
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            val player = MinecraftClient.getInstance().player ?: return@EndTick
            val helicopter = player.vehicle as? FallingBlockEntity? ?: return@EndTick
            if (helicopter.isHelicopter) {
                player.yaw = helicopter.helicopterBlock.animation.get()
            }
        })
    }

    var Entity.isHelicopter: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:IsHelicopter") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:IsHelicopter", value)
        }

    interface HelicopterBlock {
        val animation: Animation
    }

    val FallingBlockEntity.helicopterBlock get() = (this as HelicopterBlock)

    fun defaultAnimation() = Animation(0f, 360f, 0.3.seconds)

    fun applyFly() {
    }

    fun applyRotation(
        fallingBlockEntity: FallingBlockEntity,
        matrices: MatrixStack,
        tickDelta: Float,
        g: Float
    ) {
        if (!fallingBlockEntity.isHelicopter) return
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fallingBlockEntity.helicopterBlock.animation.get()))
        if (fallingBlockEntity.helicopterBlock.animation.isDone) {
            fallingBlockEntity.helicopterBlock.animation.reset()
        }
    }

    fun FallingBlockEntity.applyGravity(cir: CallbackInfoReturnable<Double>) {
        if (this.isHelicopter) {
            cir.returnValue = 0.002
        }
    }
}
