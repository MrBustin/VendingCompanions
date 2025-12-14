package net.bustin.vending_companions.blocks.custom;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class CompanionVendingMachineBlock extends BaseEntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CompanionVendingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

        if (pState.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pPos.below();
            if (pLevel.getBlockState(below).getBlock() != this) return InteractionResult.PASS;
            pPos = below;
            pState = pLevel.getBlockState(pPos);
        }
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if (entity instanceof CompanionVendingMachineBlockEntity) {
                CompanionVendingMachineBlockEntity locker = (CompanionVendingMachineBlockEntity) entity;
                ItemStack held = pPlayer.getItemInHand(pHand);

                // If holding a companion, always add one to the locker
                if (!held.isEmpty() && held.getItem() instanceof CompanionItem) {
                    ItemStack toStore = held.copy();
                    toStore.setCount(1);
                    locker.insertCompanion(toStore);
                    held.shrink(1); // remove one from player hand

                    // Don't open the GUI in this case, just store
                    return InteractionResult.CONSUME;
                }

                // Otherwise, just open the GUI as before
                NetworkHooks.openGui(((ServerPlayer) pPlayer), locker, pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }




    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;

        BlockPos above = pos.above();
        level.setBlock(above, state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();

        if (pos.getY() >= level.getMaxBuildHeight() - 1) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(ctx)) return null;

        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            super.playerWillDestroy(level, pos, state, player);
            return;
        }

        DoubleBlockHalf half = state.getValue(HALF);

        // If the TOP half is broken, route the break to the BOTTOM half (the BE lives there)
        if (half == DoubleBlockHalf.UPPER) {
            BlockPos mainPos = pos.below();
            BlockState mainState = level.getBlockState(mainPos);

            // remove the top half first without drops
            level.removeBlock(pos, false);

            if (mainState.getBlock() == this) {
                if (player.isCreative()) {
                    // creative: just delete the lower too (no drops)
                    level.removeBlock(mainPos, false);
                } else {
                    // survival: break the lower normally -> this is where your "shulker-style" item drop happens
                    level.destroyBlock(mainPos, true, player);
                }
            }
            return;
        }

        // If the BOTTOM half is broken, let vanilla flow happen (this is already working for you)
        super.playerWillDestroy(level, pos, state, player);

        // Then remove the top half without drops
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.getBlock() == this && aboveState.getValue(HALF) == DoubleBlockHalf.UPPER) {
            level.removeBlock(above, false);
        }
    }



    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {

        DoubleBlockHalf half = state.getValue(HALF);

        if (dir == Direction.UP && half == DoubleBlockHalf.LOWER) {
            if (neighbor.getBlock() != this || neighbor.getValue(HALF) != DoubleBlockHalf.UPPER) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (dir == Direction.DOWN && half == DoubleBlockHalf.UPPER) {
            if (neighbor.getBlock() != this || neighbor.getValue(HALF) != DoubleBlockHalf.LOWER) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new CompanionVendingMachineBlockEntity(pos, state)
                : null;
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntites.COMPANION_VENDING_MACHINE_BLOCK_ENTITY.get(),
                CompanionVendingMachineBlockEntity::tick);
    }
}
