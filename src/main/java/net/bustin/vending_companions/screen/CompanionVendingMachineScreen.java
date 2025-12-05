package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.awt.*;

public class CompanionVendingMachineScreen extends AbstractContainerScreen<CompanionVendingMachineMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(VendingCompanions.MOD_ID,"textures/gui/companion_vending_machine_gui.png");
    private CompanionDisplayButton companionDisplayButton;
    // actual texture size
    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;

    public CompanionVendingMachineScreen(CompanionVendingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 370;
        this.imageHeight = 300;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;


        this.blit(poseStack, x-65, y-10, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();

        // position relative to leftPos/topPos (i.e. inside the texture)
        this.titleLabelX = -28;   // vending machine title
        this.titleLabelY = -5;

        this.inventoryLabelX = 110;  // "Inventory" text above player slots
        this.inventoryLabelY = 176;

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop  = (this.height - this.imageHeight) / 2;

        int texLeft = guiLeft - 65;
        int texTop  = guiTop - 10;

        int btnX = texLeft + 40;  // now truly “20 px from left edge of texture”
        int btnY = texTop + 40;

        this.companionDisplayButton = new CompanionDisplayButton(
                btnX, btnY,
                20, 20,
                this.menu
        );

        this.addRenderableWidget(companionDisplayButton);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // title
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        // "Inventory"
        this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
    }
}