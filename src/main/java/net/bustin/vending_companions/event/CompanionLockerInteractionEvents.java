package net.bustin.vending_companions.event;

import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlock;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VendingCompanions.MOD_ID)
public class CompanionLockerInteractionEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty()) {
            return;
        }

        Level level = event.getWorld();
        BlockPos lockerPos = getLockerPos(level, event.getPos());
        if (lockerPos == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(lockerPos);
        if (!(blockEntity instanceof CompanionVendingMachineBlockEntity locker)) {
            return;
        }

        locker.setOwner(player);
        if (!locker.isOwner(player)) {
            player.displayClientMessage(
                    new TextComponent("You do not own this Companion Locker.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            consumeInteraction(event);
            return;
        }

        int moved = locker.pullCompanionsFromCurios(player);
        if (moved <= 0) {
            return;
        }

        player.displayClientMessage(
                new TextComponent("Stored Companion")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        consumeInteraction(event);
    }

    private static BlockPos getLockerPos(Level level, BlockPos clickedPos) {
        BlockState state = level.getBlockState(clickedPos);
        if (!(state.getBlock() instanceof CompanionVendingMachineBlock)) {
            return null;
        }

        if (state.getValue(CompanionVendingMachineBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = clickedPos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (!(lowerState.getBlock() instanceof CompanionVendingMachineBlock)) {
                return null;
            }
            return lowerPos;
        }

        return clickedPos;
    }

    private static void consumeInteraction(PlayerInteractEvent.RightClickBlock event) {
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }
}