package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AxeItem.class)
public abstract class AxeItemMixin {
    @Shadow
    protected abstract Optional<BlockState> tryStrip(World world, BlockPos blockPos, @Nullable PlayerEntity playerEntity, BlockState blockState);

    @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/AxeItem;tryStrip(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/block/BlockState;)Ljava/util/Optional;", shift = At.Shift.AFTER))
    private void stupid$MultiStrip(ItemUsageContext itemUsageContext, CallbackInfoReturnable<ActionResult> cir) {
        World world = itemUsageContext.getWorld();
        BlockPos blockPos = itemUsageContext.getBlockPos();
        PlayerEntity playerEntity = itemUsageContext.getPlayer();
        if (playerEntity != null) {
            BlockHitResult raycast = ColossalEnchantment.INSTANCE.raycast(world, playerEntity, RaycastContext.FluidHandling.WATER);
            Integer size = EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getColossal(), playerEntity.getMainHandStack());
            if (size != null) {
                ColossalEnchantment.INSTANCE.getSurroundingBlocks(blockPos, world, size, raycast.getSide(), (newPos, newState) -> {
                    Optional<BlockState> result = tryStrip(world, newPos, playerEntity, newState);
                    result.ifPresent(blockState -> {
                        ItemStack itemStack = itemUsageContext.getStack();
                        if (playerEntity instanceof ServerPlayerEntity) {
                            Criteria.ITEM_USED_ON_BLOCK.trigger((ServerPlayerEntity) playerEntity, newPos, itemStack);
                        }

                        world.setBlockState(newPos, result.get(), 11);
                        world.emitGameEvent(GameEvent.BLOCK_CHANGE, newPos, GameEvent.Emitter.of(playerEntity, result.get()));
                        itemStack.damage(1, playerEntity, LivingEntity.getSlotForHand(itemUsageContext.getHand()));
                    });
                });
            }
        }
    }
}
