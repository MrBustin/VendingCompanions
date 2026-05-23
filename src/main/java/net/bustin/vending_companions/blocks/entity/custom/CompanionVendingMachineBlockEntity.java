package net.bustin.vending_companions.blocks.entity.custom;

import iskallia.vault.init.ModItems;
import iskallia.vault.init.ModSounds;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlock;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompanionVendingMachineBlockEntity extends BlockEntity implements MenuProvider {

    private static final String COMPANIONS_TAG = "Companions";

    private UUID owner;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1){
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private final ItemStackHandler relicHandler = new ItemStackHandler(16) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override public int getSlotLimit(int slot) { return 1; } // relics usually 1 each
    };


    private LazyOptional<ItemStackHandler> lazyItemHandler = LazyOptional.empty();

    // List of all stored companions
    private final NonNullList<ItemStack> companions = NonNullList.create();
    private final List<Integer> pendingCapstoneDrops = new ArrayList<>();

    public CompanionVendingMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntites.COMPANION_VENDING_MACHINE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void equipCompanion(ServerPlayer player, int index, boolean quickEquip) {
        List<ItemStack> list = this.getCompanions();
        if (index < 0 || index >= list.size()) return;

        ItemStack stored = list.get(index);
        if (stored.isEmpty()) return;

        ItemStack newCompanion = stored.copy();
        newCompanion.setCount(1);

        // --- QUICK EQUIP: swap behavior ---
        if (quickEquip) {
            CurioSlotRef equippedSlot = findFirstEquippedCompanionSlot(player);
            if (equippedSlot != null) {
                ItemStack oldCurio = equippedSlot.stacks().getStackInSlot(equippedSlot.index());

                if (!oldCurio.isEmpty() && oldCurio.getItem() instanceof CompanionItem) {
                    ItemStack oldCopy = oldCurio.copy();
                    oldCopy.setCount(1);

                    equippedSlot.stacks().setStackInSlot(equippedSlot.index(), newCompanion);
                    list.set(index, oldCopy);
                    markDirtyAndSync();
                    return;
                }
            }

            CurioSlotRef emptySlot = findFirstEmptyValidCurioSlot(player, newCompanion);
            if (emptySlot != null) {
                emptySlot.stacks().setStackInSlot(emptySlot.index(), newCompanion);
                list.remove(index);
                markDirtyAndSync();
                return;
            }
        }

        // --- NORMAL EQUIP: NO SWAP ---

        boolean added = player.getInventory().add(newCompanion);
        if (!added) player.drop(newCompanion, false);


        list.remove(index);
        markDirtyAndSync();
    }


// -------- helpers --------

    // --------- Curios Helpers ----------
    private static CurioSlotRef findFirstEquippedCompanionSlot(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack -> !stack.isEmpty() && stack.getItem() instanceof CompanionItem)
                .map(CompanionVendingMachineBlockEntity::toSlotRef)
                .orElse(null);
    }

    private static CurioSlotRef findFirstEmptyValidCurioSlot(Player player, ItemStack stackToEquip) {
        final CurioSlotRef[] out = new CurioSlotRef[]{ null };

        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(curios -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
                ICurioStacksHandler handler = entry.getValue();
                if (handler == null) continue;

                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).isEmpty()) continue;

                    if (CuriosApi.getCuriosHelper().isStackValid(
                            new top.theillusivec4.curios.api.SlotContext(entry.getKey(), player, i, false, handler.isVisible()),
                            stackToEquip
                    )) {
                        out[0] = new CurioSlotRef(entry.getKey(), i, stacks);
                        return;
                    }
                }
            }
        });

        return out[0];
    }

    private static CurioSlotRef toSlotRef(SlotResult result) {
        String identifier = result.slotContext().identifier();
        int index = result.slotContext().index();
        return CuriosApi.getCuriosHelper().getCuriosHandler(result.slotContext().entity())
                .resolve()
                .flatMap(curios -> curios.getStacksHandler(identifier))
                .map(ICurioStacksHandler::getStacks)
                .map(stacks -> new CurioSlotRef(identifier, index, stacks))
                .orElse(null);
    }

    public int pullCompanionsFromCurios(ServerPlayer player) {
        final int[] moved = {0};

        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(curios -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
                ICurioStacksHandler handler = entry.getValue();
                if (handler == null) continue;

                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack curio = stacks.getStackInSlot(i);
                    if (curio.isEmpty() || !(curio.getItem() instanceof CompanionItem)) continue;

                    ItemStack toStore = curio.copy();
                    toStore.setCount(1);

                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                    this.insertCompanion(toStore);
                    moved[0]++;
                }
            }
        });

        if (moved[0] > 0) {
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

        return moved[0];
    }

    private void giveToInvOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void markDirtyAndSync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean releaseCompanion(int index) {
        if (level == null || level.isClientSide) {
            return false;
        }

        if (index < 0 || index >= companions.size()) {
            return false;
        }

        ItemStack removed = companions.remove(index);
        if (removed.isEmpty() || !(removed.getItem() instanceof CompanionItem)) {
            return false;
        }

        markDirtyAndSync();

        // Vault Hunters uses a hardcoded 50% capstone roll in ReleasingCompanionEntity.
        if (level.random.nextFloat() >= 0.5F) {
            return true;
        }

        pendingCapstoneDrops.add(10);
        setChanged();
        return true;
    }

    private void spawnReleasedCapstone() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        Direction facing = state.hasProperty(CompanionVendingMachineBlock.FACING)
                ? state.getValue(CompanionVendingMachineBlock.FACING)
                : Direction.NORTH;

        double spawnX = worldPosition.getX() + 0.5D + facing.getStepX() * 0.7D;
        double spawnY = worldPosition.getY();
        double spawnZ = worldPosition.getZ() + 0.5D + facing.getStepZ() * 0.7D;

        ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, new ItemStack(ModItems.COMPANION_CAPSTONE));
        double launchSpeed = 0.1D;
        itemEntity.setDeltaMovement(
                facing.getStepX() * launchSpeed + level.random.nextGaussian() * 0.015D,
                0.08D + level.random.nextGaussian() * 0.01D,
                facing.getStepZ() * launchSpeed + level.random.nextGaussian() * 0.015D
        );
        level.addFreshEntity(itemEntity);
        level.playSound(
                null,
                worldPosition,
                ModSounds.COMPANION_EGG_SPAWN,
                SoundSource.BLOCKS,
                0.05F,
                1.0F
        );
    }

    public List<ItemStack> getCompanions() {
        return this.companions; // or Collections.unmodifiableList, but list is fine here
    }

    public ItemStack getCompanion(int index) {
        if (index < 0 || index >= companions.size()) return ItemStack.EMPTY;
        return companions.get(index);
    }

    public void setCompanion(int index, ItemStack stack) {
        if (index < 0 || index >= companions.size()) return;
        companions.set(index, stack);
        setChanged();
    }

    public ItemStackHandler getSnackHandler() {
        return itemHandler;
    }

    // used by block
    public boolean hasCompanion() {
        return !companions.isEmpty();
    }

    /** Old API name – now just appends to the list. */
    public boolean insertCompanion(ItemStack stack) {
        if (!isCompanion(stack)) return false;

        ItemStack copy = stack.copy();
        copy.setCount(1);
        companions.add(copy);

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null,worldPosition, SoundEvents.ARMOR_EQUIP_DIAMOND, SoundSource.BLOCKS, 0.8f,1.0f);
        }
        return true;
    }

    private static boolean isCompanion(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CompanionItem;
    }

    public boolean consumeOneSnack(Item snackItem) {
        // slot 0 is your only slot
        ItemStack in = itemHandler.getStackInSlot(0);
        if (in.isEmpty() || in.getItem() != snackItem) return false;

        // actually remove 1 (calls onContentsChanged -> setChanged + sendBlockUpdated)
        ItemStack removed = itemHandler.extractItem(0, 1, false);
        return !removed.isEmpty();
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

    public static void tick(Level level, BlockPos pos, BlockState state, CompanionVendingMachineBlockEntity be) {
        if (level.isClientSide || be.pendingCapstoneDrops.isEmpty()) {
            return;
        }

        for (int i = be.pendingCapstoneDrops.size() - 1; i >= 0; i--) {
            int ticksRemaining = be.pendingCapstoneDrops.get(i) - 1;
            if (ticksRemaining <= 0) {
                be.pendingCapstoneDrops.remove(i);
                be.spawnReleasedCapstone();
            } else {
                be.pendingCapstoneDrops.set(i, ticksRemaining);
            }
        }
    }

    // ---------- NBT ----------

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }

        companions.clear();
        if (tag.contains(COMPANIONS_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(COMPANIONS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (!stack.isEmpty()) companions.add(stack);
            }
        }

        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }

        if (tag.contains("relics", Tag.TAG_COMPOUND)) {
            relicHandler.deserializeNBT(tag.getCompound("relics"));
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (owner != null) tag.putUUID("Owner", owner);

        ListTag list = new ListTag();
        for (ItemStack stack : companions) {
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put(COMPANIONS_TAG, list);

        tag.put("inventory", itemHandler.serializeNBT());

        tag.put("relics", relicHandler.serializeNBT());
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(()-> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public void setOwner(Player player) {
        if (owner == null) {
            owner = player.getUUID();
            setChanged();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        BlockPos p = this.getBlockPos();
        // 1x2x1 block volume (lower + upper), plus a little padding for items
        return new net.minecraft.world.phys.AABB(
                p.getX(),     p.getY(),     p.getZ(),
                p.getX() + 1, p.getY() + 2, p.getZ() + 1
        ).inflate(0.25);
    }

    private record CurioSlotRef(String identifier, int index, IDynamicStackHandler stacks) {}

}


