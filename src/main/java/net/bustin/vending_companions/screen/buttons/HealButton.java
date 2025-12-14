package net.bustin.vending_companions.screen.buttons;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class HealButton extends AbstractButton {

    private final CompanionDisplayButton parent;
    private final Runnable onHeal;

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;

    private final int relX;
    private final int relY;

    public HealButton(CompanionDisplayButton parent, int relX, int relY,
                      int w, int h, ResourceLocation normalTex, ResourceLocation hoverTex, Runnable onHeal) {
        super(parent.x + relX, parent.y + relY, w, h, TextComponent.EMPTY);
        this.parent = parent;
        this.relX = relX;
        this.relY = relY;
        this.onHeal = onHeal;
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
    }

    /** Keep button aligned with parent (scrolling) */
    public void syncToParent() {
        this.x = parent.x + relX;
        this.y = parent.y + relY;
        this.visible = parent.visible;
        this.active = parent.active;
    }

    @Override
    public void onPress() {
        onHeal.run();
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        final int TEX_W = 18;
        final int TEX_H = 18;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);

        ResourceLocation tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        RenderSystem.setShaderTexture(0,tex);

        blit(poseStack, this.x, this.y, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
    }

    @Override
    public void updateNarration(NarrationElementOutput out) {}
}

