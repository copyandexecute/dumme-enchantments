package gg.norisk.enchantments.mixin;

import com.mojang.datafixers.util.Pair;
import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {
    @Shadow
    @Final
    protected static Map<Block, Pair<Predicate<ItemUsageContext>, Consumer<ItemUsageContext>>> TILLING_ACTIONS;

    @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemUsageContext;getPlayer()Lnet/minecraft/entity/player/PlayerEntity;", ordinal = 0))
    private void stupid$colossalHoe(ItemUsageContext itemUsageContext, CallbackInfoReturnable<ActionResult> cir) {
        World world = itemUsageContext.getWorld();
        PlayerEntity playerEntity = itemUsageContext.getPlayer();
        if (playerEntity != null) {
            BlockHitResult raycast = ColossalEnchantment.INSTANCE.raycast(world, playerEntity, RaycastContext.FluidHandling.WATER);
            Integer size = EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getColossal(), playerEntity.getMainHandStack());
            if (size != null) {
                ColossalEnchantment.INSTANCE.getSurroundingBlocks(itemUsageContext.getBlockPos(), world, size, raycast.getSide(), false, (blockPos, blockState) -> {
                    Pair<Predicate<ItemUsageContext>, Consumer<ItemUsageContext>> pair = TILLING_ACTIONS.get(
                            world.getBlockState(blockPos).getBlock()
                    );
                    if (pair != null) {
                        Predicate<ItemUsageContext> predicate = pair.getFirst();
                        Consumer<ItemUsageContext> consumer = pair.getSecond();
                        ItemUsageContext context = new ItemUsageContext(itemUsageContext.getPlayer(), itemUsageContext.getHand(), new BlockHitResult(
                                itemUsageContext.getHitPos(), itemUsageContext.getSide(), blockPos, itemUsageContext.hitsInsideBlock()
                        ));

                        if (predicate.test(context)) {
                            world.playSound(playerEntity, blockPos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            if (!world.isClient) {
                                consumer.accept(context);
                                context.getStack().damage(1, playerEntity, LivingEntity.getSlotForHand(context.getHand()));
                            }
                        }
                    }
                });
            }
        }
    }
}
