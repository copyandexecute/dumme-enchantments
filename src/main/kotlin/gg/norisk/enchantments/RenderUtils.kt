package gg.norisk.enchantments

import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object RenderUtils {
    fun renderBlock(matrixStack: MatrixStack, pos: Vec3d, state: BlockState, blockPos: BlockPos) {
        val camera = MinecraftClient.getInstance().gameRenderer.camera
        val renderer = MinecraftClient.getInstance().blockRenderManager
        val world = MinecraftClient.getInstance().world ?: return
        val vertexConsumer = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers.getBuffer(
            RenderLayers.getBlockLayer(state)
        )

        matrixStack.push()
        matrixStack.translate(
            pos.x - camera.getPos().x,
            pos.y - camera.getPos().y,
            pos.z - camera.getPos().z
        )
        renderer.renderBlock(
            state,
            blockPos,
            world,
            matrixStack,
            vertexConsumer,
            true,
            net.minecraft.util.math.random.Random.create()
        )
        matrixStack.pop()
    }
}
