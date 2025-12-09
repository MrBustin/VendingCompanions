package net.bustin.vending_companions.menu;


import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

public class CompanionVendingMachineMenu extends AbstractContainerMenu {

    private final CompanionVendingMachineBlockEntity blockEntity;
    private final Level level;

    // store the BlockPos so the screen can send it in packets
    private final BlockPos blockPos;

    // Internal container for relic slots in the right panel
    private static final int RELIC_SLOT_COUNT = 4;
    private static final int TRAIL_SLOT_COUNT = 3;

    private final Container relicContainer = new SimpleContainer(RELIC_SLOT_COUNT);
    private final Container trailContainer = new SimpleContainer(TRAIL_SLOT_COUNT);
    private final IItemHandler snackHandler;

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        // Client-side constructor: read pos from buffer, then look up BE
        this(pContainerId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    public CompanionVendingMachineMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.COMPANION_VENDING_MACHINE_MENU.get(), pContainerId);
        this.blockEntity = (CompanionVendingMachineBlockEntity) entity;
        this.level = inv.player.level;
        this.blockPos = this.blockEntity.getBlockPos();

        this.snackHandler = this.blockEntity
                .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("No item handler!"));
        // First: machine slots (relics on the right panel)
        addRelicSlots();
        addTrailSlots();
        addSnackSlot();

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

    // used by your screen to send the BlockPos in the packet
    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void removeCompanionClient(int index) {
        List<ItemStack> list = blockEntity.getCompanions();
        if (list == null) return;
        if (index < 0 || index >= list.size()) return;

        // Mirror server behavior: shrink the list
        list.remove(index);
    }


    // ---------- slots ----------

    private void addRelicSlots() {
        int startX = 107; // must match relicSlotOffX
        int startY = 73;  // must match relicSlotOffY

        for (int i = 0; i < RELIC_SLOT_COUNT; i++) {
            int y = startY + i * 18;
            this.addSlot(new Slot(relicContainer, i, startX, y));
        }
    }

    private void addSnackSlot(){

        int x = -63;
        int y = 52;
        this.addSlot(new SlotItemHandler(snackHandler, 0, x, y));
    }

    private void addTrailSlots() {
        // Cosmetic / trail slots, vertical row
        // These should match trailSlotOffX/trailSlotOffY in the Screen.
        int startX = 207;
        int startY = 91;

        for (int i = 0; i < TRAIL_SLOT_COUNT; i++) {
            int y = startY + i * 18;
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

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return empty;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        // include both relic + trail slots as "machine"
        int machineSlots = RELIC_SLOT_COUNT + TRAIL_SLOT_COUNT+1;
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
            // From player inventory → try machine relic slots (0..RELIC_SLOT_COUNT)
            if (!this.moveItemStackTo(stackInSlot, 0, RELIC_SLOT_COUNT, false)) {
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

