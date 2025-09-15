package gg.norisk.enchantments.impl.fork

import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.impl.schleuder.SchleuderEnchantment.toRadian
import gg.norisk.enchantments.utils.EntityTypeSerializer
import gg.norisk.utils.ext.EntityRenderStateExt
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.render.entity.state.TridentEntityRenderState
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.client.render.item.ItemRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SpawnEggItem
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundEvents
import net.minecraft.text.HoverEvent
import net.minecraft.util.ActionResult
import net.minecraft.util.Arm
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.world.World
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import net.silkmc.silk.nbt.set
import org.joml.Quaternionf
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.Color
import kotlin.jvm.optionals.getOrNull
import kotlin.math.pow
import kotlin.random.Random
import net.minecraft.registry.Registries
import net.silkmc.silk.commands.PermissionLevel

object ForkEnchantment {

    @Serializable
    data class ForkedEntity(
        @Serializable(with = EntityTypeSerializer::class) val entityType: EntityType<*>,
        val customYaw: Float = Random.nextDouble(0.0, 360.0).toFloat(),
        val customPitch: Float = Random.nextDouble(0.0, 360.0).toFloat(),
        val customRoll: Float = Random.nextDouble(0.0, 360.0).toFloat(),
    )

    const val FORK_NBT_KEY = "Fork"

    fun initServer() {
        UseItemCallback.EVENT.register { player, world, hand ->
            val stack = player.getStackInHand(hand)
            if (stack.isOf(Items.TRIDENT) && stack.getForkedEntities()
                    .isNotEmpty() && EnchantmentRegistry.fork.getLevel(stack) != null
            ) {
                if (player.isSneaking) {
                    player.nrc_isEatingFork = true
                    player.nrc_nextForkEatingTime = player.age + Random.nextInt(10, 30)
                    //player.sendMessage("START".literal, false)
                }
            }
            return@register ActionResult.PASS
        }

        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("fork") {
                runs {
                    this.default()
                    this.fork()
                }
                if (FabricLoader.getInstance().isDevelopmentEnvironment) {
                    literal("modifynbttest") {
                        runs {
                            val player = this.source.playerOrThrow
                            val itemInHand = player.mainHandStack
                            addForkedEntity(itemInHand)
                        }
                    }
                }
            }
        }
    }

    fun addForkedEntity(itemStack: ItemStack) {
        val component = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
        val forkedEntities = itemStack.getForkedEntities()
        itemStack.set(DataComponentTypes.CUSTOM_DATA, component.apply {
            //val randomEntity = Registries.ENTITY_TYPE.get(Random.nextInt(Registries.ENTITY_TYPE.ids.size))
            val randomEntity = EntityType.PIG
            forkedEntities.add(ForkedEntity(randomEntity))
            it.set(FORK_NBT_KEY, Json.encodeToString(forkedEntities))
        })
    }

    fun ItemStack.getForkedEntities(): MutableList<ForkedEntity> {
        val component = getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
        val currentValue = component.nbt.getString(FORK_NBT_KEY).getOrNull()
        return if (currentValue != null) runCatching<MutableList<ForkedEntity>> { Json.decodeFromString(currentValue) }.getOrDefault(
            mutableListOf()
        ) else mutableListOf()
    }

    fun handleFirstPersonRendering(
        player: AbstractClientPlayerEntity,
        tickProgress: Float,
        pitch: Float,
        hand: Hand,
        swingProgress: Float,
        itemStack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        if (!itemStack.isOf(Items.TRIDENT)) return

        val data = itemStack.getForkedEntities()
        if (data.isEmpty()) return

        for (forkedEntity in data) {
            //println("ItemStack: ${data.copyNbt().get("fork")}")

            val client = MinecraftClient.getInstance()

            // Get the entity renderer
            val entityType = forkedEntity.entityType
            val fakeEntity = entityType.create(player.world, SpawnReason.MOB_SUMMONED) ?: continue
            val entityRenderDispatcher = client.entityRenderDispatcher
            val entityRenderer =
                entityRenderDispatcher.getRenderer(fakeEntity) as? EntityRenderer<Entity, EntityRenderState> ?: continue

            matrices.push()


            setupArrowTransformation(
                matrices,
                fakeEntity,
                forkedEntity,
                false
            )

            // Create render state for the entity
            val renderState = entityRenderer.createRenderState()
            //val fakePlayer = FakePlayer(MinecraftClient.getInstance().world!!, player.gameProfile, player)
            entityRenderer.updateRenderState(fakeEntity, renderState, tickProgress)
            (renderState as? LivingEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }
            (renderState as? PlayerEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }

            // Render the entity
            entityRenderer.render(renderState, matrices, vertexConsumers, light)

            matrices.pop()
        }
    }

    private fun setupArrowTransformation(
        matrices: MatrixStack,
        entity: Entity,
        forkedEntity: ForkedEntity,
        isThirdPerson: Boolean,
        isTridentEntity: Boolean = false
    ) {
        // Scale first (affects all subsequent transformations)
        val baseScale = 0.3f
        matrices.scale(baseScale, baseScale, baseScale)
        if (!isThirdPerson) {
            matrices.translate(1f, 1.3f, -0.8f)
        } else {
            matrices.translate(0f, 1.9f, 0f)
            if (isTridentEntity) {
                matrices.translate(0f, -3f, 0f)
            }
        }

        // === ROTATION AROUND ENTITY CENTER ===
        // Translate to entity center (Player height is ~1.8 blocks, so center is at 0.9)
        val entityHeight = entity.height
        matrices.translate(0f, entityHeight / 2f, 0f)

        // Here you can add custom rotations around the entity's center
        // Example rotations (you can modify these values):
        val time = System.currentTimeMillis()
        val timeSeconds = (time % 10000) / 1000.0f  // Convert to seconds, cycle every 10 seconds

        val customRotationTest = (timeSeconds * 360f / 1f) % 360f

        val customYaw = forkedEntity.customYaw
        val customPitch = forkedEntity.customPitch
        val customRoll = forkedEntity.customRoll

        if (customYaw != 0f || customPitch != 0f || customRoll != 0f) {
            matrices.multiply(
                Quaternionf().rotateXYZ(
                    toRadian(customPitch), toRadian(customYaw), toRadian(customRoll)
                )
            )
        }

        // Translate back from center
        matrices.translate(0f, -entityHeight / 2f, 0f)
    }

    private fun <S : ServerCommandSource> CommandContext<S>.fork() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.TRIDENT) {
            addEnchantment(EnchantmentRegistry.fork.getEntry(player.world), 1)
        })
        player.inventory.setStack(35, itemStack(Items.TRIDENT) {
            addEnchantment(EnchantmentRegistry.fork.getEntry(player.world), 1)
            addEnchantment(Enchantments.LOYALTY.getEntry(player.world), 1)
        })

        // Get all spawn eggs dynamically from registry
        val spawnEggs = Registries.ITEM.stream()
            .filter { item -> item is SpawnEggItem }
            .toList()

        // Fill slots 1-8 (remaining hotbar slots) with random spawn eggs
        for (slot in 1..8) {
            val randomEgg = spawnEggs.random()
            player.inventory.setStack(slot, itemStack(randomEgg) {
                count = 64
            })
        }

        player.sendMessage(literalText {
            text("§b§lFork Enchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Zuerst sneaken dann rechtsklick zum essen der Mobs".literal)
            }
        })
    }

    fun HeldItemRenderer.handleEatingAnimation(
        original: Boolean,
        player: AbstractClientPlayerEntity,
        tickProgress: Float,
        pitch: Float,
        hand: Hand,
        swingProgress: Float,
        item: ItemStack,
        equipProgress: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ): Boolean {
        if (item.isOf(Items.TRIDENT) && player.nrc_isEatingFork) {
            val bl = hand == Hand.MAIN_HAND
            val arm = if (bl) player.mainArm else player.mainArm.opposite
            applyEquipOffsetForTrident(matrices, arm, equipProgress)
            applyEatOrDrinkTransformationForTrident(matrices, tickProgress, arm, item, player)
            return false
        }
        return original
    }

    fun applyEquipOffsetForTrident(matrices: MatrixStack, arm: Arm?, equipProgress: Float) {
        val i = if (arm == Arm.RIGHT) 1 else -1
        matrices.translate(0.3f, -0.34f, 0f)
        //matrices.translate(i * 0.56f, -0.52f + equipProgress, -0.72f)
    }

    fun applyEatOrDrinkTransformationForTrident(
        matrices: MatrixStack,
        tickProgress: Float,
        arm: Arm?,
        stack: ItemStack,
        player: PlayerEntity
    ) {
        // Calculate original animation values
        val originalF = (player.getItemUseTimeLeft() - stack.getMaxUseTime(player)) - tickProgress + 1.0f
        val originalG = originalF / stack.getMaxUseTime(player)

        val f: Float = originalF
        val g: Float = originalG

        // Apply the same logic as before, but with our modified f/g values
        if (g < 0.8f) {
            val h = MathHelper.abs(MathHelper.cos(f / 4.0f * Math.PI.toFloat()) * 0.1f)
            matrices.translate(0.0f, h, 0.0f)
        }

        val h = 1.0f - g.toDouble().pow(27.0).toFloat()
        val i = if (arm == Arm.RIGHT) 1 else -1
        matrices.translate(h * 0.6f * i, h * -0.5f, h * 0.0f)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0f))
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0f))
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0f))
    }

    fun <S : EntityRenderState> handleThirdPersonRendering(
        entityState: S,
        itemState: ItemRenderState,
        arm: Arm,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val player = (entityState as? EntityRenderStateExt?)?.nrc_entity as? PlayerEntity? ?: return
        val hand = if (arm == Arm.RIGHT) Hand.MAIN_HAND else Hand.OFF_HAND
        val itemStack = player.getStackInHand(hand)
        val data = itemStack.getForkedEntities()
        val tickProgress = MinecraftClient.getInstance().renderTickCounter.getTickProgress(false)

        for (forkedEntity in data) {
            //println("ItemStack: ${data.copyNbt().get("fork")}")

            val client = MinecraftClient.getInstance()

            // Get the entity renderer
            val entityType = forkedEntity.entityType
            val fakeEntity = entityType.create(player.world, SpawnReason.MOB_SUMMONED) ?: continue
            val entityRenderDispatcher = client.entityRenderDispatcher
            val entityRenderer =
                entityRenderDispatcher.getRenderer(fakeEntity) as? EntityRenderer<Entity, EntityRenderState> ?: continue

            matrices.push()


            if (player.isUsingItem) {
                matrices.translate(0f, -1.4f, 0f)
            }

            setupArrowTransformation(matrices, fakeEntity, forkedEntity, true)

            // Create render state for the entity
            val renderState = entityRenderer.createRenderState()
            //val fakePlayer = FakePlayer(MinecraftClient.getInstance().world!!, player.gameProfile, player)
            entityRenderer.updateRenderState(fakeEntity, renderState, tickProgress)
            (renderState as? LivingEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }
            (renderState as? PlayerEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }

            // Render the entity
            entityRenderer.render(renderState, matrices, vertexConsumers, light)

            matrices.pop()
        }
    }

    fun LivingEntity.tickItemUsage() {
        val player = (this as? PlayerEntity?) ?: return
        if (activeItem.isOf(Items.TRIDENT) && nrc_isEatingFork) {
            val forkedEntities = activeItem.getForkedEntities()
            if (forkedEntities.isNotEmpty()) {
                if (player.age.mod(4) == 0) {
                    spawnParticlesAndPlaySound(random, this, activeItem, 5)
                }
                if (age >= nrc_nextForkEatingTime) {
                    player.nrc_nextForkEatingTime = player.age + Random.nextInt(10, 30)
                    val forkedEntity = forkedEntities.removeLast()
                    val entity = forkedEntity.entityType.create(world, SpawnReason.MOB_SUMMONED) ?: return
                    val itemStack = activeItem
                    val component = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                    itemStack.set(DataComponentTypes.CUSTOM_DATA, component.apply {
                        it.set(FORK_NBT_KEY, Json.encodeToString(forkedEntities))
                    })
                    player.hungerManager.add(4, 4f)
                    player.sound(SoundEvents.ENTITY_PLAYER_BURP, pitch = random.nextTriangular(1.0f, 0.2f))
                    val livingEntity = entity as? LivingEntity?
                    if (livingEntity != null) {
                        val deahtSound = livingEntity.deathSound!!
                        player.sound(deahtSound, volume = 0.8f, pitch = random.nextTriangular(1.0f, 0.2f))
                    }
                    if (forkedEntities.isEmpty()) {
                        stopUsingItem()
                    }
                }
            }
        } else {
            if (nrc_isEatingFork) {
                nrc_isEatingFork = false
                //(this as? PlayerEntity?)?.sendMessage("STOP".literal, false)
            }
        }
        //println("Ticking: $stack")
    }

    fun spawnParticlesAndPlaySound(
        random: net.minecraft.util.math.random.Random,
        user: LivingEntity,
        stack: ItemStack?,
        particleCount: Int
    ) {
        val f = if (random.nextBoolean()) 0.5f else 1.0f
        val g = random.nextTriangular(1.0f, 0.2f)
        val j = f
        val k = g
        if (true) {
            user.spawnItemParticles(Items.PORKCHOP.defaultStack, particleCount)
        }

        val soundEvent = SoundEvents.ENTITY_GENERIC_EAT.value()
        user.playSound(soundEvent, j, k)
    }

    var Entity.nrc_isEatingFork: Boolean
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_isEatingFork") ?: false
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_isEatingFork", value)
        }
    var Entity.nrc_nextForkEatingTime: Int
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_nextForkEatingTime") ?: 0
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_nextForkEatingTime", value)
        }
    var TridentEntity.nrc_forkedEntities: String?
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_forkedEntities")
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_forkedEntities", value)
        }


    fun onStoppedUsing(
        stack: ItemStack,
        world: World,
        user: LivingEntity,
        remainingUseTicks: Int,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        val player = user as? PlayerEntity? ?: return
        if (player.nrc_isEatingFork) {
            cir.returnValue = false
            return
        }
        player.nrc_isEatingFork = false
        //player.sendMessage("STOP".literal, false)
    }

    fun TridentEntity.onEntityHit(entityHitResult: EntityHitResult, ci: CallbackInfo) {
        if (EnchantmentRegistry.fork.getLevel(this.itemStack) == null) return
        val forkedEntities = this.itemStack.getForkedEntities()
        val component = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
        forkedEntities.add(ForkedEntity(entityHitResult.entity.type))
        itemStack.set(DataComponentTypes.CUSTOM_DATA, component.apply {
            it.set(FORK_NBT_KEY, Json.encodeToString(forkedEntities))
        })
        this.nrc_forkedEntities = Json.encodeToString(forkedEntities)
        if (entityHitResult.entity.type != EntityType.PLAYER) {
            entityHitResult.entity.discard()
        }
    }

    fun handleTridentEntityRendering(
        tridentEntityRenderState: TridentEntityRenderState,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val tridentEntity =
            (tridentEntityRenderState as? EntityRenderStateExt?)?.nrc_entity as? TridentEntity? ?: return
        if (tridentEntity.nrc_forkedEntities == null) {
            return
        }
        val data = runCatching<MutableList<ForkedEntity>> {
            Json.decodeFromString(tridentEntity.nrc_forkedEntities ?: "")
        }.getOrDefault(mutableListOf())
        val tickProgress = MinecraftClient.getInstance().renderTickCounter.getTickProgress(false)

        for (forkedEntity in data) {
            val client = MinecraftClient.getInstance()
            // Get the entity renderer
            val entityType = forkedEntity.entityType
            val fakeEntity = entityType.create(tridentEntity.world, SpawnReason.MOB_SUMMONED) ?: continue
            val entityRenderDispatcher = client.entityRenderDispatcher
            val entityRenderer =
                entityRenderDispatcher.getRenderer(fakeEntity) as? EntityRenderer<Entity, EntityRenderState> ?: continue

            matrices.push()
            setupArrowTransformation(matrices, fakeEntity, forkedEntity, true, isTridentEntity = true)

            // Create render state for the entity
            val renderState = entityRenderer.createRenderState()
            //val fakePlayer = FakePlayer(MinecraftClient.getInstance().world!!, player.gameProfile, player)
            entityRenderer.updateRenderState(fakeEntity, renderState, tickProgress)
            (renderState as? LivingEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }
            (renderState as? PlayerEntityRenderState?)?.apply {
                //relativeHeadYaw = player.headYaw
                relativeHeadYaw = 0f
                this.pitch = 0f
            }

            // Render the entity
            entityRenderer.render(renderState, matrices, vertexConsumers, light)
            matrices.pop()
        }
    }

    fun TridentEntity.onInitDataTracker() {
        if (!world.isClient) {
            val forkedEntities = this.itemStack?.getForkedEntities() ?: return
            this.nrc_forkedEntities = Json.encodeToString(forkedEntities)
        }
    }
}