package gg.norisk.enchantments.impl

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import gg.norisk.enchantments.EnchantmentRegistry.colossal
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.RaycastContext
import net.minecraft.world.RaycastContext.FluidHandling
import net.minecraft.world.World
import net.silkmc.silk.core.Silk.server
import net.silkmc.silk.core.server.players
import org.apache.logging.log4j.util.BiConsumer


object ColossalEnchantment {
    interface ColossalPlayer {
        var stupid_lastSide: Direction?
    }

    fun initServer() {
        PlayerBlockBreakEvents.AFTER.register(PlayerBlockBreakEvents.After { world, player, pos, state, blockEntity ->
            val scale = colossal.getLevel(player.mainHandStack) ?: return@After
            getSurroundingBlocks(
                pos,
                player.world,
                scale,
                (player as ColossalPlayer).stupid_lastSide ?: return@After
            ) { newPos, newState ->
                world.breakBlock(newPos, true, player)
            }
        })
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
    }

    fun scaleItem(
        itemStack: ItemStack,
        modelTransformationMode: ModelTransformationMode,
        bl: Boolean,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int,
        j: Int,
        bakedModel: BakedModel
    ) {
        var scale = colossal.getLevel(itemStack)?.toFloat() ?: return
        scale *= 3
        matrixStack.scale(scale, scale, scale)
    }

    fun drawBiggerBlockOutline(
        instance: WorldRenderer,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        entity: Entity,
        d: Double,
        e: Double,
        f: Double,
        blockPos: BlockPos,
        blockState: BlockState,
        original: Operation<Void>
    ) {
        val player = MinecraftClient.getInstance().player ?: return
        val blockHit = MinecraftClient.getInstance().crosshairTarget as? BlockHitResult? ?: return
        original.call(instance, matrixStack, vertexConsumer, entity, d, e, f, blockPos, blockState)

        val size = colossal.getLevel(player.mainHandStack) ?: return
        // Die Seite, die getroffen wurde
        getSurroundingBlocks(blockPos, entity.world, size, blockHit.side) { pos, state ->
            original.call(
                instance, matrixStack, vertexConsumer, entity, d, e, f, pos, state
            )
        }
    }

    fun getSurroundingBlocks(
        blockPos: BlockPos, world: World, size: Int, side: Direction,  // Die Richtung, in die der Strahl verläuft
        callBack: BiConsumer<BlockPos, BlockState>
    ) {
        // Schleife über alle x, y, z Offsets
        for (x in -size..size) {
            for (y in -size..size) {
                for (z in -size..size) {
                    // Berechne die neue Blockposition relativ zur Trefferposition
                    val newBlockPos = blockPos.add(x, y, z).offset(side, -size)

                    // Stelle sicher, dass wir nicht den zentralen Block (den ursprünglich getroffenen Block) erneut rendern
                    //if (newBlockPos == blockPos) continue

                    // Hole den Blockzustand der neuen Position
                    val newBlockState = world.getBlockState(newBlockPos) ?: continue

                    // Callback für die neue Blockposition
                    callBack.accept(newBlockPos, newBlockState)
                }
            }
        }
    }

    fun raycast(world: World, playerEntity: PlayerEntity, fluidHandling: FluidHandling): BlockHitResult {
        val vec3d = playerEntity.eyePos
        val vec3d2 = vec3d.add(
            playerEntity.getRotationVector(playerEntity.pitch, playerEntity.yaw)
                .multiply(playerEntity.blockInteractionRange)
        )
        return world.raycast(
            RaycastContext(
                vec3d, vec3d2, RaycastContext.ShapeType.OUTLINE, fluidHandling, playerEntity
            )
        )
    }

    fun spawnBlockBreakingInfos(i: Int, blockPos: BlockPos, j: Int, isClient: Boolean) {
        if (isClient) {
            val player = MinecraftClient.getInstance().player ?: return
            if (player.id != i) return
            val scale = colossal.getLevel(player.mainHandStack) ?: return
            val blockHit = MinecraftClient.getInstance().crosshairTarget as? BlockHitResult? ?: return
            getSurroundingBlocks(blockPos, player.world, scale, blockHit.side) { pos, state ->
                MinecraftClient.getInstance().worldRenderer.setBlockBreakingInfo(pos.hashCode(), pos, j)
            }
        } else {
            val player = server?.players?.firstOrNull { it.id == i } ?: return

            val scale = colossal.getLevel(player.mainHandStack) ?: return
            val raycast = raycast(player.world, player, FluidHandling.WATER)
            (player as ColossalPlayer).stupid_lastSide = raycast.side

            getSurroundingBlocks(blockPos, player.world, scale, raycast.side) { pos, state ->
                for (serverPlayerEntity in server?.players ?: emptyList()) {
                    if (serverPlayerEntity == player) continue
                    if (serverPlayerEntity.world == player.world) {
                        val d = pos.x.toDouble() - serverPlayerEntity.x
                        val e = pos.y.toDouble() - serverPlayerEntity.y
                        val f = pos.z.toDouble() - serverPlayerEntity.z
                        if (d * d + e * e + f * f < 1024.0) {
                            serverPlayerEntity.networkHandler.sendPacket(
                                BlockBreakingProgressS2CPacket(
                                    pos.hashCode(),
                                    pos,
                                    j
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun multiplyDamage(
        original: Float,
        entity: Entity,
        f: Float,
        damageSource: DamageSource,
        player: ServerPlayerEntity
    ): Float {
        val scale = colossal.getLevel(player.mainHandStack) ?: return original
        return original * (scale * 3)
    }
}
