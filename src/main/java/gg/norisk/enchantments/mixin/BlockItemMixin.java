package gg.norisk.enchantments.mixin;

import gg.norisk.enchantments.EnchantmentRegistry;
import gg.norisk.enchantments.EnchantmentUtils;
import gg.norisk.enchantments.impl.ColossalEnchantment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    public abstract @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext itemPlacementContext);

    @Shadow
    protected abstract BlockState placeFromNbt(BlockPos blockPos, World world, ItemStack itemStack, BlockState blockState);

    @Shadow
    protected abstract boolean postPlacement(BlockPos blockPos, World world, @Nullable PlayerEntity playerEntity, ItemStack itemStack, BlockState blockState);

    @Shadow
    private static void copyComponentsToBlockEntity(World world, BlockPos blockPos, ItemStack itemStack) {
    }

    @Shadow
    protected abstract SoundEvent getPlaceSound(BlockState blockState);

    @Shadow
    public abstract ActionResult place(ItemPlacementContext itemPlacementContext);

    @Shadow
    protected abstract @Nullable BlockState getPlacementState(ItemPlacementContext itemPlacementContext);

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"))
    private void stupid$colossalPlace(ItemPlacementContext itemPlacementContext, CallbackInfoReturnable<ActionResult> cir) {
        ItemPlacementContext itemUsageContext = this.getPlacementContext(itemPlacementContext);
        if (itemUsageContext == null) return;
        World world = itemUsageContext.getWorld();
        PlayerEntity playerEntity = itemUsageContext.getPlayer();
        ItemStack itemStack = itemUsageContext.getStack();
        BlockState blockState = this.getPlacementState(itemUsageContext);

        if (playerEntity != null) {
            Integer size = EnchantmentUtils.INSTANCE.getLevel(EnchantmentRegistry.INSTANCE.getColossal(), playerEntity.getMainHandStack());
            if (size != null) {
                ColossalEnchantment.INSTANCE.getSurroundingBlocks(itemUsageContext.getBlockPos(), world, size, itemUsageContext.getSide(),true, (blockPos, oldBlockState) -> {
                    if (blockState != null) {
                        if (itemPlacementContext.getWorld().setBlockState(blockPos, blockState, 11)) {
                            BlockState blockState2 = world.getBlockState(blockPos);
                            if (blockState2.isOf(blockState.getBlock())) {
                                blockState2 = this.placeFromNbt(blockPos, world, itemStack, blockState2);
                                this.postPlacement(blockPos, world, playerEntity, itemStack, blockState2);
                                copyComponentsToBlockEntity(world, blockPos, itemStack);
                                blockState2.getBlock().onPlaced(world, blockPos, blockState2, playerEntity, itemStack);
                                if (playerEntity instanceof ServerPlayerEntity) {
                                    Criteria.PLACED_BLOCK.trigger((ServerPlayerEntity) playerEntity, blockPos, itemStack);
                                }
                            }

                            BlockSoundGroup blockSoundGroup = blockState2.getSoundGroup();
                            world.playSound(
                                    playerEntity,
                                    blockPos,
                                    this.getPlaceSound(blockState2),
                                    SoundCategory.BLOCKS,
                                    (blockSoundGroup.getVolume() + 1.0F) / 2.0F,
                                    blockSoundGroup.getPitch() * 0.8F
                            );
                            world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(playerEntity, blockState2));
                            itemStack.decrementUnlessCreative(1, playerEntity);
                        }
                    }
                });
            }
        }
    }
}
