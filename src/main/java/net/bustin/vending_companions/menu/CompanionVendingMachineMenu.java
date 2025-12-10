package net.bustin.vending_companions.menu;


import iskallia.vault.init.ModItems;
import iskallia.vault.item.CompanionItem;
import iskallia.vault.item.CompanionParticleTrailItem;
import iskallia.vault.item.CompanionRelicItem;
import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.bustin.vending_companions.menu.slots.SnackSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompanionVendingMachineMenu extends AbstractContainerMenu {

    public static final int RELIC_SLOT_COUNT = 4;
    public static final int TRAIL_SLOT_COUNT = 3;

    private final CompanionVendingMachineBlockEntity blockEntity;
    private final Level level;
    private final BlockPos blockPos;

    // internal GUI inventories for relic/trail slots
    private final SimpleContainer relicContainer = new SimpleContainer(RELIC_SLOT_COUNT);
    private final SimpleContainer trailContainer = new SimpleContainer(TRAIL_SLOT_COUNT);

    // currently selected companion in the locker (LIVE reference)
    private int selectedIndex = 0;
    private ItemStack companionStack = ItemStack.EMPTY;

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, net.minecraft.world.level.block.entity.BlockEntity entity) {
        super(ModMenuTypes.COMPANION_VENDING_MACHINE_MENU.get(), pContainerId);
        this.blockEntity = (CompanionVendingMachineBlockEntity) entity;
        this.level = inv.player.level;
        this.blockPos = this.blockEntity.getBlockPos();

        // initialise selected companion + GUI relic/trail inventories
        List<ItemStack> comps = blockEntity.getCompanions();
        if (!comps.isEmpty()) {
            this.selectedIndex = 0;
            this.companionStack = comps.get(0); // IMPORTANT: no copy
        }

        refreshRelicAndTrailFromCompanion();

        // slots: relics + trails + player inv + hotbar
        addRelicSlots();
        addTrailSlots();
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ---------- helpers exposed to screen ----------

    public List<ItemStack> getCompanions() {
        return blockEntity.getCompanions();
    }

    public ItemStack getCompanion(int index) {
        return blockEntity.getCompanion(index);
    }

    public CompanionVendingMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    /**
     * Called by the screen when you click a different CompanionDisplayButton.
     */
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;

        List<ItemStack> comps = blockEntity.getCompanions();
        if (index >= 0 && index < comps.size()) {
            this.companionStack = comps.get(index);  // live ref
        } else {
            this.companionStack = ItemStack.EMPTY;
        }

        refreshRelicAndTrailFromCompanion();
    }


    // ---------- relic / trail sync ----------

    private void refreshRelicAndTrailFromCompanion() {
        // clear GUI contents first
        for (int i = 0; i < relicContainer.getContainerSize(); i++) {
            relicContainer.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < trailContainer.getContainerSize(); i++) {
            trailContainer.setItem(i, ItemStack.EMPTY);
        }

        if (!(companionStack.getItem() instanceof CompanionItem)) {
            return;
        }

        // ---- relics -> GUI container (this was wrong before) ----
        for (int i = 0; i < relicContainer.getContainerSize(); ++i) {
            final int slot = i;
            CompanionItem.getRelic(companionStack, slot).ifPresent(mods ->
                    relicContainer.setItem(slot, CompanionRelicItem.create(mods))
            );
        }

        // ---- trails -> GUI container ----
        for (int i = 0; i < trailContainer.getContainerSize(); ++i) {
            int colour = CompanionItem.getCosmeticColour(companionStack, i);
            if (colour != -1) {
                CompanionParticleTrailItem.TrailType type =
                        CompanionItem.getCosmeticTrailType(companionStack, i);
                if (type != null) {
                    trailContainer.setItem(i, CompanionParticleTrailItem.create(colour, type));
                }
            }
        }
    }

    // ---------- slots ----------

    private void addRelicSlots() {
        int startX = 107; // must match relicSlotOffX in screen
        int startY = 73;  // must match relicSlotOffY

        for (int i = 0; i < RELIC_SLOT_COUNT; i++) {
            int y = startY + i * 18;
            this.addSlot(new RelicSlot(relicContainer, i, startX, y));
        }
    }

    private void addTrailSlots() {
        int startX = 207; // must match trailSlotOffX in screen
        int startY = 91;  // must match trailSlotOffY

        for (int i = 0; i < TRAIL_SLOT_COUNT; i++) {
            int y = startY + i * 18;
            this.addSlot(new TrailSlot(trailContainer, i, startX, y));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 110;
        int startY = 187;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = startX + col * 18;
                int y = startY + row * 18;
                int index = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int startX = 110;
        int y = 245;

        for (int i = 0; i < 9; ++i) {
            int x = startX + i * 18;
            this.addSlot(new Slot(playerInventory, i, x, y));
        }
    }

    // ---------- QUICK MOVE (shift-click) ----------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);

        // Mirror VH: don't allow shift-taking relics out via quickMove
        if (slot instanceof RelicSlot relicSlot) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int containerSlots = RELIC_SLOT_COUNT + TRAIL_SLOT_COUNT; // 7

            if (index < containerSlots) {
                // from machine → player
                if (!this.moveItemStackTo(stackInSlot, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.getItem() instanceof CompanionRelicItem) {
                // player → relic slots
                if (!this.moveItemStackTo(stackInSlot, 0, RELIC_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.getItem() instanceof CompanionParticleTrailItem) {
                // player → trail slots
                if (!this.moveItemStackTo(stackInSlot, RELIC_SLOT_COUNT, containerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // non-relic/trail items: let them stay in player inventory
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    // ---------- interaction / cleanup ----------

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.COMPANION_VENDING_MACHINE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // We DON'T drop relicContainer / trailContainer contents.
        // Real data lives in companionStack NBT.
    }

    public void removeCompanionClient(int index) {
        List<ItemStack> comps = this.blockEntity.getCompanions();
        if (index < 0 || index >= comps.size()) {
            return;
        }

        comps.remove(index);

        if (comps.isEmpty()) {
            // no more companions in the locker
            clearSelectedCompanion();
        } else {
            // clamp selection to valid range and point companionStack at new one
            this.selectedIndex = Math.min(this.selectedIndex, comps.size() - 1);
            this.companionStack = comps.get(this.selectedIndex);
            refreshRelicAndTrailFromCompanion();
        }
    }

    public void clearSelectedCompanion() {
        // no active selection
        this.selectedIndex = -1;
        this.companionStack = ItemStack.EMPTY;

        // clear the GUI relic/trail inventories
        refreshRelicAndTrailFromCompanion();
    }

    // ---------- custom slot types (ported from VH, slightly loosened) ----------

    public class RelicSlot extends Slot {
        private final int relicIndex;

        public RelicSlot(Container inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.relicIndex = index;
        }

        public boolean isUnlocked() {
            return companionStack.getItem() instanceof CompanionItem
                    && this.relicIndex < CompanionItem.getRelicSlots(companionStack);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isUnlocked()
                    && stack.getItem() instanceof CompanionRelicItem
                    && !this.hasItem()
                    && CompanionItem.getCompanionHearts(companionStack) > 0;
        }

        @Override
        public boolean mayPickup(Player player) {
            // If you want VH-style "shift only", uncomment this:
            // return this.isUnlocked() && this.hasItem() && player.isShiftKeyDown();
            return this.isUnlocked() && this.hasItem();
        }

        @Override
        public void setChanged() {
            super.setChanged();

            if (!(companionStack.getItem() instanceof CompanionItem)) {
                return;
            }

            ItemStack stack = this.getItem();
            if (stack.isEmpty()) {
                CompanionItem.setRelic(companionStack, this.relicIndex, 0, Collections.emptyList());
            } else {
                List<ResourceLocation> mods = CompanionRelicItem.getModifiers(stack);
                CompanionItem.setRelic(
                        companionStack,
                        this.relicIndex,
                        CompanionRelicItem.getModel(stack),
                        mods
                );
            }

            blockEntity.setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        }

        //tooltip for locked slots
        public List<Component> getUnlockTooltip() {
            int levelReq;
            switch (this.relicIndex) {
                case 0 -> levelReq = 2;
                case 1 -> levelReq = 5;
                case 2 -> levelReq = 8;
                default -> levelReq = 10;
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(new TextComponent("Locked Relic Slot").withStyle(ChatFormatting.RED));
            tooltip.add(new TextComponent("Unlocks at level " + levelReq).withStyle(ChatFormatting.GRAY));
            return tooltip;
        }
    }

    public class TrailSlot extends Slot {
        private final int trailIndex;

        public TrailSlot(Container inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.trailIndex = index;
        }

        public boolean isUnlocked() {
            return companionStack.getItem() instanceof CompanionItem
                    && this.trailIndex < CompanionItem.getCosmeticSlots(companionStack);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isUnlocked()
                    && stack.getItem() instanceof CompanionParticleTrailItem
                    && !this.hasItem()
                    && CompanionItem.getCompanionHearts(companionStack) > 0;
        }

        @Override
        public boolean mayPickup(Player player) {
            // VH behaviour would be: return this.isUnlocked() && this.hasItem() && player.isShiftKeyDown();
            return this.isUnlocked() && this.hasItem();
        }

        @Override
        public void setChanged() {
            super.setChanged();

            if (!(companionStack.getItem() instanceof CompanionItem)) {
                return;
            }

            ItemStack stack = this.getItem();
            if (stack.isEmpty()) {
                CompanionItem.setCosmeticColour(companionStack, this.trailIndex, -1);
                CompanionItem.setCosmeticTrailType(companionStack, this.trailIndex, null);
            } else {
                CompanionItem.setCosmeticColour(
                        companionStack,
                        this.trailIndex,
                        CompanionParticleTrailItem.getColour(stack)
                );
                CompanionItem.setCosmeticTrailType(
                        companionStack,
                        this.trailIndex,
                        CompanionParticleTrailItem.getType(stack)
                );
            }

            blockEntity.setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        }

        // 🔒 NEW: tooltip for locked particle slots
        public List<Component> getUnlockTooltip() {
            int levelReq;
            switch (this.trailIndex) {
                case 0 -> levelReq = 3;
                case 1 -> levelReq = 6;
                default -> levelReq = 10;
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(new TextComponent("Locked Particle Slot").withStyle(ChatFormatting.RED));
            tooltip.add(new TextComponent("Unlocks at level " + levelReq).withStyle(ChatFormatting.GRAY));
            return tooltip;
        }
    }

}


