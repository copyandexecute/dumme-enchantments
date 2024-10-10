package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ShovelItem.class)
public abstract class ShovelItemMixin {
    @Shadow
    @Final
    protected static Map<Block, BlockState> PATH_STATES;

    @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemUsageContext;getPlayer()Lnet/minecraft/entity/player/PlayerEntity;", ordinal = 0))
    private void stupid$colossalShovel(ItemUsageContext itemUsageContext, CallbackInfoReturnable<ActionResult> cir) {
        World world = itemUsageContext.getWorld();
        PlayerEntity playerEntity = itemUsageContext.getPlayer();
        if (playerEntity != null) {
            BlockHitResult raycast = ColossalEnchantment.INSTANCE.raycast(world, playerEntity, RaycastContext.FluidHandling.WATER);
            Integer size = EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getColossal(), playerEntity.getMainHandStack());
            if (size != null) {
                ColossalEnchantment.INSTANCE.getSurroundingBlocks(itemUsageContext.getBlockPos(), world, size, raycast.getSide(), false, (blockPos, blockState) -> {
                    BlockState blockState2 = PATH_STATES.get(blockState.getBlock());
                    BlockState blockState3 = null;
                    if (blockState2 != null && world.getBlockState(blockPos.up()).isAir()) {
                        world.playSound(playerEntity, blockPos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        blockState3 = blockState2;
                    } else if (blockState.getBlock() instanceof CampfireBlock && (Boolean) blockState.get(CampfireBlock.LIT)) {
                        if (!world.isClient()) {
                            world.syncWorldEvent(null, 1009, blockPos, 0);
                        }

                        CampfireBlock.extinguish(itemUsageContext.getPlayer(), world, blockPos, blockState);
                        blockState3 = blockState.with(CampfireBlock.LIT, Boolean.FALSE);
                    }
                    if (blockState3 != null) {
                        if (!world.isClient) {
                            world.setBlockState(blockPos, blockState3, 11);
                            world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(playerEntity, blockState3));
                            itemUsageContext.getStack().damage(1, playerEntity, LivingEntity.getSlotForHand(itemUsageContext.getHand()));
                        }
                    }
                });
            }
        }
    }
}
