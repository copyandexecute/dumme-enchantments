package gg.norisk.enchantments.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld clientWorld, GameProfile gameProfile) {
        super(clientWorld, gameProfile);
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void stupid$Ram(CallbackInfo ci) {
        //RollEnchantment.INSTANCE.handleInput((ClientPlayerEntity) (Object) this);
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")
    )
    private boolean stupid$isUsingItem(boolean original) {
        return original && EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getRam(), this.activeItemStack) == null;
    }
}
