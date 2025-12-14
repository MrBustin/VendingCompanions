package net.bustin.vending_companions.screen.buttons;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class HealButton extends AbstractButton {

    private static final int TEX_W = 18;
    private static final int TEX_H = 18;

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;

    private final Runnable onPress;
    private final TextComponent tooltip;

    public HealButton(int x, int y, int w, int h,
                      ResourceLocation normalTex,
                      ResourceLocation hoverTex,
                      TextComponent tooltip,
                      Runnable onPress) {
        super(x, y, w, h, TextComponent.EMPTY);
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.tooltip = tooltip;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    public TextComponent getTooltip() {
        return tooltip;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        float sx = (float) this.width  / (float) TEX_W;
        float sy = (float) this.height / (float) TEX_H;

        poseStack.pushPose();
        poseStack.translate(this.x, this.y, 0);
        poseStack.scale(sx, sy, 1.0f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);

        ResourceLocation tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        RenderSystem.setShaderTexture(0, tex);

        blit(poseStack, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        poseStack.popPose();
    }

    @Override
    public void updateNarration(NarrationElementOutput out) {}
}



