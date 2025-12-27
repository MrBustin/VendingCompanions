package net.bustin.vending_companions.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class FavouriteButton extends AbstractButton {

    private static final int TEX_W = 18;
    private static final int TEX_H = 18;

    private final CompanionDisplayButton parent;
    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;
    private final ResourceLocation favTex;
    private final ResourceLocation favHoverTex;

    private final int relX;
    private final int relY;

    public FavouriteButton(CompanionDisplayButton parent, int relX, int relY,
                           int w, int h,
                           ResourceLocation normalTex, ResourceLocation hoverTex, ResourceLocation favTex, ResourceLocation favHoverTex) {

        // NOW w/h are the actual button size you want on screen
        super(parent.x + relX, parent.y + relY, w, h, TextComponent.EMPTY);

        this.parent = parent;
        this.relX = relX;
        this.relY = relY;
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.favTex = favTex;
        this.favHoverTex = favHoverTex;
    }

    public void syncToParent() {
        this.x = parent.x + relX;
        this.y = parent.y + relY;
        this.visible = parent.visible;
        this.active = parent.active;
    }

    @Override
    public void onPress() {
        parent.toggleFavourite();
    }

    public Component getTooltip(){
        if(parent.isFavourite()){
            return new TextComponent("Click to Unfavorite");
        }else
            return new TextComponent("Click to Favourite");
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        boolean fav = parent.isFavourite(); // asks screen via parent

        ResourceLocation tex;
        if (fav) {
            tex = this.isHoveredOrFocused() ? favHoverTex : favTex;
        } else {
            tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        }

        float sx = (float) this.width  / (float) TEX_W;
        float sy = (float) this.height / (float) TEX_H;

        poseStack.pushPose();
        poseStack.translate(this.x, this.y, 0);
        poseStack.scale(sx, sy, 1.0f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);
        RenderSystem.setShaderTexture(0, tex);

        blit(poseStack, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        poseStack.popPose();
    }



    @Override
    public void updateNarration(NarrationElementOutput out) {}
}



