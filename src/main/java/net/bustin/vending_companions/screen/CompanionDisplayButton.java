package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;


public class CompanionDisplayButton extends AbstractButton {

    private final CompanionVendingMachineMenu menu;

    public CompanionDisplayButton(int x, int y, int width, int height,
                                  CompanionVendingMachineMenu menu) {
        super(x, y, width, height, new TextComponent(""));
        this.menu = menu;
    }

    @Override
    public void onPress() {
        // no-op for now – we'll add behavior later
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // Draw vanilla button background
        super.renderButton(poseStack, mouseX, mouseY, partialTicks);

        ItemStack stack = menu.getDisplayedCompanion();
        if (!stack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();

            int itemX = this.x + 2;
            int itemY = this.y + 2;

            mc.getItemRenderer().renderAndDecorateItem(stack, itemX, itemY);
            mc.getItemRenderer().renderGuiItemDecorations(mc.font, stack, itemX, itemY);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
