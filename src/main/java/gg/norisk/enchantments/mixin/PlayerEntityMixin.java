package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.impl.ColossalEnchantment;
import gg.norisk.enchantments.impl.TrashEnchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ColossalEnchantment.ColossalPlayer {
    @Unique
    private Direction lastSide;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z"), cancellable = true)
    private void dropItemInjection(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        TrashEnchantment.INSTANCE.applyTrash((PlayerEntity) ((Object) this), itemStack, bl, bl2, cir);
    }

    @Override
    public void setStupid_lastSide(@Nullable Direction direction) {
        lastSide = direction;
    }

    @Nullable
    @Override
    public Direction getStupid_lastSide() {
        return lastSide;
    }
}
