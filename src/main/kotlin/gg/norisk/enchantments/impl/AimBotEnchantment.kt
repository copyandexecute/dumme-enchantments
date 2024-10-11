package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.enchantments.EnchantmentRegistry.aimbot
import gg.norisk.enchantments.EnchantmentUtils
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.ProjectileEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAttachmentType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.RotationAxis
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literalText
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.Color
import kotlin.jvm.optionals.getOrNull
import kotlin.math.asin
import kotlin.math.atan2

object AimBotEnchantment {
    fun initServer() {
        ServerTickEvents.END_WORLD_TICK.register { world ->
            for (player in world.players) {
                if (aimbot.getLevel(player.mainHandStack) != null) {
                    val distance = 128
                    val firstAttempt = EnchantmentUtils.raycastEntity(player, distance)
                    val entity =
                        firstAttempt.getOrNull() ?: EnchantmentUtils.raycastThickEntity(player, 128, 15.0).getOrNull()
                        ?: continue
                    if (entity.id != player.stupidAutoAimTargetId) {
                        player.sound(SoundEvents.UI_LOOM_SELECT_PATTERN,0.2,2f)
                        player.stupidAutoAimTargetId = entity.id
                        //player.sendMessage("You are looking at $entity".literal)
                    }
                } else {
                    if (player.stupidAutoAimTargetId != -1) {
                        player.stupidAutoAimTargetId = -1
                    }
                }
            }
        }
    }

    fun ArrowEntity.handleTick(callbackInfo: CallbackInfo) {
        if (!isStupidAimbot) return
        pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY
        val entity = world.getEntityById(stupidAimbotTargetId) ?: return
        val i = 1.0
        if (isStupidAimbot && (age > 0)) {
            if (!this.stupidIsOwnerAlive(entity)) {
                if (!world.isClient && this.pickupType == PersistentProjectileEntity.PickupPermission.ALLOWED) {
                    //this.dropStack(this.asItemStack(), 0.1f)
                }
                this.discard()
            } else {
                this.isNoClip = true
                if (squaredDistanceTo(entity) < 3) {
                    this.isNoClip = false
                }
                val vec3d = entity.eyePos.subtract(this.pos)
                this.setPos(this.x, this.y + vec3d.y * 0.015 * i, this.z)
                if (world.isClient) {
                    this.lastRenderY = this.y
                }

                val d = 0.05 * i
                this.velocity = velocity.multiply(0.95).add(vec3d.normalize().multiply(d))
            }
        }
    }

    private fun ArrowEntity.stupidIsOwnerAlive(entity: Entity?): Boolean {
        return if (entity == null || !entity.isAlive) false else entity !is ServerPlayerEntity || !entity.isSpectator()
    }

    var PersistentProjectileEntity.isStupidAimbot: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:isStupidAimbot") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:isStupidAimbot", value)
        }

    var PersistentProjectileEntity.stupidAimbotTargetId: Int
        get() = this.getSyncedData<Int>("$MOD_ID:isStupidAimbotTargetId") ?: -1
        set(value) {
            this.setSyncedData("$MOD_ID:isStupidAimbotTargetId", value)
        }

    var PlayerEntity.stupidAutoAimTargetId: Int
        get() = this.getSyncedData<Int>("$MOD_ID:stupidAutoAimTargetId") ?: -1
        set(value) {
            this.setSyncedData("$MOD_ID:stupidAutoAimTargetId", value, (this as? ServerPlayerEntity?))
        }

    fun applyProjectileSpawned(
        serverWorld: ServerWorld,
        itemStack: ItemStack,
        projectile: PersistentProjectileEntity
    ) {
        mcCoroutineTask(sync = true, client = false, delay = 1.ticks) {
            val level = aimbot.getLevel(itemStack) ?: return@mcCoroutineTask
            projectile.isStupidAimbot = true
            val shooter = projectile.owner as? PlayerEntity? ?: return@mcCoroutineTask
            projectile.stupidAimbotTargetId = shooter.stupidAutoAimTargetId
        }
    }

    fun <T : Entity> renderTargetNameTag(
        entity: T,
        tickDelta: Float,
        g: Float,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int,
        dispatcher: EntityRenderDispatcher,
        textRenderer: TextRenderer
    ) {
        val text = literalText {
            text("▼")
            bold = true
            color = Color.RED.rgb
        }
        val player = MinecraftClient.getInstance().player ?: return
        if (player.stupidAutoAimTargetId != entity.id) return
        val d: Double = dispatcher.getSquaredDistanceToCamera(entity)
        if (!(d > 4096.0)) {
            val vec3d = entity.attachments.getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta))
            if (vec3d != null) {
                val bl = !entity.isSneaky
                val j = if ("deadmau5" == text.getString()) -10 else 0
                matrixStack.push()
                matrixStack.translate(vec3d.x, vec3d.y + 0.5, vec3d.z)
                matrixStack.multiply(dispatcher.getRotation())
                val scale = 2f
                //matrixStack.translate(0f, scale,0f)
                matrixStack.scale(scale, scale, scale)
                matrixStack.scale(0.025f, -0.025f, 0.025f)
                val matrix4f = matrixStack.peek().positionMatrix
                val g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0f)
                val k = (g * 255.0f).toInt() shl 24
                val h: Float = (-textRenderer.getWidth(text) / 2).toFloat()
                textRenderer.draw(
                    text,
                    h,
                    j.toFloat(),
                    553648127,
                    false,
                    matrix4f,
                    vertexConsumerProvider,
                    if (bl) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL,
                    k,
                    i
                )
                if (bl) {
                    textRenderer.draw(
                        text,
                        h,
                        j.toFloat(),
                        -1,
                        false,
                        matrix4f,
                        vertexConsumerProvider,
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        i
                    )
                }

                matrixStack.pop()
            }
        }
    }

    fun <T : PersistentProjectileEntity> ProjectileEntityRenderer<T>.handleRendering(
        persistentProjectileEntity: T,
        f: Float,
        g: Float,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int
    ): Boolean {
        val owner = persistentProjectileEntity.world.getEntityById(persistentProjectileEntity.stupidAimbotTargetId)
            ?: return false

        // Berechne die Vektoren der Positionen
        val ownerPos = owner.pos // Position des Owners
        val arrowPos = persistentProjectileEntity.pos // Position des Pfeils

        // Berechne den Richtungsvektor vom Pfeil zum Owner
        val directionVec = ownerPos.subtract(arrowPos).normalize()

        // Berechne die Yaw und Pitch aus dem Richtungsvektor
        val yaw = -Math.toDegrees(atan2(directionVec.z, directionVec.x)) - 90.0
        val pitch = Math.toDegrees(asin(directionVec.y))

        matrixStack.push()

        // Anwenden der Rotation, sodass der Pfeil immer auf den Owner zeigt
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw.toFloat() + 90f))
        //matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch.toFloat() ))

        // Setze die Pfeil-Transformationen und Skala (basierend auf deinem ursprünglichen Code)
        matrixStack.scale(0.05625f, 0.05625f, 0.05625f)
        matrixStack.translate(-4.0f, 0.2f, 0.0f)

        // Rende den Pfeil (dein bestehender Code)
        val vertexConsumer =
            vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(this.getTexture(persistentProjectileEntity)))
        val entry = matrixStack.peek()

        this.vertex(entry, vertexConsumer, -7, -2, -2, 0.0f, 0.15625f, -1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, -2, 2, 0.15625f, 0.15625f, -1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, 2, 2, 0.15625f, 0.3125f, -1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, 2, -2, 0.0f, 0.3125f, -1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, 2, -2, 0.0f, 0.15625f, 1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, 2, 2, 0.15625f, 0.15625f, 1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, -2, 2, 0.15625f, 0.3125f, 1, 0, 0, i)
        this.vertex(entry, vertexConsumer, -7, -2, -2, 0.0f, 0.3125f, 1, 0, 0, i)

        for (u in 0..3) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f))
            this.vertex(entry, vertexConsumer, -8, -2, 0, 0.0f, 0.0f, 0, 1, 0, i)
            this.vertex(entry, vertexConsumer, 8, -2, 0, 0.5f, 0.0f, 0, 1, 0, i)
            this.vertex(entry, vertexConsumer, 8, 2, 0, 0.5f, 0.15625f, 0, 1, 0, i)
            this.vertex(entry, vertexConsumer, -8, 2, 0, 0.0f, 0.15625f, 0, 1, 0, i)
        }

        matrixStack.pop()
        return true
    }
}
