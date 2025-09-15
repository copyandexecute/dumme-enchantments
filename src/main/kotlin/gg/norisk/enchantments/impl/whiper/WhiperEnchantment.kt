package gg.norisk.enchantments.impl.whiper

import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.sound.WhiperSoundInstance
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import org.joml.Math.toRadians
import java.awt.Color
import kotlin.math.sin

object WhiperEnchantment {
    val WHIPER_TEXTURE = Identifier.of(MOD_ID, "textures/whiper.png")
    var ourWorldTime = 200

    fun initServer() {
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("whiper") {
                runs {
                    this.default()
                    this.whiper()
                }
                literal("toggle") {
                    runs {
                        val player = this.source.playerOrThrow
                        player.nrc_hasWhiper = !player.nrc_hasWhiper
                    }
                }
            }
        }
    }

    fun initClient() {
        ClientTickEvents.END_WORLD_TICK.register {
            val player = MinecraftClient.getInstance().cameraEntity as? LivingEntity? ?: return@register
            val helmet = player.getEquippedStack(EquipmentSlot.HEAD)
            if (EnchantmentRegistry.whiper.getLevel(helmet) == null) return@register

            if (player.nrc_hasWhiper) {
                ourWorldTime++
            }
        }
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:nrc_hasWhiper") return@listen
            if (!event.entity.world.isClient) return@listen
            if (event.entity.nrc_hasWhiper) {
                MinecraftClient.getInstance().soundManager.play(WhiperSoundInstance(event.entity))
            }
        }
        HudRenderCallback.EVENT.register { drawContext, tickCounter ->
            val client = MinecraftClient.getInstance()
            val world = client.world ?: return@register

            val player = MinecraftClient.getInstance().cameraEntity as? LivingEntity? ?: return@register
            val helmet = player.getEquippedStack(EquipmentSlot.HEAD)

            if (EnchantmentRegistry.whiper.getLevel(helmet) == null) return@register

            // EXACT calculation as in Minecraft's setShaderGameTime!
            val worldTime = ourWorldTime % 24000L
            val tickProgress = if (player.nrc_hasWhiper) tickCounter.getTickProgress(true) else 0f
            val gameTime = (worldTime.toFloat() + tickProgress) / 24000.0f  // Exact GameTime formula
            val wiperTime = gameTime * 5000.0f               // Match your shader speed
            val angle = toRadians(80f)
            val wiperAngle = sin(wiperTime) * angle   // ±80 degrees in radians (match shader)

            // Convert radians to degrees for Minecraft rotation
            val wiperAngleDegrees = Math.toDegrees(wiperAngle.toDouble()).toFloat()

            val matrices = drawContext.matrices

            // Screen dimensions
            val screenWidth = client.window.scaledWidth
            val screenHeight = client.window.scaledHeight

            // Wiper texture dimensions (adjust to your texture size)
            val wiperWidth = 640
            val wiperLength = 360

            // Position at bottom center (like in shader: vec2(0.5, 0.0))
            val pivotX = screenWidth / 2
            val pivotY = screenHeight

            matrices.push()

            // Move to pivot point (bottom center)
            matrices.translate(pivotX.toFloat(), pivotY.toFloat(), 0.0f)

            // Rotate around the pivot (bottom of wiper)
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wiperAngleDegrees))

            // Draw texture from pivot point upward
            drawContext.drawTexture(
                RenderLayer::getGuiTextured,
                "textures/whiper.png".toId(),
                -wiperWidth / 2,  // Center horizontally around pivot
                -wiperLength,     // Draw upward from pivot
                0f,
                0f,
                wiperWidth,
                wiperLength,
                wiperWidth,
                wiperLength
            )

            matrices.pop()
        }
    }

    fun Entity.toggleSneaking(sneaking: Boolean) {
        if (sneaking) {
            val livingEntity = this as? LivingEntity? ?: return
            if (EnchantmentRegistry.whiper.getLevel(livingEntity.getEquippedStack(EquipmentSlot.HEAD)) == null) return
            nrc_hasWhiper = !nrc_hasWhiper
            if (!nrc_hasWhiper) {
                sound(SoundRegistry.LIGHTSWITCH_OFF, pitch = 0.9f, volume = 0.8f)
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.whiper() {
        val player = this.source.playerOrThrow
        player.serverWorld.setWeather(0,0,true, false)

        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(EnchantmentRegistry.raindrop.getEntry(player.world), 1)
        })
        player.inventory.setStack(6, itemStack(Items.EXPERIENCE_BOTTLE, 64) {})
        player.inventory.setStack(7, itemStack(Items.ANVIL, 64) {})
        player.inventory.setStack(8, itemStack(Items.ENCHANTED_BOOK, 1) {
            addEnchantment(EnchantmentRegistry.whiper.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("§b§lRaindrop Enchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Mach Scheibenwischer auf den Helm und Sneak".literal)
            }
        })
    }

    fun shouldRenderRaindrops(): Boolean {
        val player = MinecraftClient.getInstance().cameraEntity as? LivingEntity? ?: return false
        val helmet = player.getEquippedStack(EquipmentSlot.HEAD)
        if (EnchantmentRegistry.raindrop.getLevel(helmet) != null && player.isTouchingWaterOrRain) {
            return true
        }
        return false
    }

    val isWhiperActive get() = MinecraftClient.getInstance().player?.nrc_hasWhiper ?: false

    var Entity.nrc_hasWhiper: Boolean
        get() = this.getSyncedData("${MOD_ID}:nrc_hasWhiper") ?: false
        set(value) {
            this.setSyncedData("${MOD_ID}:nrc_hasWhiper", value)
        }
}