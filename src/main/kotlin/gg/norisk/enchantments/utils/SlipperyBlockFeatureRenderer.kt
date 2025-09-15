package gg.norisk.enchantments.utils

import gg.norisk.enchantments.EnchantmentRegistry.slippery
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.mixin.client.ModelPartAccessor
import gg.norisk.utils.ext.EntityRenderStateExt
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemDisplayContext
import net.minecraft.util.math.random.Random

//Credits an https://github.com/chyzman/wearThat/blob/master/src/main/java/com/chyzman/wearthat/client/WearThatClient.java
class SlipperyBlockFeatureRenderer<S : EntityRenderState, M : EntityModel<S>>(
    featureRendererContext: FeatureRendererContext<S, M>,
    val root: ModelPart,
) :
    FeatureRenderer<S, M>(featureRendererContext) {

    fun compareCuboids(cuboid1: ModelPart.Cuboid, cuboid2: ModelPart.Cuboid): Boolean {
        return cuboid1.minX == cuboid2.minX &&
                cuboid1.minY == cuboid2.minY &&
                cuboid1.minZ == cuboid2.minZ &&
                cuboid1.maxX == cuboid2.maxX &&
                cuboid1.maxY == cuboid2.maxY &&
                cuboid1.maxZ == cuboid2.maxZ
    }

    private fun ModelPart.renderBlock(
        matrices: MatrixStack,
        entity: LivingEntity,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val level = slippery.getLevel(entity.getEquippedStack(EquipmentSlot.FEET)) ?: return

        matrices.push()
        applyTransform(matrices)

        var size = 0.0

        forEachCuboid(matrices) { entry, string, i, cuboid ->
            size = cuboid.maxY.toDouble()
        }

        matrices.translate(0.0, size / 18, 0.0) // Position anpassen
        matrices.translate(0.0, -0.1, 0.0) // Position anpassen

        //val legHeight = it.cuboids.firstOrNull()?.dimensions?.y?.toDouble() ?: 0.0
        val heldItemRenderer = MinecraftClient.getInstance().itemRenderer

        heldItemRenderer.renderItem(
            entity,
            Blocks.ICE.asItem().defaultStack,
            ItemDisplayContext.FIXED,
            matrices,
            vertexConsumers,
            entity.world,
            light,
            OverlayTexture.DEFAULT_UV,
            entity.getId() + ItemDisplayContext.FIXED.ordinal
        )
        matrices.pop()
    }

    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        state: S,
        limbAngle: Float,
        limbDistance: Float
    ) {
        val entity = (state as? EntityRenderStateExt?)?.nrc_entity as? LivingEntity? ?: return
        val level = slippery.getLevel(entity.getEquippedStack(EquipmentSlot.FEET)) ?: return

        val legs = (root as ModelPartAccessor).children.filter {
            it.key.contains("leg", true) || it.key.contains("tentacle", true) || it.key.contains("rod", true)
        }.map { it.value }

        var current: Class<*> = this.contextModel::class.java
        while (current.superclass != null) { // we don't want to process Object.class
            current.declaredFields.forEach { field ->
                runCatching {
                    field.isAccessible = true
                    field.get(this.contextModel) as ModelPart
                }.onSuccess {
                    val random = Random.create()
                    if (it.isEmpty) return@onSuccess
                    if (legs.any { leg -> compareCuboids(leg.getRandomCuboid(random), it.getRandomCuboid(random)) }) {
                        if (entity is PlayerEntity) {
                            if (it.hasChild("left_pants") || it.hasChild("right_pants")) {
                                it.renderBlock(matrices, entity, vertexConsumers, light)
                            }
                        } else {
                            it.renderBlock(matrices, entity, vertexConsumers, light)
                        }
                    }
                }
                runCatching {
                    field.isAccessible = true
                    field.get(this.contextModel) as Array<ModelPart>
                }.onSuccess { modelParts ->
                    for (it in modelParts) {
                        if (it.isEmpty) return@onSuccess
                        val random = Random.create()
                        if (legs.any { leg ->
                                compareCuboids(
                                    leg.getRandomCuboid(random),
                                    it.getRandomCuboid(random)
                                )
                            }) {
                            it.renderBlock(matrices, entity, vertexConsumers, light)
                        }
                    }
                }
            }

            current = current.superclass
        }
    }
}
