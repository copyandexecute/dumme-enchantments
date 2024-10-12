package gg.norisk.enchantments.command

import com.mojang.brigadier.context.CommandContext
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentRegistry.bouncy
import gg.norisk.enchantments.EnchantmentRegistry.dopamin
import gg.norisk.enchantments.EnchantmentRegistry.helicopter
import gg.norisk.enchantments.EnchantmentRegistry.slippery
import gg.norisk.enchantments.EnchantmentRegistry.verification
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.literalText
import java.awt.Color
import kotlin.random.Random


object EnchantmentsCommand {
    fun initServer() {
        command("dummeenchantmentsnichtausführen") {
            requires { it.hasPermissionLevel(PermissionLevel.COMMAND_RIGHTS.level) }
            literal("1") {
                runs {
                    this.default()
                    this.helicopter()
                }
            }
            literal("2") {
                runs {
                    this.default()
                    this.dopamin()
                }
            }
            literal("3") {
                runs {
                    this.default()
                    this.slippery()
                }
            }
            literal("4") {
                runs {
                    this.default()
                    this.verification()
                }
            }
            literal("5") {
                runs {
                    this.default()
                    this.hot()
                }
            }
            literal("6") {
                runs {
                    this.default()
                    this.bouncy()
                }
            }
            literal("7") {
                runs {
                    this.default()
                    this.slots()
                }
            }
            literal("8") {
                runs {
                    this.default()
                    this.squish()
                }
            }
            literal("9") {
                runs {
                    this.default()
                    this.glitch()
                }
            }
            literal("10") {
                runs {
                    this.default()
                    this.trash()
                }
            }
        }
        command("dummeenchantmentsnichtausführen2") {
            requires { it.hasPermissionLevel(PermissionLevel.COMMAND_RIGHTS.level) }
            literal("1") {
                runs {
                    this.default()
                    this.roll()
                }
            }
            literal("2") {
                runs {
                    this.default()
                    this.balloon()
                }
            }
            literal("3") {
                runs {
                    this.default()
                    this.colossal()
                }
            }
            literal("4") {
                runs {
                    this.default()
                    this.inverted()
                }
            }
            literal("5") {
                runs {
                    this.default()
                    this.aimbot()
                }
            }
            literal("6") {
                runs {
                    this.default()
                    this.ram()
                }
            }
            literal("7") {
                runs {
                    this.default()
                    this.meme()
                }
            }
            literal("8") {
                runs {
                    this.default()
                    this.medusa()
                }
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.helicopter() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_SHOVEL) {
            addEnchantment(helicopter.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_PICKAXE) {
            addEnchantment(helicopter.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("Tipp: du kannst auch einsteigen") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.bouncy() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.BOW) {
            addEnchantment(bouncy.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.ARROW, 64) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.slots() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_SWORD) {
            addEnchantment(EnchantmentRegistry.slots.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.VILLAGER_SPAWN_EGG, 64) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.squish() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.squish.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.IRON_GOLEM_SPAWN_EGG, 64) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.glitch() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.LEATHER_BOOTS) {
            addEnchantment(EnchantmentRegistry.glitch.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("Tipp: sneaken") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.medusa() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(EnchantmentRegistry.medusa.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(EnchantmentRegistry.medusa.getEntry(player.world), 2)
        })
        player.giveItemStack(itemStack(Items.PARROT_SPAWN_EGG, 64) {})

        player.sendMessage(literalText {
            text("Tipp: die entities müssen dich anschauen") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.meme() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.meme.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.PIG_SPAWN_EGG, 1) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.ram() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.SHIELD) {
            addEnchantment(EnchantmentRegistry.ram.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.PIG_SPAWN_EGG, 64) {})

        player.sendMessage(literalText {
            text("Tipp: gedrückt halten") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.aimbot() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.BOW) {
            addEnchantment(EnchantmentRegistry.aimbot.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.ARROW, 64) {})
        player.giveItemStack(itemStack(Items.ARROW, 64) {})
        player.giveItemStack(itemStack(Items.ARROW, 64) {})

        player.giveItemStack(itemStack(Items.SHEEP_SPAWN_EGG, 64) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.roll() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.LEATHER_LEGGINGS) {
            addEnchantment(EnchantmentRegistry.rolling.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("Tipp: sneaken zum togglen") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.inverted() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_BOOTS) {
            addEnchantment(EnchantmentRegistry.inverted.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_LEGGINGS) {
            addEnchantment(EnchantmentRegistry.inverted.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(EnchantmentRegistry.inverted.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.inverted.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.VILLAGER_SPAWN_EGG, 64) {
        })
    }


    private fun <S : ServerCommandSource> CommandContext<S>.colossal() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.colossal.getEntry(player.world), 3)
        })
        player.inventory.setStack(3, itemStack(Items.DIAMOND_PICKAXE) {
            addEnchantment(EnchantmentRegistry.colossal.getEntry(player.world), 1)
        })
        player.inventory.setStack(5, itemStack(Items.DIAMOND_SHOVEL) {
            addEnchantment(EnchantmentRegistry.colossal.getEntry(player.world), 4)
        })
        player.inventory.setStack(7, itemStack(Items.NETHERITE_AXE) {
            addEnchantment(EnchantmentRegistry.colossal.getEntry(player.world), 1)
        })
        player.inventory.setStack(8, itemStack(Items.OAK_LOG, 64) {
            addEnchantment(EnchantmentRegistry.colossal.getEntry(player.world), 1)
        })
    }


    private fun <S : ServerCommandSource> CommandContext<S>.balloon() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.balloon.getEntry(player.world), 1)
        })

        player.giveItemStack(itemStack(Items.PIG_SPAWN_EGG) {})
        player.giveItemStack(itemStack(Items.VILLAGER_SPAWN_EGG) {})
        player.giveItemStack(itemStack(Items.DONKEY_SPAWN_EGG) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.trash() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.LEATHER_LEGGINGS) {
            addEnchantment(EnchantmentRegistry.trashEnchantment.getEntry(player.world), 1)
        })
        repeat(16) {
            player.giveItemStack(itemStack(Registries.ITEM.get(Random.nextInt(Registries.ITEM.size()))) {})
        }

        player.sendMessage(literalText {
            text("Tipp: sneaken zum togglen und dann items droppen") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
            }
        })
    }


    private fun <S : ServerCommandSource> CommandContext<S>.dopamin() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(dopamin.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(dopamin.getEntry(player.world), 2)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(dopamin.getEntry(player.world), 3)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_HELMET) {
            addEnchantment(dopamin.getEntry(player.world), 4)
        })
    }

    private fun <S : ServerCommandSource> CommandContext<S>.verification() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_SWORD) {
            addEnchantment(verification.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.VILLAGER_SPAWN_EGG, 64) {})
    }

    private fun <S : ServerCommandSource> CommandContext<S>.hot() {
        val player = this.source.playerOrThrow

        repeat(9) {
            player.giveItemStack(itemStack(Items.IRON_SWORD) {
                addEnchantment(EnchantmentRegistry.hot.getEntry(player.world), 1)
            })
        }
    }


    private fun <S : ServerCommandSource> CommandContext<S>.slippery() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.DIAMOND_BOOTS) {
            addEnchantment(slippery.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.DIAMOND_BOOTS) {
            addEnchantment(slippery.getEntry(player.world), 4)
        })
        player.giveItemStack(itemStack(Items.COW_SPAWN_EGG, 64) {})
        player.giveItemStack(itemStack(Items.PIG_SPAWN_EGG, 64) {})
        player.giveItemStack(itemStack(Items.VILLAGER_SPAWN_EGG, 64) {})

        player.sendMessage(literalText {
            text("Tipp: click mal hier drauf") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                clickEvent = ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/item replace entity @e armor.feet with minecraft:diamond_boots[enchantments={levels:{'enchantments:slippery':1}}]"
                )
            }
        })
    }


    private fun RegistryKey<Enchantment>.getEntry(world: World): RegistryEntry<Enchantment> {
        return world.registryManager.get(RegistryKeys.ENCHANTMENT).getEntry(this.value).get()
    }

    private fun <S : ServerCommandSource> CommandContext<S>.default() {
        val world = this.source.world
        val server = this.source.server
        world.gameRules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server)
        world.gameRules.get(GameRules.DO_WEATHER_CYCLE).set(false, server)
        world.timeOfDay = 6000
        world.resetWeather()
        val player = this.source.playerOrThrow
        player.heal(player.maxHealth)
        player.hungerManager.saturationLevel = 2000f
        player.hungerManager.foodLevel = 2000
        player.inventory.clear()
        player.changeGameMode(GameMode.SURVIVAL)
    }
}
