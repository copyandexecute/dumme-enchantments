package gg.norisk.enchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(world, blockPos, f, gameProfile);
    }

    @ModifyReturnValue(
            method = "getDamageAgainst",
            at = @At("RETURN")
    )
    private float stupid$colossalDamage(float original, Entity entity, float f, DamageSource damageSource) {
        return ColossalEnchantment.INSTANCE.multiplyDamage(original, entity, f, damageSource, (ServerPlayerEntity) (Object) this);
    }
}
