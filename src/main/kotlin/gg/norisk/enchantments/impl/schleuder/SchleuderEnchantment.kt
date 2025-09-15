package gg.norisk.enchantments.impl.schleuder

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentRegistry.schleuder
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.sound.SchleuderSoundInstance
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.sound.StretchSoundInstance
import gg.norisk.enchantments.utils.Animation
import gg.norisk.utils.ext.EntityRenderStateExt
import me.x150.geckoAnimLib.core.ModelPartTransform
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.option.Perspective
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World
import net.silkmc.silk.commands.PermissionLevel
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import org.joml.Quaternionf
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.invoke.arg.Args
import java.awt.Color
import kotlin.math.PI
import kotlin.time.Duration.Companion.seconds

object SchleuderEnchantment {
    fun shouldRenderBow(player: AbstractClientPlayerEntity, hand: Hand, item: ItemStack): Boolean {
        // Check if the item has the schleuder enchantment
        return item.isOf(Items.BOW) && schleuder.getLevel(item) != null
    }

    fun initServer() {
        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("schleuder") {
                runs {
                    this.default()
                    this.schleuder()
                }
            }
        }
        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            if (world.isClient) {
                val itemStack = player.getStackInHand(hand)
                if (schleuder.getLevel(itemStack) != null) {
                    val client = MinecraftClient.getInstance()
                    val clientPlayer = client.player
                    if (player == clientPlayer) {
                        // Start the continuous stretch sound
                        client.soundManager.play(
                            StretchSoundInstance(
                                clientPlayer,
                                hand,
                                { schleuder.getLevel(clientPlayer.getStackInHand(hand)) != null },
                                SoundRegistry.SLINGTSHOT_STRETCH
                            )
                        )
                    }
                }
            }
            return@UseItemCallback ActionResult.PASS
        })
    }

    fun handleFirstPersonRendering(
        player: AbstractClientPlayerEntity,
        tickProgress: Float,
        pitch: Float,
        hand: Hand,
        swingProgress: Float,
        item: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val client = MinecraftClient.getInstance()

        // Get the entity renderer
        val entityRenderDispatcher = client.entityRenderDispatcher
        val entityRenderer =
            entityRenderDispatcher.getRenderer(PlayerEntityRenderState()) as? EntityRenderer<Entity, EntityRenderState>
                ?: return

        matrices.push()


        setupArrowTransformation(matrices, hand, swingProgress, tickProgress, item, player)

        // Create render state for the entity
        val renderState = entityRenderer.createRenderState()
        val fakePlayer = FakePlayer(MinecraftClient.getInstance().world!!, player.gameProfile, player)
        entityRenderer.updateRenderState(fakePlayer, renderState, tickProgress)
        (renderState as? PlayerEntityRenderState?)?.apply {
            //relativeHeadYaw = player.headYaw
            relativeHeadYaw = 0f
            this.pitch = player.pitch
        }

        // Render the entity
        entityRenderer.render(renderState, matrices, vertexConsumers, light)

        matrices.pop()
    }

    private fun setupArrowTransformation(
        matrices: MatrixStack,
        hand: Hand,
        swingProgress: Float,
        tickProgress: Float,
        item: ItemStack,
        player: PlayerEntity,
    ) {
        // Position like an arrow being drawn - same calculation as vanilla bow
        val useTime = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0f)
        var pullProgress = useTime / 20.0f
        pullProgress = (pullProgress * pullProgress + pullProgress * 2.0f) / 3.0f
        if (pullProgress > 1.0f) {
            pullProgress = 1.0f
        }

        val pullDistance = pullProgress * 0.3f // How far back the entity gets pulled

        // Position offset based on hand
        val handOffset = if (hand == Hand.MAIN_HAND) -0.1 else 0.1

        // Initial position
        matrices.translate(0f, 1.5f, 0f)

        val hasLaunchMultiplier = player.nrc_schleuderLaunchAnimation != null
        val yawRad = toRadian(-90f)
        val pitchRad = toRadian(20f)

        // Rotate to lie horizontally like an arrow, slightly angled up
        matrices.multiply(Quaternionf().rotateXYZ(pitchRad, yawRad, toRadian(90f)))

        // Calculate translation along the arrow direction (mathematical approach)
        val distance = 1.5f
        val rollRad = toRadian(90f)

        // Transform a forward vector (0,0,1) through the same rotations to get direction
        // After XYZ rotation: X=20°, Y=-90°, Z=90°
        // This gives us the direction the golem is "pointing" after rotation
        val dx = -kotlin.math.cos(pitchRad) * kotlin.math.sin(yawRad) * distance * 0.8f

        val launchMultiplier = 1.6f

        val dy = kotlin.math.sin(pitchRad) * distance * launchMultiplier
        val dz = -kotlin.math.cos(pitchRad) * kotlin.math.cos(yawRad) * distance

        // Move in opposite direction (backward along the arrow)
        matrices.translate(-dx, -dy, -dz)


        // Scale based on bow pull and base size
        val baseScale = 0.3f
        val pullScale = 1.0f - (pullProgress * 0.2f) // Slightly smaller when pulled back
        val finalScale = baseScale * pullScale

        matrices.scale(baseScale, baseScale, baseScale)

        if (player.nrc_schleuderLaunchAnimation != null) {
            val animation = player.nrc_schleuderLaunchAnimation ?: return
            val fadeScale = 1f - animation.getProgress()
            //matrices.scale(fadeScale, fadeScale, fadeScale)
            if (player.nrc_schleuderLaunchAnimation?.isDone == true) {
                player.nrc_schleuderLaunchAnimation = null
            }
        }
    }

    fun handleForwardRotation(
        args: Args,
        livingEntityRenderState: LivingEntityRenderState,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int
    ) {
        val entity = (livingEntityRenderState as? EntityRenderStateExt?)?.nrc_entity as? PlayerEntity? ?: return

        val matrices = args.get<MatrixStack>(0)
        val fullBodyTransform = ModelPartTransform.identity()
        val tickDelta = MinecraftClient.getInstance().renderTickCounter.getTickProgress(false);

        // Pivot-Fix: Erst zum Player-Zentrum, dann rotieren, dann zurück
        val playerHeight = 1.8f // Standard Player-Höhe

        // 1. Zum Player-Zentrum bewegen

        val arrow = entity.vehicle ?: return
        if (arrow.nrc_isSchleuderArrow == false) return
        matrices.translate(0f, playerHeight / 2f, 0f)

        // 2. Pitch von Degrees zu Radians konvertieren
        val pitchRadians = (arrow.getLerpedPitch(tickDelta) * 4) * (Math.PI / 180.0).toFloat()

        // 3. Rotation um Player-Zentrum
        matrices.multiply(
            Quaternionf().rotationZYX(
                0f, // rotZ
                0f, // rotY
                -pitchRadians
            ).rotateY(0f, Quaternionf())
        )

        // 4. Zurück zur ursprünglichen Position
        matrices.translate(0f, -playerHeight / 2f, 0f)

        args.set(0, matrices)
    }

    /**
     * Converts an angle from degrees to radians.
     *
     * @param deg The angle in degrees.
     * @return The angle in radians.
     */
    fun toRadian(deg: Float): Float {
        return deg * (PI.toFloat() / 180.0f)
    }

    fun applyProjectileSpawned(
        serverWorld: ServerWorld,
        itemStack: ItemStack,
        projectile: ProjectileEntity
    ) {
        val level = schleuder.getLevel(itemStack) ?: return
        projectile.nrc_isSchleuderArrow = true
        mcCoroutineTask(sync = true, client = false, delay = 1.ticks) {
            //projectile.isStupidAimbot = true
            val shooter = projectile.owner as? PlayerEntity? ?: return@mcCoroutineTask
            shooter.nrc_schleuderLaunchAnimation = Animation(1.6f, -10f, 0.3.seconds, Animation.Easing.LINEAR)
            shooter.startRiding(projectile, true)
            shooter.sound(SoundRegistry.SLINGSHOT_RELEASE)
            //projectile.stupidAimbotTargetId = shooter.stupidAutoAimTargetId
        }
    }

    fun onStoppedUsing(
        stack: ItemStack,
        world: World,
        user: LivingEntity,
        remainingUseTicks: Int,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (world.isClient) {
            schleuder.getLevel(stack) ?: return
            val player = MinecraftClient.getInstance().player
            if (user == player) {
                MinecraftClient.getInstance().options.perspective = Perspective.THIRD_PERSON_BACK
                MinecraftClient.getInstance().soundManager.play(SchleuderSoundInstance(player, {
                    true
                }))
            }
        }
    }

    var Entity.nrc_schleuderLaunchAnimation: Animation?
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_schleuderLaunchAnimation")
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_schleuderLaunchAnimation", value)
        }

    var Entity.nrc_isSchleuderArrow: Boolean?
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_isSchleuderArrow")
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_isSchleuderArrow", value)
        }

    private fun <S : ServerCommandSource> CommandContext<S>.schleuder() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.BOW) {
        })
        player.giveItemStack(itemStack(Items.ANVIL) {
        })
        player.giveItemStack(itemStack(Items.ENCHANTED_BOOK, 1) {
            addEnchantment(EnchantmentRegistry.schleuder.getEntry(player.world), 1)
        })
        player.giveItemStack(itemStack(Items.EXPERIENCE_BOTTLE, 32) {
        })
        player.inventory.setStack(9, itemStack(Items.ARROW, 64) {})
        player.inventory.setStack(35, itemStack(Items.BOW, 1) {
            addEnchantment(EnchantmentRegistry.schleuder.getEntry(player.world), 1)
            addEnchantment(EnchantmentRegistry.bouncy.getEntry(player.world), 1)
        })

        player.sendMessage(literalText {
            text("§b§lSchleuder Enchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent = HoverEvent.ShowText("Du hast noch einen extra Bogen im Inventar".literal)
            }
        })
    }

    class FakePlayer(clientWorld: ClientWorld, gameProfile: GameProfile, val realOne: PlayerEntity) :
        OtherClientPlayerEntity(
            clientWorld,
            gameProfile,
        ) {
        override fun isPartVisible(modelPart: PlayerModelPart): Boolean {
            return realOne.isPartVisible(modelPart)
        }
    }
}