package net.bustin.vending_companions.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;



public class VariantToggleButton extends Button {

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;
    private final Component tooltip; // <-- new

    public VariantToggleButton(int x, int y, int width, int height,
                               ResourceLocation normalTex,
                               ResourceLocation hoverTex,
                               Component tooltip,
                               OnPress onPress) {

        super(x, y, width, height, TextComponent.EMPTY, onPress);
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.tooltip = tooltip;
    }

    public Component getTooltip() {
        return tooltip;
    }

    public boolean isMouseOverButton(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);

        ResourceLocation tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        RenderSystem.setShaderTexture(0, tex);

        blit(poseStack,
                this.x, this.y,
                0, 0,
                this.width, this.height,
                this.width, this.height);
    }
}

