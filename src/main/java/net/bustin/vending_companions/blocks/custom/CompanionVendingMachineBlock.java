package net.bustin.vending_companions.blocks.custom;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class CompanionVendingMachineBlock extends BaseEntityBlock {
    public CompanionVendingMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof CompanionVendingMachineBlockEntity) {
                NetworkHooks.openGui(((ServerPlayer)pPlayer), (CompanionVendingMachineBlockEntity)entity, pPos);
                CompanionVendingMachineBlockEntity locker = (CompanionVendingMachineBlockEntity) entity;
                ItemStack held = pPlayer.getItemInHand(pHand);

                if (!held.isEmpty() && held.getItem() instanceof CompanionItem) {
                    if (!locker.hasCompanion()) {
                        ItemStack toStore = held.copy();
                        toStore.setCount(1);
                        locker.insertCompanion(toStore);
                        held.shrink(1); // remove one from player hand

                        return InteractionResult.CONSUME; // don't open GUI in this case
                    } else {
                        // optional: feedback when already occupied
                        pPlayer.displayClientMessage(
                                new TextComponent("This locker already has a companion stored!"), true);
                        return InteractionResult.CONSUME;
                    }
                }
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CompanionVendingMachineBlockEntity(pPos, pState);
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntites.COMPANION_VENDING_MACHINE_BLOCK_ENTITY.get(),
                CompanionVendingMachineBlockEntity::tick);
    }
}
