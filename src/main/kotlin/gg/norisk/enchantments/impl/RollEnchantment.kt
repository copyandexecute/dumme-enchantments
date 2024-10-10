package gg.norisk.enchantments.impl

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.emote.network.EmoteNetworking.playEmote
import gg.norisk.emote.network.EmoteNetworking.stopEmote
import gg.norisk.enchantments.EnchantmentRegistry.rolling
import gg.norisk.enchantments.EnchantmentRegistry.slippery
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.impl.TrashEnchantment.isTrash
import gg.norisk.enchantments.sound.SoundRegistry
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object RollEnchantment {
    fun initClient() {
        WorldRenderEvents.LAST.register {
            val player = MinecraftClient.getInstance().player ?: return@register
            player.handleInput()
        }
    }

    //Credits to https://modrinth.com/mod/pitchy/version/bPhNIk2u
    fun Entity.handlePitchChange(e: Double, d: Double, callBackInfo: CallbackInfo) {
        if (!isStupidRolling) return
        if (this is ClientPlayerEntity) {
            if (MathHelper.abs(this.pitch) > 180.0f) {
                this.pitch = (-MathHelper.sign(this.pitch.toDouble()) * 180).toFloat() + (this.pitch - (MathHelper.sign(
                    this.pitch.toDouble()
                ) * 180).toFloat())
                this.prevPitch =
                    (-MathHelper.sign(this.prevPitch.toDouble()) * 180).toFloat() + (this.prevPitch - (MathHelper.sign(
                        this.prevPitch.toDouble()
                    ) * 180).toFloat())
            }

            val changePitch: Float = d.toFloat() * 0.15f
            val changeYaw: Float = e.toFloat() * 0.15f
            this.pitch += changePitch
            this.yaw += if (MathHelper.abs(this.pitch) % 360.0f > 90.0f) changeYaw else changeYaw
            this.prevPitch += changePitch
            this.prevYaw += if (MathHelper.abs(this.pitch) % 360.0f > 90.0f) changeYaw else changeYaw
            callBackInfo.cancel()
        }
    }

    fun ClientPlayerEntity.handleInput() {
        if (!isStupidRolling) return
        val delta = MinecraftClient.getInstance().renderTickCounter.getTickDelta(false)
        val speed = velocity.horizontalLengthSquared() * 40
        // lerpedSideways = MathHelper.lerp(delta, lerpedSideways, input.movementSideways * 50f)
        //lerpedForward = MathHelper.lerp(delta, lerpedForward, input.movementForward * 50f)
        changeLookDirection(
            input.movementSideways * -1 * 2.0,
            input.movementForward * 5.0 * speed,
        )
    }

    fun handleMatrixStackRotation(
        player: AbstractClientPlayerEntity,
        matrixStack: MatrixStack,
        f: Float,
        g: Float,
        h: Float,
        i: Float
    ) {
        if (!player.isStupidRolling) return
        val size = player.height / 2
        matrixStack.translate(0f, size, 0f)
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.yaw))
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.pitch * 4))
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(player.yaw))
        matrixStack.translate(0f, -size, 0f)
    }

    fun Entity.handleSneaking(isSneaking: Boolean) {
        val player = this as? ServerPlayerEntity? ?: return
        val trash = rolling.getLevel(getEquippedStack(EquipmentSlot.LEGS))

        if (isSneaking && trash != null) {
            player.toggleRolling()
        } else if (isTrash && trash == null) {
            player.toggleRolling()
        }
    }

    private fun ServerPlayerEntity.toggleRolling() {
        isStupidRolling = !isStupidRolling
        if (isStupidRolling) {
            playEmote("emotes/rolling.animation.json".toId())
            world.playSoundFromEntity(
                null,
                this,
                SoundEvents.ENTITY_ARMADILLO_UNROLL_FINISH,
                SoundCategory.PLAYERS,
                1f,
                2f
            )
        } else {
            world.playSoundFromEntity(
                null,
                this,
                SoundEvents.ENTITY_ARMADILLO_UNROLL_FINISH,
                SoundCategory.PLAYERS,
                1f,
                1f
            )
            stopEmote("emotes/rolling.animation.json".toId())
        }
    }

    fun applyStepSound(
        instance: Entity,
        soundEvent: SoundEvent,
        f: Float,
        g: Float,
        original: Operation<Void>
    ): Boolean {
        if (instance is LivingEntity && instance.isStupidRolling && rolling.getLevel(instance.getEquippedStack(EquipmentSlot.LEGS)) != null) {
            original.call(instance, SoundEvents.ENTITY_BREEZE_SLIDE, f, 0.2f)
            return true
        }
        return false
    }

    var Entity.isStupidRolling: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:IsRolling") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:IsRolling", value)
        }
}
