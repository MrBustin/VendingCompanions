package net.bustin.vending_companions.menu;


import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
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

import java.util.List;

public class CompanionVendingMachineMenu extends AbstractContainerMenu {

    private final CompanionVendingMachineBlockEntity blockEntity;
    private final Level level;

    // Internal container for relic slots in the right panel
    private static final int RELIC_SLOT_COUNT = 4;
    private static final int TRAIL_SLOT_COUNT = 3;


    private final Container relicContainer = new SimpleContainer(RELIC_SLOT_COUNT);
    private final Container trailContainer = new SimpleContainer(TRAIL_SLOT_COUNT);

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.COMPANION_VENDING_MACHINE_MENU.get(), pContainerId);
        this.blockEntity = (CompanionVendingMachineBlockEntity) entity;
        this.level = inv.player.level;

        // First: machine slots (relics on the right panel)
        addRelicSlots();
        addTrailSlots();

        // Then: player inventory + hotbar
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

    // ---------- slots ----------

    private void addRelicSlots() {
        int startX = 107; // must match relicSlotOffX
        int startY = 73;  // must match relicSlotOffY

        for (int i = 0; i < 4; i++) {
            int y = startY + i * 18;
            this.addSlot(new Slot(relicContainer, i, startX, y));
        }
    }

    private void addTrailSlots() {
        // Cosmetic / trail slots, horizontal row
        // These should match trailSlotOffX/trailSlotOffY in the Screen.
        int startX = 207;
        int startY = 91;

        for (int i = 0; i < TRAIL_SLOT_COUNT; i++) {
            int y = startY + i * 18; // Verticle row
            int x = startX;
            this.addSlot(new Slot(trailContainer, i, x, y));
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

    // ---------- interaction / cleanup ----------

    // For now, treat machine slots like a chest: allow quick-move between machine and player
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return empty;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        int machineSlots = RELIC_SLOT_COUNT; // 0..3
        int playerInvStart = machineSlots;
        int playerInvEnd = playerInvStart + 27; // main inventory
        int hotbarStart = playerInvEnd;
        int hotbarEnd = hotbarStart + 9;

        // If clicked slot is in machine → move to player
        if (index < machineSlots) {
            if (!this.moveItemStackTo(stackInSlot, playerInvStart, hotbarEnd, true)) {
                return empty;
            }
        } else {
            // From player inventory → try machine relic slots
            if (!this.moveItemStackTo(stackInSlot, 0, machineSlots, false)) {
                return empty;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.COMPANION_VENDING_MACHINE.get());
    }

    // If we close the GUI and there are items in the relic slots,
    // drop them so nothing gets voided (until we wire them to the companion)
    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level.isClientSide) {
            for (int i = 0; i < relicContainer.getContainerSize(); i++) {
                ItemStack stack = relicContainer.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
        }
    }
}


