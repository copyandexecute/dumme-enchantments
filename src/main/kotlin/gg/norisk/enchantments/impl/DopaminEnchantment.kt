package gg.norisk.enchantments.impl

import gg.norisk.enchantments.EnchantmentRegistry.dopamin
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.logger
import gg.norisk.enchantments.StupidEnchantments.toId
import gg.norisk.enchantments.sound.GoofyAhSoundInstance
import gg.norisk.enchantments.utils.GifUtils
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.EQUIPMENT_CHANGE
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.network.packet.s2cPacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.imageio.ImageIO

object DopaminEnchantment {
    val gifs = mutableSetOf<Gif>()
    var goofyAhSound: GoofyAhSoundInstance? = null

    data class Gif(val frames: Int, val baseIdentifier: Identifier, val width: Int, val height: Int) {
        private var currentFrame = 0
        private var nextUpdate: Long = 0

        fun update() {
            if (System.currentTimeMillis() > nextUpdate) {
                nextUpdate = System.currentTimeMillis() + 30
                currentFrame++
                if (currentFrame >= frames) {
                    currentFrame = 0
                }
            }
        }

        val texture get() = "${baseIdentifier.path}_$currentFrame".toId()
    }

    fun initClient() {
        startOrStopGoofyBeat.receiveOnClient { packet, context ->
            mcCoroutineTask(sync = true, client = true) {
                val player = context.client.world?.getEntityById(packet) as? PlayerEntity? ?: return@mcCoroutineTask
                if (player == MinecraftClient.getInstance().player) {
                    goofyAhSound?.stop()
                    goofyAhSound = GoofyAhSoundInstance(player)
                    MinecraftClient.getInstance().soundManager.play(goofyAhSound)
                }
            }
        }

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(object : IdentifiableResourceReloadListener {
                override fun getFabricId(): Identifier {
                    return "gifs".toId()
                }

                override fun reload(
                    synchronizer: ResourceReloader.Synchronizer,
                    manager: ResourceManager,
                    prepareProfiler: Profiler,
                    applyProfiler: Profiler,
                    prepareExecutor: Executor,
                    applyExecutor: Executor
                ): CompletableFuture<Void> {
                    return CompletableFuture.runAsync {
                        loadGifs(manager)
                    }.thenCompose(synchronizer::whenPrepared).thenAcceptAsync(
                        {
                        }, applyExecutor
                    )
                }
            })

        HudRenderCallback.EVENT.register(HudRenderCallback { drawContext, tickCounter ->
            val player = MinecraftClient.getInstance().player ?: return@HudRenderCallback
            val level = dopamin.getLevel(player.getEquippedStack(EquipmentSlot.HEAD)) ?: return@HudRenderCallback
            gifs.forEach(Gif::update)
            for (gif in gifs.take(level)) {
                val width = gif.width / 2
                val height = gif.height / 2

                val x = when (gif.baseIdentifier.path) {
                    "gifs/subwaysurfers.gif" -> {
                        MinecraftClient.getInstance().window.scaledWidth - width
                    }

                    "gifs/dino.gif" -> {
                        MinecraftClient.getInstance().window.scaledWidth - width
                    }

                    else -> 0
                }

                val y = when (gif.baseIdentifier.path) {
                    "gifs/dino.gif" -> {
                        MinecraftClient.getInstance().window.scaledHeight - height
                    }

                    "gifs/lego.gif" -> {
                        MinecraftClient.getInstance().window.scaledHeight - height
                    }

                    else -> 0
                }

                drawContext.drawTexture(gif.texture, x, y, 0f, 0f, width, height, width, height)
            }
        })
    }

    fun initServer() {
        EQUIPMENT_CHANGE.register(ServerEntityEvents.EquipmentChange { livingEntity, equipmentSlot, previousStack, currentStack ->
            if (equipmentSlot != EquipmentSlot.HEAD) return@EquipmentChange
            startOrStopGoofyBeat.sendToAll(livingEntity.id)
        })
    }

    val startOrStopGoofyBeat = s2cPacket<Int>("start-or-stop-goofy-beat".toId())

    // Jup ich hoffe das macht keine Probleme grüße
    fun loadGifs(
        resourceManager: ResourceManager,
    ) {
        gifs.clear()
        resourceManager.findResources("gifs") { it.toString().endsWith(".gif") }.forEach { identifier, resource ->
            val frames = GifUtils.readGif(resource.inputStream)

            logger.info("Loading Gif $identifier")

            for ((index, imageFrame) in frames.withIndex()) {
                val id = "${identifier.path}_$index".toId()
                val baos = ByteArrayOutputStream()
                ImageIO.write(imageFrame.image, "png", baos)
                baos.flush()
                MinecraftClient.getInstance().textureManager.registerTexture(
                    id, NativeImageBackedTexture(NativeImage.read(ByteArrayInputStream(baos.toByteArray())))
                )
            }

            gifs.add(Gif(frames.size, identifier, frames.random().width, frames.random().height))
        }
    }
}
