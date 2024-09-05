package gg.norisk.enchantments.impl

import gg.norisk.datatracker.entity.getSyncedData
import gg.norisk.datatracker.entity.setSyncedData
import gg.norisk.enchantments.EnchantmentRegistry.bouncy
import gg.norisk.enchantments.EnchantmentUtils.getLevel
import gg.norisk.enchantments.StupidEnchantments.MOD_ID
import gg.norisk.enchantments.mixin.PersistentProjectileEntityAccessor
import gg.norisk.enchantments.sound.SoundRegistry
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.core.entity.modifyVelocity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object BouncyEnchantment {
    fun ProjectileEntity.onCollision(hitResult: HitResult, ci: CallbackInfo) {
        if (hitResult is BlockHitResult && world.getBlockState(hitResult.blockPos).isSolid && this.isBouncy) {
            val blockPos = hitResult.blockPos
            val blockState = world.getBlockState(blockPos)

            if (this is PersistentProjectileEntity) {
                val sound = (this as PersistentProjectileEntityAccessor).invokeGetHitSound()
                this.playSound(SoundRegistry.BOUNCY, 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f))
            }

            // Hole den Normalenvektor des Blocks, auf den getroffen wurde
            val normal = Vec3d(
                hitResult.side.vector.x.toDouble(),
                hitResult.side.vector.y.toDouble(),
                hitResult.side.vector.z.toDouble()
            ) // Der Normalenvektor basierend auf der Seite des Blocks

            // Der aktuelle Geschwindigkeitsvektor des Projektils
            val velocity = this.velocity

            // Berechne den reflektierten Vektor
            val reflectedVelocity = velocity.subtract(normal.multiply(2 * velocity.dotProduct(normal)))

            // Setze die neue Geschwindigkeit auf das reflektierte Ergebnis
            this.modifyVelocity(reflectedVelocity)

            // Stoppe das Event, da wir den Treffer manuell behandelt haben
            ci.cancel()
        }
    }

    fun applyBounce(livingEntity: LivingEntity, projectileEntity: ProjectileEntity) {
        val bounce = bouncy.getLevel(livingEntity.getEquippedStack(EquipmentSlot.MAINHAND)) ?: return
        projectileEntity.isBouncy = true
    }

    var Entity.isBouncy: Boolean
        get() = this.getSyncedData<Boolean>("$MOD_ID:IsBouncy") ?: false
        set(value) {
            this.setSyncedData("$MOD_ID:IsBouncy", value)
        }
}
