package net.bustin.vending_companions.screen;


import com.mojang.blaze3d.vertex.PoseStack;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;


public class CompanionDisplayButton extends AbstractButton {

    private final CompanionVendingMachineMenu menu;
    private final int companionIndex;
    private final CompanionVendingMachineScreen parent;

    public final int baseY; // original Y before scroll

    public CompanionDisplayButton(int x, int y, int width, int height,
                                  CompanionVendingMachineMenu menu,
                                  int companionIndex, CompanionVendingMachineScreen parent) {
        super(x, y, width, height, new TextComponent(""));
        this.menu = menu;
        this.companionIndex = companionIndex;
        this.parent = parent;
        this.baseY = y;
    }

    @Override
    public void onPress() {
        parent.setSelectedCompanionIndex(companionIndex);

    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        int bgColor = this.isHovered ? 0xFF7B7B7B : 0xFF6B6B6B;
        int borderColor = 0xFF000000;

        // background
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, bgColor);
        // border
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + 1, borderColor); // top
        fill(poseStack, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, borderColor); // bottom
        fill(poseStack, this.x, this.y, this.x + 1, this.y + this.height, borderColor); // left
        fill(poseStack, this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, borderColor); // right

        ItemStack stack = menu.getCompanion(companionIndex);
        if (!stack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();

            int itemX = this.x + 2; // left padding
            int itemY = this.y + (this.height - 16) / 2;

            mc.getItemRenderer().renderAndDecorateItem(stack, itemX, itemY);
            mc.getItemRenderer().renderGuiItemDecorations(mc.font, stack, itemX, itemY);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }
}






