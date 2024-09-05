package gg.norisk.enchantments.utils

import gg.norisk.enchantments.impl.TrashEnchantment.isTrash
import gg.norisk.enchantments.impl.TrashEnchantment.trashItems
import gg.norisk.enchantments.mixin.client.ModelPartAccessor
import net.minecraft.block.Blocks
import net.minecraft.client.model.ModelPart
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.random.Random

//Credits an https://github.com/chyzman/wearThat/blob/master/src/main/java/com/chyzman/wearthat/client/WearThatClient.java
class TrashItemsFeatureRenderer<T : LivingEntity, M : EntityModel<T>>(
    context: FeatureRendererContext<T, M>,
    private val heldItemRenderer: HeldItemRenderer,
    val root: ModelPart,
) : FeatureRenderer<T, M>(context) {
    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        entity: T,
        limbAngle: Float,
        limbDistance: Float,
        tickDelta: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        if (!entity.isTrash) return

        val legs = (root as ModelPartAccessor).children.filter {
            it.key.contains("leg") || it.key.contains("tentacle") || it.key.contains("rod")
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
                        it.renderTrash(matrices, entity, vertexConsumers, light)
                        return
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
                            it.renderTrash(matrices, entity, vertexConsumers, light)
                        }
                    }
                }
            }
            current = current.superclass
        }
    }

    fun compareCuboids(cuboid1: ModelPart.Cuboid, cuboid2: ModelPart.Cuboid): Boolean {
        return cuboid1.minX == cuboid2.minX &&
                cuboid1.minY == cuboid2.minY &&
                cuboid1.minZ == cuboid2.minZ &&
                cuboid1.maxX == cuboid2.maxX &&
                cuboid1.maxY == cuboid2.maxY &&
                cuboid1.maxZ == cuboid2.maxZ
    }

    private fun ModelPart.renderTrash(
        matrices: MatrixStack,
        entity: T,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        if (!entity.isTrash) return

        matrices.push()
        rotate(matrices)

        var size = 0.0

        forEachCuboid(matrices) { entry, string, i, cuboid ->
            size = cuboid.maxY.toDouble()
        }

        val scale = 1f
        matrices.scale(scale, scale, scale)

        matrices.translate(0.0, size / 36, 0.0) // Position anpassen
        matrices.translate(-0.1, -0.7, 0.0) // Position anpassen

        for ((index, trash) in entity.trashItems.trash.withIndex()) {
            //val legHeight = it.cuboids.firstOrNull()?.dimensions?.y?.toDouble() ?: 0.0

            matrices.translate(0.0, (index * -0.001), 0.0) // Position anpassen


            heldItemRenderer.renderItem(
                entity as LivingEntity,
                trash,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                light
            )
        }

        matrices.pop()
    }
}
