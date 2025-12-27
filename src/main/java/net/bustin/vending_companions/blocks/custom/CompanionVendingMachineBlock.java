package net.bustin.vending_companions.blocks.custom;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.ChatFormatting;
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

        // If player clicked the UPPER half, reroute to LOWER (BE lives there)
        if (pState.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pPos.below();
            if (pLevel.getBlockState(below).getBlock() != this) return InteractionResult.PASS;
            pPos = below;
            pState = pLevel.getBlockState(pPos);
        }

        // Client side: just return success so it plays hand animation
        if (pLevel.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (!(entity instanceof CompanionVendingMachineBlockEntity locker)) {
            throw new IllegalStateException("Our Container provider is missing!");
        }

        // ---------------- OWNER GATE (VH-style) ----------------
        // First valid interaction claims ownership
        locker.setOwner(pPlayer);

        // If not owner, deny BOTH inserting and opening GUI
        if (!locker.isOwner(pPlayer)) {
            pPlayer.displayClientMessage(
                    new TextComponent("You do not own this Companion Locker.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.CONSUME;
        }
        // -------------------------------------------------------

        ItemStack held = pPlayer.getItemInHand(pHand);

        // SHIFT + empty hand: pull companion from Curios head slot into locker
        if (pPlayer.isShiftKeyDown() && held.isEmpty() && pPlayer instanceof ServerPlayer sp) {
            boolean moved = locker.pullCompanionFromHeadCurio(sp);
            if (moved) {
                // optional feedback
                pPlayer.displayClientMessage(
                        new TextComponent("Stored your equipped companion.").withStyle(ChatFormatting.GREEN),
                        true
                );
                return InteractionResult.CONSUME; // don't open GUI
            }
            return InteractionResult.PASS; // nothing to move
        }

        // If holding a companion, add one to the locker and DON'T open GUI
        if (!held.isEmpty() && held.getItem() instanceof CompanionItem) {
            ItemStack toStore = held.copy();
            toStore.setCount(1);

            locker.insertCompanion(toStore);
            held.shrink(1);

            return InteractionResult.CONSUME;
        }



        // Otherwise, open the GUI
        NetworkHooks.openGui((ServerPlayer) pPlayer, locker, pPos);
        return InteractionResult.CONSUME;
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
