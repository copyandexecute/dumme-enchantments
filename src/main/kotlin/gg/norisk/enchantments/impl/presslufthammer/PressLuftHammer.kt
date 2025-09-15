package gg.norisk.enchantments.impl.presslufthammer

import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.emote.ext.playEmote
import gg.norisk.emote.ext.stopEmote
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.sound.PressLuftHammerSoundInstance
import gg.norisk.enchantments.utils.BoomShake
import gg.norisk.enchantments.utils.CameraShaker
import gg.norisk.utils.events.MouseEvents
import gg.norisk.utils.ext.EntityRenderStateExt
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.render.item.ItemRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.minecraft.util.Arm
import net.minecraft.util.Hand
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.math.geometry.filledCirclePositionSet
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import net.silkmc.silk.network.packet.c2sPacket
import org.joml.Quaternionf
import java.awt.Color
import java.lang.Math.toRadians
import kotlin.random.Random

object PressLuftHammer {
    fun initServer() {
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("presslufthammer") {
                runs {
                    this.default()
                    this.presslufthammer()
                }
                literal("toggle") {
                    runs {
                        val player = this.source.playerOrThrow
                        player.nrc_isPressLuftHammer = !player.nrc_isPressLuftHammer
                    }
                }
            }
        }

        ServerTickEvents.END_WORLD_TICK.register { server ->
            for (player in server.players.filter { it.nrc_isPressLuftHammer }) {
                if (!player.hasPressLuftHammerInHand) {
                    player.nrc_isPressLuftHammer = false
                    continue
                }

                val level =
                    EnchantmentRegistry.presslufthammer.getLevel(player.getStackInHand(Hand.MAIN_HAND)) ?: continue

                val posToBreak = player.blockPos.down()
                for (pos in posToBreak.filledCirclePositionSet(level * 2)) {
                    if (pos == posToBreak || Random.nextBoolean()) {
                        BlockBreaker.incrementBlockBreak(pos, player.serverWorld, player)
                    }
                }
                //player.modifyVelocity(Vec3d(0.0,0.001,0.0))
            }
        }

        presslufthammerPacket.receiveOnServer({ packet, context ->
            mcCoroutineTask(sync = true, client = true) {
                val player = context.player
                if (player.hasPressLuftHammerInHand) {
                    player.nrc_isPressLuftHammer = packet
                }
            }
        })
    }

    val Entity.hasPressLuftHammerInHand: Boolean
        get() {
            val livingEntity = this as? LivingEntity? ?: return false
            val itemInHand = livingEntity.getStackInHand(Hand.MAIN_HAND)
            val hasEnchantment = EnchantmentRegistry.presslufthammer.getLevel(itemInHand) ?: return false
            return true
        }

    val presslufthammerPacket = c2sPacket<Boolean>("presslufthammer".toId())

    fun initClient() {
        MouseEvents.mouseClickEvent.listen { event ->
            if (MinecraftClient.getInstance().options.useKey.matchesMouse(event.key.code)) {
                presslufthammerPacket.send(event.pressed)
            }
        }
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:nrc_isPressLuftHammer") return@listen
            if (!event.entity.world.isClient) return@listen
            val player = event.entity as? AbstractClientPlayerEntity? ?: return@listen
            if (event.entity.nrc_isPressLuftHammer) {
                player.playEmote("emotes/presslufthammer.animation.json".toId())
                MinecraftClient.getInstance().soundManager.play(PressLuftHammerSoundInstance(event.entity))
            } else {
                player.stopEmote("emotes/presslufthammer.animation.json".toId())
            }
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            val entity = MinecraftClient.getInstance().cameraEntity as? LivingEntity ?: return@register
            if (entity.nrc_isPressLuftHammer) {
                val level = EnchantmentRegistry.presslufthammer.getLevel(entity.getStackInHand(Hand.MAIN_HAND))

                if (level == 1) {
                    CameraShaker.addEvent(BoomShake(Random.nextDouble(0.001, 0.01), 0.1, 2.0))
                } else {
                    CameraShaker.addEvent(BoomShake(Random.nextDouble(0.1, 0.4), 0.1, 0.4))
                }
            }
        }
    }

    var Entity.nrc_isPressLuftHammer: Boolean
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_isPressLuftHammer") ?: false
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_isPressLuftHammer", value)
        }

    private fun <S : ServerCommandSource> CommandContext<S>.presslufthammer() {
        val player = this.source.playerOrThrow

        player.inventory.setStack(3, itemStack(Items.DIAMOND_PICKAXE) {
            addEnchantment(EnchantmentRegistry.presslufthammer.getEntry(player.world), 1)
        })
        player.inventory.setStack(5, itemStack(Items.DIAMOND_PICKAXE) {
            addEnchantment(EnchantmentRegistry.presslufthammer.getEntry(player.world), 5)
        })

        player.sendMessage(literalText {
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent =
                    HoverEvent.ShowText("Rechtsklick gedrückt halten für den Effekt".literal)
            }
        })
    }


    fun <S : EntityRenderState> handleThirdPersonRendering(
        entityState: S,
        itemState: ItemRenderState,
        arm: Arm,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val player = (entityState as? EntityRenderStateExt?)?.nrc_entity as? PlayerEntity? ?: return
        val hand = if (arm == Arm.RIGHT) Hand.MAIN_HAND else Hand.OFF_HAND

        if (player.nrc_isPressLuftHammer && player.hasPressLuftHammerInHand) {
            if (hand == Hand.MAIN_HAND) {
                val xAngle = toRadians(70.0).toFloat()
                val yAngle = toRadians(90.0).toFloat()
                val zAngle = toRadians(180.0).toFloat()
                matrices.multiply(
                    Quaternionf().rotateXYZ(xAngle, yAngle, zAngle).rotateLocalY(toRadians(-35.0).toFloat())
                )
                matrices.translate(-0f,0f,-0.2f)
                //matrices.
                //matrices.scale(5f, 2.0f, 5f)
            }
        }
    }
}