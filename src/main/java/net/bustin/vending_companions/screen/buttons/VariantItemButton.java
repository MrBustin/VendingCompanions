package net.bustin.vending_companions.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;



public class VariantItemButton extends Button {
    private final ItemStack icon;
    private final Component tooltip;

    public VariantItemButton(int x, int y, int w, int h, ItemStack icon, Component tooltip, OnPress onPress) {
        super(x, y, w, h, TextComponent.EMPTY, onPress);
        this.icon = icon == null ? ItemStack.EMPTY : icon;
        this.tooltip = tooltip == null ? TextComponent.EMPTY : tooltip;
    }

    public Component getTooltip() {
        return tooltip;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderButton(poseStack, mouseX, mouseY, partialTick);

        if (icon.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int ix = this.x + (this.width - 16) / 2;
        int iy = this.y + (this.height - 16) / 2;

        poseStack.pushPose();
        poseStack.translate(0, 0, 50);

        float oldBlit = mc.getItemRenderer().blitOffset;
        mc.getItemRenderer().blitOffset = 500.0F;

        RenderSystem.enableDepthTest();
        mc.getItemRenderer().renderAndDecorateItem(icon, ix, iy);
        mc.getItemRenderer().renderGuiItemDecorations(mc.font, icon, ix, iy);
        RenderSystem.disableDepthTest(); // ✅ IMPORTANT

        mc.getItemRenderer().blitOffset = oldBlit;
        poseStack.popPose();
    }


}
