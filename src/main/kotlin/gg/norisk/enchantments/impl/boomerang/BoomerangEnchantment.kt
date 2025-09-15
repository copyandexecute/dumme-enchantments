package gg.norisk.enchantments.impl.boomerang

import com.mojang.brigadier.context.CommandContext
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import java.awt.Color

object BoomerangEnchantment {
    fun initServer() {
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("boomerang") {
                runs {
                    this.default()
                    this.boomerang()
                }
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.boomerang() {
        val player = this.source.playerOrThrow

        player.inventory.setStack(4, itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.boomerang.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("§b§lEnchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Rechtsklick zum Werfen".literal)
            }
        })
    }
}