package net.bustin.vending_companions.blocks.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class CompanionVendingMachineBlockItem extends BlockItem {

    private static final String COMPANIONS_TAG = "Companions"; // must match your BE tag

    public CompanionVendingMachineBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // your "shulker-style" data should be saved somewhere on the item when broken
        // common patterns: "BlockEntityTag" or your own root tags
        CompoundTag beTag = null;

        if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            beTag = tag.getCompound("BlockEntityTag");
        } else if (tag.contains(COMPANIONS_TAG, Tag.TAG_LIST)) {
            // if you wrote it directly on root (less common, but handle it)
            beTag = tag;
        }

        if (beTag == null) return;

        if (!beTag.contains(COMPANIONS_TAG, Tag.TAG_LIST)) return;

        ListTag list = beTag.getList(COMPANIONS_TAG, Tag.TAG_COMPOUND);

        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.of(list.getCompound(i));
            if (!s.isEmpty()) count++;
        }

        if (count > 0) {
            tooltip.add(new TextComponent("Contains " + count + " companions")
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
