package gg.norisk.enchantments.impl.freeze

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.brigadier.context.CommandContext
import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.registeredTypes
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.datatracker.entity.syncedValueChangeEvent
import gg.norisk.enchantments.EnchantmentRegistry
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.EnchantmentUtils.sound
import gg.norisk.enchantments.StupidEnchantments
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.command.EnchantmentsCommand.default
import gg.norisk.enchantments.command.EnchantmentsCommand.getEntry
import gg.norisk.enchantments.sound.SoundRegistry
import gg.norisk.enchantments.utils.Animation
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.text.HoverEvent
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.event.GameEvent
import net.silkmc.silk.commands.PermissionLevel
import org.joml.Vector3f
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.core.text.literalText
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object FreezeEnchantment {
    interface IFrozenEntity {
        var nrc_lastEntityRenderStateLol: EntityRenderState?
    }

    fun initServer() {
        (registeredTypes as MutableMap<Any, Any>).put(
            Animation::class,
            Animation.serializer(),
        )

        command("enchantments") {
            requiresPermissionLevel(PermissionLevel.OWNER)
            literal("freeze") {
                runs {
                    this.default()
                    this.freeze()
                }
            }
        }

        // Server Tick Event für Frost Walker Effekt
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register { world ->
            for (entity in world.iterateEntities()) {
                if (entity is LivingEntity && entity.nrc_isFrozen && entity.isOnGround) {
                    applyFrostWalkerEffect(world, entity)
                }
            }
        }
    }

    fun applyTargetDamage(world: ServerWorld, entity: Entity, damageSource: DamageSource, itemStack: ItemStack?) {
        if (entity.nrc_isFrozen && itemStack?.isOf(Items.MACE) == true) {
            playFreezeBreakAnimationAnimationServerSided(entity)
            entity.nrc_frozenAnimation = null
            if (entity.type != EntityType.PLAYER) {
                entity.discard()
            }
            return
        }
        val freezeLevel = EnchantmentRegistry.freeze.getLevel(itemStack)
        if (freezeLevel != null) {
            if (!entity.nrc_isFrozen) {
                val duration = (freezeLevel * 6).seconds
                entity.nrc_isFrozen = true
                entity.nrc_frozenAnimation = Animation(0f, 240f, 2.seconds, Animation.Easing.EXPO_OUT)
                val livingEntity = entity as? LivingEntity?
                livingEntity?.sound(SoundRegistry.FREEZE, volume = 2f, Random.Default.nextDouble(0.8, 1.3))
                livingEntity?.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        duration.inTicks,
                        5,
                        false,
                        false,
                        false
                    )
                )
                //world.server.broadcastText("${entity.type} wurde gefreezed")
                mcCoroutineTask(sync = true, client = false, delay = duration) {
                    entity.nrc_isFrozen = false
                    entity.nrc_frozenAnimation = null
                    entity.sound(
                        SoundEvents.BLOCK_GLASS_BREAK, 0.8f, Random.nextDouble(0.7, 0.8),
                    )
                    //world.server.broadcastText("${entity.type} wurde entfreezed")
                }
            }
        }
    }

    fun initClient() {
        syncedValueChangeEvent.listen { event ->
            if (event.key != "$MOD_ID:nrc_frozenAnimation") return@listen
            if (event.entity.nrc_frozenAnimation != null) {
                if (event.entity.world.isClient) {
                    playFreezeAnimationClientSided(event.entity)
                }
            }
            if (event.entity.nrc_frozenAnimation == null) {
                if (event.entity.world.isClient) {
                    playFreezeBreakAnimationAnimationClientSided(event.entity)
                }
                // Nur serverseitig für Display Entities
            }
        }
    }

    private fun playFreezeBreakAnimationAnimationServerSided(entity: Entity) {
        val world = entity.world as? ServerWorld ?: return
        val entityPos = entity.pos
        val entityHeight = entity.height * 1.25

        // === REALISTISCHE ICE BREAK SIMULATION ===
        // Spawne 12-18 Eis-Fragmente mit verschiedenen Eigenschaften
        val fragmentCount = Random.nextInt(12, 19)
        val iceFragments = mutableListOf<DisplayEntity.BlockDisplayEntity>()

        repeat(fragmentCount) { i ->
            // Verschiedene Eis-Arten für Variation
            val iceBlocks = listOf(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE)
            val iceBlock = iceBlocks.random()

            // Fragment-Größe: Mix aus großen und kleinen Stücken
            val fragmentSize = when {
                i < 3 -> Random.nextFloat() * 0.25f + 0.3f  // 3 große Stücke (0.3-0.55)
                i < 8 -> Random.nextFloat() * 0.2f + 0.15f   // 5 mittlere Stücke (0.15-0.35)
                else -> Random.nextFloat() * 0.15f + 0.05f   // Rest kleine Stücke (0.05-0.2)
            }

            // Sanftes Zerbrechen: Kleine, lokale Bewegungen
            val angle = Random.nextDouble() * 2 * Math.PI
            val heightFactor = Random.nextDouble(0.3, 0.9) // Mehr um Entity-Mitte
            val localRadius = Random.nextDouble(0.2, 0.8) // Viel kleinerer Radius

            // Start-Position: Sehr nah an Entity
            val startX = entityPos.x + Random.nextDouble(-0.15, 0.15)
            val startY = entityPos.y + entityHeight * heightFactor
            val startZ = entityPos.z + Random.nextDouble(-0.15, 0.15)

            // Sanfte Velocity: Mehr nach oben/unten, weniger horizontal
            val baseVelocity = Random.nextDouble(0.1, 0.3) // Viel langsamer
            val velocityX = cos(angle) * baseVelocity * localRadius * 0.5 // Reduzierte horizontale Kraft
            val velocityY = Random.nextDouble(0.05, 0.25) // Sanftere vertikale Bewegung
            val velocityZ = sin(angle) * baseVelocity * localRadius * 0.5

            // Display Entity erstellen (korrekter Konstruktor)
            val blockDisplay = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world)
            blockDisplay.setPosition(startX, startY, startZ)
            blockDisplay.setBlockState(iceBlock.defaultState)

            // Transformation mit korrekten Methoden setzen
            val scale = Vector3f(fragmentSize, fragmentSize, fragmentSize)
            val translation = Vector3f(0f, 0f, 0f)

            // Anfangs-Rotation für Realismus
            val leftRotation = org.joml.Quaternionf().rotationXYZ(
                Math.toRadians(Random.nextFloat() * 360f.toDouble()).toFloat(),
                Math.toRadians(Random.nextFloat() * 360f.toDouble()).toFloat(),
                Math.toRadians(Random.nextFloat() * 360f.toDouble()).toFloat()
            )
            val rightRotation = org.joml.Quaternionf() // Identität

            // Komplette Transformation setzen
            val transformation = AffineTransformation(
                translation, leftRotation, scale, rightRotation
            )
            blockDisplay.setTransformation(transformation)

            // Custom Velocity Storage (DisplayEntities haben keine eingebaute Physik)
            var currentVelX = velocityX
            var currentVelY = velocityY
            var currentVelZ = velocityZ

            // In Welt spawnen
            world.spawnEntity(blockDisplay)
            iceFragments.add(blockDisplay)

            // === PHYSICS SIMULATION ===
            mcCoroutineTask(sync = true, client = false, howOften = 20, period = 1.ticks) { task ->
                if (!blockDisplay.isAlive) {
                    blockDisplay.discard()
                    return@mcCoroutineTask
                }

                // Gravity und Luftreibung anwenden
                val gravityStrength = 0.04 // Realistische Schwerkraft
                currentVelX *= 0.99 // Leichte Luftreibung
                currentVelY -= gravityStrength
                currentVelZ *= 0.99

                // Position manuell updaten (DisplayEntities haben keine Auto-Physik)
                val currentPos = blockDisplay.pos
                val newX = currentPos.x + currentVelX
                val newY = currentPos.y + currentVelY
                val newZ = currentPos.z + currentVelZ

                // Boden-Kollision prüfen
                val groundY = world.getTopY(
                    net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    newX.toInt(), newZ.toInt()
                ).toDouble()

                if (newY <= groundY && currentVelY < 0) {
                    // Bounce-Effekt
                    currentVelY = -currentVelY * 0.4 // Bounce mit Energy-Loss
                    currentVelX *= 0.8 // Reibung beim Aufprall
                    currentVelZ *= 0.8

                    // Sound bei Aufprall
                    if (kotlin.math.abs(currentVelY) > 0.05) {
                        world.playSoundFromEntity(
                            null, blockDisplay,
                            SoundEvents.BLOCK_GLASS_BREAK,
                            net.minecraft.sound.SoundCategory.BLOCKS,
                            0.1f + fragmentSize * 0.3f,
                            Random.nextFloat() * 0.4f + 0.8f
                        )
                    }

                    blockDisplay.setPosition(newX, groundY + 0.1, newZ)
                } else {
                    blockDisplay.setPosition(newX, newY, newZ)
                }

                // Rotation während Flug (Tumbling)
                val rotationSpeed = 6f
                val currentTransformation = DisplayEntity.getTransformation(blockDisplay.dataTracker)

                val currentLeftRot = currentTransformation.leftRotation
                val newLeftRotation = org.joml.Quaternionf(currentLeftRot).rotateXYZ(
                    Math.toRadians(rotationSpeed.toDouble()).toFloat(),
                    Math.toRadians(rotationSpeed * 0.7).toFloat(),
                    Math.toRadians(rotationSpeed * 1.3).toFloat()
                )

                // Transformation mit neuer Rotation updaten
                val newTransformation = AffineTransformation(
                    currentTransformation.translation,
                    newLeftRotation,
                    currentTransformation.scale,
                    currentTransformation.rightRotation
                )
                blockDisplay.setTransformation(newTransformation)

                // Fade-out in letzten 20 Ticks (1 Sekunde)
                val timeLeft = (20 - task.round).toFloat() // Verbleibende Ticks

                if (timeLeft <= 10) { // Letzte Sekunde
                    val alpha = (timeLeft / 10f).coerceIn(0f, 1f)
                    val fadeScale = alpha * fragmentSize

                    val fadeTransformation = AffineTransformation(
                        currentTransformation.translation,
                        newLeftRotation,
                        Vector3f(fadeScale, fadeScale, fadeScale),
                        currentTransformation.rightRotation
                    )
                    blockDisplay.setTransformation(fadeTransformation)
                }
            } // Läuft automatisch 60x (3 Sekunden)
        }

        // Sound-Effekt für die gesamte Explosion
        world.playSound(
            null, entity.blockPos,
            SoundEvents.BLOCK_GLASS_BREAK,
            net.minecraft.sound.SoundCategory.BLOCKS,
            1.2f, 0.8f
        )
    }

    private fun playFreezeBreakAnimationAnimationClientSided(entity: Entity) {
        val world = entity.world
        if (!world.isClient) return

        // Sofortige Explosion von Eis-Splittern
        repeat(25) {
            val angle = Random.nextDouble() * 2 * Math.PI
            val radius = Random.nextDouble(0.5, 2.0)
            val height = Random.nextDouble(0.0, entity.height.toDouble())

            val x = entity.x + radius * cos(angle)
            val z = entity.z + radius * sin(angle)
            val y = entity.y + height

            world.addParticleClient(
                ParticleTypes.SNOWFLAKE,
                x, y, z,
                Random.nextDouble(-0.3, 0.3),
                Random.nextDouble(0.0, 0.2),
                Random.nextDouble(-0.3, 0.3)
            )
        }

        // Große Eis-Brocken die wegfliegen
        repeat(15) {
            world.addParticleClient(
                ParticleTypes.CLOUD,
                entity.getParticleX(1.0),
                entity.randomBodyY,
                entity.getParticleZ(1.0),
                Random.nextDouble(-0.2, 0.2),
                Random.nextDouble(0.0, 0.15),
                Random.nextDouble(-0.2, 0.2)
            )
        }

        // Feiner Frost-Staub
        repeat(20) {
            world.addParticleClient(
                ParticleTypes.WHITE_ASH,
                entity.getParticleX(0.8),
                entity.y + entity.height * Random.nextFloat(),
                entity.getParticleZ(0.8),
                Random.nextDouble(-0.1, 0.1),
                Random.nextDouble(0.0, 0.1),
                Random.nextDouble(-0.1, 0.1)
            )
        }

        // Zentrale Explosion am Entity-Zentrum
        world.addParticleClient(
            ParticleTypes.POOF,
            entity.x,
            entity.y + entity.height * 0.5,
            entity.z,
            0.0, 0.1, 0.0
        )

        // Nachträgliche Partikel-Welle mit Verzögerung
        mcCoroutineTask(sync = true, client = true, delay = 50.milliseconds) {
            repeat(10) {
                val angle = it * (2 * Math.PI / 10)
                val radius = 1.0
                val x = entity.x + radius * cos(angle)
                val z = entity.z + radius * sin(angle)

                world.addParticleClient(
                    ParticleTypes.SNOWFLAKE,
                    x, entity.y + 0.1, z,
                    cos(angle) * 0.1,
                    Random.nextDouble(0.0, 0.05),
                    sin(angle) * 0.1
                )
            }
        }

        // Finale Frost-Dampf-Wolke
        mcCoroutineTask(sync = true, client = true, delay = 100.milliseconds) {
            repeat(8) {
                world.addParticleClient(
                    ParticleTypes.WHITE_ASH,
                    entity.getParticleX(1.5),
                    entity.y + entity.height * 0.3,
                    entity.getParticleZ(1.5),
                    Random.nextDouble(-0.05, 0.05),
                    Random.nextDouble(0.02, 0.08),
                    Random.nextDouble(-0.05, 0.05)
                )
            }
        }
    }

    private fun playFreezeAnimationClientSided(entity: Entity) {
        val world = entity.world
        if (!world.isClient) return

        // Schneeflocken-Kreis um die Entity
        repeat(10) {
            val angle = it * (2 * Math.PI / 20)
            val radius = 1.5
            val x = entity.x + radius * kotlin.math.cos(angle)
            val z = entity.z + radius * kotlin.math.sin(angle)
            val y = entity.y + entity.height * Random.nextFloat()

            world.addParticleClient(
                ParticleTypes.SNOWFLAKE,
                x, y, z,
                Random.nextDouble(-0.1, 0.1),
                Random.nextDouble(-0.05, 0.05),
                Random.nextDouble(-0.1, 0.1)
            )
        }

        // Weißer Nebel/Frost-Effekt
        repeat(10) {
            world.addParticleClient(
                ParticleTypes.WHITE_ASH,
                entity.getParticleX(1.0),
                entity.randomBodyY,
                entity.getParticleZ(1.0),
                Random.nextDouble(-0.1, 0.1),
                Random.nextDouble(0.0, 0.1),
                Random.nextDouble(-0.1, 0.1)
            )
        }

        // Eisige Wolken-Partikel
        repeat(5) {
            world.addParticleClient(
                ParticleTypes.CLOUD,
                entity.getParticleX(0.8),
                entity.y + entity.height * 0.7,
                entity.getParticleZ(0.8),
                Random.nextDouble(-0.05, 0.05),
                Random.nextDouble(-0.02, 0.02),
                Random.nextDouble(-0.05, 0.05)
            )
        }

        // Kontinuierliche Partikel während der Freeze-Dauer
        mcCoroutineTask(sync = true, client = true, howOften = 10, delay = 100.milliseconds) {
            if (!entity.nrc_isFrozen) return@mcCoroutineTask

            // Sanfte fallende Schneeflocken
            repeat(3) {
                world.addParticleClient(
                    ParticleTypes.SNOWFLAKE,
                    entity.getParticleX(1.2),
                    entity.y + entity.height + Random.nextDouble(0.0, 0.5),
                    entity.getParticleZ(1.2),
                    Random.nextDouble(-0.02, 0.02),
                    Random.nextDouble(-0.1, -0.05),
                    Random.nextDouble(-0.02, 0.02)
                )
            }

            // Gelegentliche Frost-Dampf-Effekte
            if (Random.nextFloat() < 0.3f) {
                world.addParticleClient(
                    ParticleTypes.WHITE_ASH,
                    entity.x,
                    entity.y + entity.height * 0.5,
                    entity.z,
                    Random.nextDouble(-0.03, 0.03),
                    Random.nextDouble(0.0, 0.05),
                    Random.nextDouble(-0.03, 0.03)
                )
            }
        }
    }

    private fun <S : ServerCommandSource> CommandContext<S>.freeze() {
        val player = this.source.playerOrThrow

        player.giveItemStack(itemStack(Items.WOODEN_SWORD) {
            addEnchantment(EnchantmentRegistry.freeze.getEntry(player.world), 1)
        })

        player.giveItemStack(itemStack(Items.PIG_SPAWN_EGG, 64) {})
        player.giveItemStack(itemStack(Items.HUSK_SPAWN_EGG, 64) {})
        player.giveItemStack(itemStack(Items.IRON_GOLEM_SPAWN_EGG, 64) {})
        player.giveItemStack(itemStack(Items.HORSE_SPAWN_EGG, 64) {})

        player.inventory.setStack(8, itemStack(Items.MACE, 1) {})

        player.sendMessage(literalText {
            text("§b§lFreeze Enchantment - ")
            text("Wenn du nicht weiter weißt ->") {
                italic = true
                color = Color.LIGHT_GRAY.rgb
                hoverEvent = HoverEvent.ShowText("Hover über die Nummern für Details".literal)
            }
            text("\n§71. ") {
                hoverEvent =
                    HoverEvent.ShowText("Schlage Entities mit dem Schwert - sie werden für 6 Sekunden eingefroren und können geschubst werden".literal)
            }
            text("§72. ") {
                hoverEvent =
                    HoverEvent.ShowText("Verwende die Mace (Slot 9) um gefrorene Entities zu zerbrechen - coole Eis-Splitter Animation!".literal)
            }
            text("§73. ") {
                hoverEvent =
                    HoverEvent.ShowText("Gefrorene Entities verwandeln Wasser unter ihnen zu Eis (Frost Walker Effekt)".literal)
            }
            text("§74. ") {
                hoverEvent =
                    HoverEvent.ShowText("Spawne Mobs mit den Eggs und probiere verschiedene Kombinationen aus!".literal)
            }
        })
    }

    var Entity.nrc_isFrozen: Boolean
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_isFrozen") ?: false
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_isFrozen", value)
        }
    var Entity.nrc_frozenAnimation: Animation?
        get() = this.getSyncedData("${StupidEnchantments.MOD_ID}:nrc_frozenAnimation")
        set(value) {
            this.setSyncedData("${StupidEnchantments.MOD_ID}:nrc_frozenAnimation", value)
        }

    fun <S : EntityRenderState, T : Entity> copyRenderState(original: S, entity: T, tickProgress: Float) {

    }

    fun modifyRenderState(state: EntityRenderState) {
        (state as? LivingEntityRenderState?)?.apply {
            hurt = false
        }
    }

    fun LivingEntity.getAttackKnockbackAgainstMixin(
        target: Entity,
        damageSource: DamageSource,
        cir: CallbackInfoReturnable<Float>
    ) {
        val level = EnchantmentRegistry.freeze.getLevel(weaponStack) ?: return
        cir.returnValue = 0f
    }

    fun LivingEntity.cancelKnockback(
        strength: Double,
        x: Double,
        z: Double,
        original: Operation<Void>,
        world: ServerWorld,
        source: DamageSource,
        amount: Float
    ) {
        val attacker = source.attacker
        if (attacker != null) {
            val level = EnchantmentRegistry.freeze.getLevel(attacker.weaponStack)
            if (level != null) {
                original.call(this, strength * 0.2, x * 0.2, z * 0.2)
                return
            }
        }
        original.call(this, strength, x, z)
    }

    /**
     * Implementiert den Frost Walker Effekt für gefrorene Entities
     * Basiert auf der slippery.json Logik: verwandelt Wasser in Frosted Ice
     */
    private fun applyFrostWalkerEffect(world: ServerWorld, entity: LivingEntity) {
        val entityPos = entity.blockPos
        val radius = 3 // Fester Radius wie in slippery.json (base: 3)
        val height = 1
        val yOffset = -1 // offset: [0, -1, 0] - einen Block unter der Entity

        // Durchgehe alle Blöcke in einem Kreis um die Entity
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val distance = kotlin.math.sqrt((x * x + z * z).toDouble())
                if (distance > radius) continue // Außerhalb des Kreises

                for (y in 0 until height) {
                    val targetPos = entityPos.add(x, yOffset + y, z)
                    val abovePos = targetPos.up()

                    val targetState = world.getBlockState(targetPos)
                    val aboveState = world.getBlockState(abovePos)
                    val fluidState = world.getFluidState(targetPos)

                    // Prüfe alle Bedingungen aus der JSON:
                    // 1. Block darüber muss Luft sein (matching_block_tag: air)
                    // 2. Block muss Wasser sein (matching_blocks: water)
                    // 3. Fluid muss Wasser sein (matching_fluids: water)
                    // 4. Position muss frei/unobstructed sein
                    if (aboveState.isAir &&
                        targetState.isOf(Blocks.WATER) &&
                        fluidState.isOf(net.minecraft.fluid.Fluids.WATER) &&
                        !world.getBlockState(targetPos).isFullCube(world, targetPos)
                    ) {

                        // Erstelle Frosted Ice mit age=0 wie in der JSON
                        val frostedIceState = Blocks.FROSTED_ICE.defaultState
                            .with(net.minecraft.block.FrostedIceBlock.AGE, 0)

                        // Setze den Block
                        world.setBlockState(
                            targetPos, frostedIceState,
                            net.minecraft.block.Block.NOTIFY_ALL or net.minecraft.block.Block.REDRAW_ON_MAIN_THREAD
                        )

                        // Triggere Game Event wie in der JSON
                        world.emitGameEvent(entity, GameEvent.BLOCK_PLACE, targetPos)

                        // Optionaler Frost-Sound
                        world.playSoundFromEntity(
                            null, entity,
                            SoundEvents.BLOCK_GLASS_PLACE,
                            net.minecraft.sound.SoundCategory.BLOCKS,
                            0.3f, 1.5f
                        )
                    }
                }
            }
        }
    }

    val Duration.inTicks: Int get() = (this.inWholeMilliseconds / 50).toInt()

    fun LivingEntity.apply(block: Block, original: Operation<Float>): Float {
        if (!nrc_isFrozen) {
            return original.call(block)
        } else {
            val slippery = 1.0f + 0.1f * 1.1f
            return slippery
        }
    }

    fun applyStepSound(instance: Entity, pos: BlockPos, state: BlockState, original: Operation<Void>) {
        if (instance is LivingEntity && instance.nrc_isFrozen) {
            val blockSoundGroup = state.soundGroup
            instance.sound(
                SoundEvents.ENTITY_BREEZE_SLIDE,
                blockSoundGroup.getVolume() * 0.8F,
                blockSoundGroup.getPitch()
            )
        } else {
            original.call(instance, pos, state)
        }
    }
}