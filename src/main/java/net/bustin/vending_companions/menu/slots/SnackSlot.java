package net.bustin.vending_companions.menu.slots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SnackSlot extends SlotItemHandler {

    private final Item allowedItem;

    public SnackSlot(IItemHandler handler, int index, int x, int y, Item allowedItem) {
        super(handler, index, x, y);
        this.allowedItem = allowedItem;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Only allow the specific item
        return stack.getItem() == allowedItem;
    }

    // Optional: limit stack size to 1, if you want
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Integer.MAX_VALUE;
    }
}
