package gg.norisk.enchantments.impl

import gg.norisk.enchantments.EnchantmentRegistry.verification
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.toId
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.TextureComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.util.UISounds
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.silkmc.silk.commands.clientCommand
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import net.silkmc.silk.network.packet.s2cPacket
import java.awt.Color
import kotlin.random.Random

object VerificationEnchantment {
    val captchas = listOf(
        "textures/captchas/shrek.png".toId(),
        "textures/captchas/pflanzen.png".toId(),
        "textures/captchas/pigs.png".toId(),
        "textures/captchas/schafe.png".toId(),
        "textures/captchas/steve.png".toId(),
        "textures/captchas/villager.png".toId(),
    )

    fun initClient() {
        verificationScreenS2C.receiveOnClient { packet, context ->
            mcCoroutineTask(sync = true, client = true) {
                val entity = context.client.world?.getEntityById(packet) ?: return@mcCoroutineTask
                if (entity == MinecraftClient.getInstance().player) {
                    openCaptchaScreen()
                }
            }
        }
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        clientCommand("verification") {
            runs {
                openCaptchaScreen()
            }
        }
    }

    val verificationScreenS2C = s2cPacket<Int>("verification-screen".toId())

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        verification.getLevel(itemStack) ?: return
        val player = damageSource.attacker as? ServerPlayerEntity ?: return
        verificationScreenS2C.send(player.id, player)
    }

    private class VerificationScreen : BaseOwoScreen<FlowLayout>() {
        override fun createAdapter(): OwoUIAdapter<FlowLayout> {
            return OwoUIAdapter.create(this, Containers::verticalFlow);
        }

        override fun build(rootComponent: FlowLayout) {
            rootComponent.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            rootComponent.child(VerificationComponent(captchas.random()))
        }

        override fun shouldPause(): Boolean {
            return false
        }

        override fun shouldCloseOnEsc(): Boolean {
            return false
        }
    }

    private fun openCaptchaScreen() {
        mcCoroutineTask(sync = true, client = true, delay = 1.ticks) {
            MinecraftClient.getInstance().setScreen(VerificationScreen())
        }
    }

    private class VerificationComponent(
        val captcha: Identifier,
        horizontalSizing: Sizing = Sizing.fixed(140),
        verticalSizing: Sizing = Sizing.content()
    ) : FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
        init {
            val header = Containers.verticalFlow(Sizing.fill(), Sizing.content())
            header.padding(Insets.of(10))
            header.child(Components.label("Select all squares with".literal))
            header.child(Components.label(literalText {
                bold = true
                val text = captcha.path.substringAfterLast("/").substringBeforeLast(".png")
                text(text)
            }))
            header.child(Components.label("If there are none, click skip".literal))
            header.surface(Surface.flat(Color.decode("#51ABFF").rgb))

            child(header)
            gap(2)

            // Größe des gesamten Bildes
            val captchaSize = 128 // z.B. 128x128 Pixel großes Bild
            val gridSize = 4      // 4x4 Grid

            // Größe jedes Quadrats in Pixel
            val squareSize = captchaSize / gridSize

            val grid = Containers.grid(Sizing.fill(), Sizing.content(), gridSize, gridSize)

            repeat(gridSize) { row ->
                repeat(gridSize) { column ->
                    val xOffset = column * squareSize
                    val yOffset = row * squareSize

                    // Jedes Grid-Element wird mit einem Teil des Bildes gefüllt
                    grid.child(
                        ClickableImage(
                            Components.texture(
                                captcha,      // Das Bild
                                xOffset, yOffset,       // Position des Bildausschnitts im Bild
                                squareSize, squareSize, // Größe des Bildausschnitts
                                captchaSize, captchaSize // Größe des gesamten Bildes
                            )
                        ),
                        row, column
                    )
                }
            }
            child(grid)
            padding(Insets.of(2))
            surface(Surface.flat(Color.WHITE.rgb))

            val buttonWrapper = Containers.grid(Sizing.fill(), Sizing.content(), 1, 2)
            buttonWrapper.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            buttonWrapper.child(Containers.horizontalFlow(Sizing.fill(50), Sizing.content()).apply {
                gap(4)
                child(Components.label(literalText {
                    text("↺")
                    color = Color.LIGHT_GRAY.rgb
                }).apply {
                    mouseDown().subscribe { _, _, _ ->
                        UISounds.playButtonSound()
                        openCaptchaScreen()
                        return@subscribe true
                    }
                })
                child(Components.label(literalText {
                    text("\uD83C\uDFA7")
                    color = Color.LIGHT_GRAY.rgb
                }).apply {
                    mouseDown().subscribe { _, _, _ ->
                        val sound = Registries.SOUND_EVENT.get(Random.nextInt(Registries.SOUND_EVENT.size()))
                            ?: return@subscribe true
                        MinecraftClient.getInstance().soundManager.play(
                            PositionedSoundInstance.master(
                                sound,
                                1f
                            )
                        )
                        return@subscribe true
                    }
                })
                child(Components.label(literalText {
                    text("\uD83D\uDEC8")
                    color = Color.LIGHT_GRAY.rgb
                }).apply {
                    this.tooltip(Text.literal("bitti abo?"))
                })
            }, 0, 0)
            buttonWrapper.child(Containers.horizontalFlow(Sizing.fill(50), Sizing.content()).apply {
                horizontalAlignment(HorizontalAlignment.RIGHT)
                child(Components.button("Verify".literal) {
                    MinecraftClient.getInstance().currentScreen?.close()
                }.apply {
                    textShadow(false)
                    renderer(
                        ButtonComponent.Renderer.flat(
                            Color.decode("#51ABFF").rgb,
                            Color.decode("#51ABFF").brighter().rgb,
                            Color.decode("#51ABFF").darker().darker().rgb
                        )
                    )
                })
            }, 0, 1)

            child(buttonWrapper)
        }
    }

    private class ClickableImage(
        val image: TextureComponent,
        horizontalSizing: Sizing = Sizing.content(),
        verticalSizing: Sizing = Sizing.content()
    ) : FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
        var isSelected = false

        init {
            child(image)
            padding(Insets.of(1))
            mouseDown().subscribe { _, _, _ ->
                isSelected = !isSelected
                UISounds.playInteractionSound()
                if (isSelected) {
                    surface(Surface.outline(Color.RED.rgb))
                } else {
                    surface(Surface.BLANK)
                }

                return@subscribe true
            }
        }
    }
}
