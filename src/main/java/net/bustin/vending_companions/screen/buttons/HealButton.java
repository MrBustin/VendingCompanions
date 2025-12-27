package net.bustin.vending_companions.screen.buttons;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.screen.CompanionVendingMachineScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;

public class HealButton extends AbstractButton {

    private static final int TEX_W = 18;
    private static final int TEX_H = 18;

    private boolean hasSnacks = false;

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;
    private final ResourceLocation disabledTex;

    private final Runnable onPress;

    public HealButton(int x, int y, int w, int h,
                      ResourceLocation normalTex,
                      ResourceLocation hoverTex, ResourceLocation dissabledTex,
                      Runnable onPress) {
        super(x, y, w, h, TextComponent.EMPTY);
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.disabledTex = dissabledTex;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    public TextComponent getTooltip() {
        if (!hasSnacks) {
            return new TextComponent("Missing Companion Snacks");
        }
        return new TextComponent("Heal Companion");
    }

    public void setHasSnacks(boolean value) {
        this.hasSnacks = value;
        this.active = value;
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



        ResourceLocation tex;
        if (!hasSnacks) {
            tex = disabledTex;
        } else if (isHoveredOrFocused()) {
            tex = hoverTex;
        } else {
            tex = normalTex;
        }
        RenderSystem.setShaderTexture(0, tex);

        blit(poseStack, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        poseStack.popPose();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if(hasSnacks) {
            soundManager.play(
                    SimpleSoundInstance.forUI(
                            SoundEvents.PLAYER_BURP,
                            1.0f, 1.0f
                    )
            );
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput out) {}
}



