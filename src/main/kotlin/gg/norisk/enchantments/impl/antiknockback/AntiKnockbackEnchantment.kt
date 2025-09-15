package gg.norisk.enchantments.impl.antiknockback

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.brigadier.context.CommandContext
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.impl.InvertedEnchantment.getStupidKnockbackPos
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

object AntiKnockbackEnchantment {
    fun initServer() {
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("antiknockback") {
                runs {
                    this.default()
                    this.antiknockback()
                }
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.antiknockback() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.antiknockback.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.antiknockback.getEntry(player.world), 50)
        })
        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.antiknockback.getEntry(player.world), 255)
        })
        player.giveItemStack(itemStack(Items.HUSK_SPAWN_EGG, 64) {
        })

        player.sendMessage(literalText {
            text("§b§lEnchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Einfach Mobs schlagen".literal)
            }
        })
    }

    fun handleAntiKnockback(
        damageSource: DamageSource,
        player: PlayerEntity,
        instance: LivingEntity,
        original: Operation<Void>,
        strength: Double,
        antikbLevel: Int
    ) {
        var d = 0.0
        var e = 0.0
        if (damageSource.getSource() is ProjectileEntity) {
            val doubleDoubleImmutablePair: DoubleDoubleImmutablePair =
                (damageSource.source as ProjectileEntity).getKnockback(instance, damageSource)
            d = -doubleDoubleImmutablePair.leftDouble()
            e = -doubleDoubleImmutablePair.rightDouble()
        } else if (damageSource.position != null) {
            val pos = player.getStupidKnockbackPos()
            d = pos.getX() - instance.getX()
            e = pos.getZ() - instance.getZ()
        }
        val knockbackAgainst: Float = 1 + instance.getAttackKnockbackAgainst(player, damageSource)
        player.velocityModified = true
        if (antikbLevel == 255) {
            mcCoroutineTask(sync = true, client = false, howOften = antikbLevel.toLong() / 2, period = 1.ticks) {
                player.velocityModified = true
                original.call(player, (knockbackAgainst * strength * antikbLevel), d, e)
            }
        } else {
            original.call(player, (knockbackAgainst * strength * antikbLevel), d, e)
        }
    }
}