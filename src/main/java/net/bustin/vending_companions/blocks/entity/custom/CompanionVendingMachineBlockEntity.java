package net.bustin.vending_companions.blocks.entity.custom;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CompanionVendingMachineBlockEntity extends BlockEntity implements MenuProvider {

    private static final String COMPANION_TAG = "Companion";

    // For now: one stored companion. We can change this to a List<ItemStack> later.
    private ItemStack storedCompanion = ItemStack.EMPTY;

    public CompanionVendingMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntites.COMPANION_VENDING_MACHINE_BLOCK_ENTITY.get(), pos, blockState);
    }

    // ---------- storage helpers ----------

    public ItemStack getStoredCompanion() {
        return storedCompanion;
    }

    /** Stores exactly one companion; returns true if accepted. */
    public boolean insertCompanion(ItemStack stack) {
        if (!isCompanion(stack)) return false;

        // For now, overwrite whatever was there
        this.storedCompanion = stack.copy();
        this.storedCompanion.setCount(1);

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    private static boolean isCompanion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof CompanionItem;
    }

    // ---------- MenuProvider ----------

    @Override
    public Component getDisplayName() {
        return new TextComponent("Companion Locker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new CompanionVendingMachineMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, CompanionVendingMachineBlockEntity be) {
        // future logic
    }

    // ---------- NBT sync ----------

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(COMPANION_TAG)) {
            this.storedCompanion = ItemStack.of(tag.getCompound(COMPANION_TAG));
        } else {
            this.storedCompanion = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!storedCompanion.isEmpty()) {
            tag.put(COMPANION_TAG, storedCompanion.serializeNBT());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean hasCompanion() {
        return !this.storedCompanion.isEmpty();
    }

    public void clearCompanion() {
        this.storedCompanion = ItemStack.EMPTY;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}

