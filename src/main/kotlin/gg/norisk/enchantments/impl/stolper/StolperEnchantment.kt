package gg.norisk.enchantments.impl.stolper

import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.registeredTypes
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.serialization.Vec3dSerializer
import gg.norisk.emote.network.EmoteNetworking.playEmote
import gg.norisk.emote.network.EmoteNetworking.stopEmote
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.sound.SoundRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.Color
import kotlin.random.Random

object StolperEnchantment {
    fun initServer() {
        // Register Vec3d serializer
        (registeredTypes as MutableMap<Any, Any>).put(
            Vec3d::class,
            Vec3dSerializer
        )
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("stolper") {
                runs {
                    this.default()
                    this.stumble()
                }
                literal("trigger") {
                    runs {
                        this.source.playerOrThrow.stolper()
                    }
                }
            }
        }

        // Register tick event to check for player movement and trigger stumbling
        ServerTickEvents.END_WORLD_TICK.register { world ->
            for (player in world.players) {
                // Check if player has the stumble enchantment on their boots
                val level = EnchantmentRegistry.stumble.getLevel(player.getEquippedStack(EquipmentSlot.FEET))
                //if (level != null) {
                    // If player is already stumbling, check if they've moved enough to get up
                    if (player.isStumbling) {
                        player.stumbleTicks++
                        val distanceMoved = player.pos.distanceTo(player.stumblePos ?: player.pos)
                        if (distanceMoved >= 0.3 && player.stumbleTicks > 30) {
                            // Player has moved enough, stop the stumbling
                            player.isStumbling = false
                            player.stumblePos = null
                            player.stumbleTicks = 0
                            player.stopEmote("emotes/stolpernv2.animation.json".toId())
                        }
                    }
                //}
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.stumble() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.ANVIL) {
        })
        player.giveItemStack(itemStack(Items.DIAMOND_BOOTS) {
        })
        player.giveItemStack(itemStack(Items.ENCHANTED_BOOK, 1) {
            addEnchantment(EnchantmentRegistry.stumble.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.EXPERIENCE_BOTTLE,32) {
        })

        player.sendMessage(literalText {
            text("§b§lEnchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Einfach etwas rumlaufen ohne springen".literal)
            }
        })
    }

    private fun Entity.stolper() {
        val entity = this as? ServerPlayerEntity? ?: return
        playEmote("emotes/stolpernv2.animation.json".toId())
        sound(SoundRegistry.STOLPERN, 0.6, Random.nextDouble(0.9,1.2))
        damage(serverWorld, this.damageSources.generic(), 1f)
        isStumbling = true
        stumblePos = pos
        stumbleTicks = 0
        //addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 5, 5, false, false, false))
    }

    fun Entity.onStepSound(pos: BlockPos, state: BlockState, ci: CallbackInfo) {
        val player = this as? ServerPlayerEntity? ?: return
        val level = EnchantmentRegistry.stumble.getLevel(player.getEquippedStack(EquipmentSlot.FEET))
        if (level == null) return
        if (Random.nextInt(0, 100) > 90 && !player.isStumbling) { // 0.5% chance per tick when moving
            // Trigger stumbling
            player.stolper()
        }
    }

    fun handleApplyMovementInput(player: PlayerEntity, movementInput: Vec3d): Vec3d {
        if (player.isStumbling) {
            if (player.stumbleTicks < 30) {
                return Vec3d.ZERO
            }
        }

        return movementInput
    }

    // Extension properties to track stumbling state
    var PlayerEntity.isStumbling: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:isStumbling") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:isStumbling", value)
        }

    var PlayerEntity.stumbleTicks: Int
        get() = this.getSyncedData("$MOD_ID:stumbleTicks") ?: 0
        set(value) {
            this.setSyncedData("$MOD_ID:stumbleTicks", value)
        }

    var PlayerEntity.stumblePos: Vec3d?
        get() = this.getSyncedData<Vec3d>("$MOD_ID:stumblePos")
        set(value) {
            this.setSyncedData("$MOD_ID:stumblePos", value)
        }
}
