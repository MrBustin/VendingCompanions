package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.screen.CompanionHomeScreen;
import iskallia.vault.container.CompanionHomeContainer;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static iskallia.vault.block.render.CryoChamberRenderer.mc;


public class CompanionVendingMachineScreen extends AbstractContainerScreen<CompanionVendingMachineMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_vending_machine_gui.png");

    private CompanionDisplayButton companionDisplayButton;
    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;

    private final List<CompanionDisplayButton> companionButtons = new ArrayList<>();

    // how many buttons can be visible at once
    private static final int VISIBLE_ROWS = 5;

    private int scrollRowOffset = 0;

    // Scroll area config (box where buttons live)
    private int listX, listY, listWidth, listHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int detailsX, detailsY, detailsWidth, detailsHeight;
    private int selectedIndex = -1;

    // Your custom scrollbar texture
    private static final ResourceLocation SCROLLBAR_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/scrollbar.png");

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

        this.blit(poseStack, x - 65, y - 10, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);

        // --- draw scrollbar using your PNG ---
        if (maxScroll > 0) {
            RenderSystem.setShaderTexture(0, SCROLLBAR_TEX);

            // Example: full scrollbar area next to the list
            int barX = listX + listWidth + 4;
            int barY = listY;
            int barWidth = 8;
            int barHeight = listHeight;

            // Knob position based on scrollOffset/maxScroll
            int knobHeight = 12; // change to your PNG knob height
            float t = (float) scrollOffset / (float) maxScroll;
            int knobY = barY + (int) ((barHeight - knobHeight) * t);

            // Assuming your PNG is just the knob, at (0,0) of its texture
            this.blit(poseStack, barX, knobY, 0, 0, barWidth, knobHeight);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);

        renderCompanionDetails(poseStack, mouseX, mouseY);

        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();

        // labels
        this.titleLabelX = -28;
        this.titleLabelY = -5;
        this.inventoryLabelX = 110;
        this.inventoryLabelY = 176;

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop  = (this.height - this.imageHeight) / 2;

        int texLeft = guiLeft - 65;
        int texTop  = guiTop - 10;

        int btnX = texLeft + 39;
        int firstBtnY = texTop + 41;

        int btnWidth = 121;
        int btnHeight = 40;
        int spacing = 2;

        // Right-side details panel area
        this.detailsWidth = 165;
        this.detailsHeight = 175;

        this.detailsX = texLeft + 175;  // tweak if needed
        this.detailsY = texTop + 4;

        this.listX = btnX;
        this.listY = firstBtnY;
        this.listWidth = btnWidth;
        this.listHeight = VISIBLE_ROWS * (btnHeight + spacing);

        companionButtons.clear();
        this.clearWidgets(); // clear previous buttons/widgets, slots will be re-added by super

        List<ItemStack> companions = this.menu.getCompanions();

        // ---- ROW-BASED SCROLL SETUP ----
        int maxRows = companions.size();
        int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);
        scrollRowOffset = Mth.clamp(scrollRowOffset, 0, maxScrollRows);

        for (int i = 0; i < companions.size(); i++) {
            int row = i;  // each companion = one row

            // skip rows outside current window [scrollRowOffset, scrollRowOffset + VISIBLE_ROWS)
            if (row < scrollRowOffset || row >= scrollRowOffset + VISIBLE_ROWS) {
                continue;
            }

            int visualRow = row - scrollRowOffset; // 0..VISIBLE_ROWS-1
            int y = firstBtnY + visualRow * (btnHeight + spacing);

            CompanionDisplayButton button = new CompanionDisplayButton(
                    btnX,
                    y,
                    btnWidth,
                    btnHeight,
                    this.menu,
                    i, // still using the real companion index
                    this
            );
            companionButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // title
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        // "Inventory"
        this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Only scroll if the mouse is over the list area
        boolean overList = mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight;

        if (overList) {
            List<ItemStack> companions = this.menu.getCompanions();

            int maxRows = companions.size();
            int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);

            if (maxScrollRows > 0 && delta != 0) {
                // delta > 0 = scroll up, < 0 = scroll down
                int dir = (int) -Math.signum(delta); // same feel as your 1.21 code
                scrollRowOffset = Mth.clamp(scrollRowOffset + dir, 0, maxScrollRows);

                this.init(); // rebuild visible buttons with new offset
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // First, see if any companion button was clicked
        for (CompanionDisplayButton compBtn : companionButtons) {
            if (compBtn.mouseClicked(mouseX, mouseY, button)) {
                return true; // handled
            }
        }
        // Otherwise, let vanilla handle slots, etc.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setSelectedCompanionIndex(int index) {
        this.selectedIndex = index;
    }

    private void renderCompanionDetails(PoseStack poseStack, int mouseX, int mouseY) {
        if (selectedIndex < 0) {
            return; // nothing selected yet
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Panel background (semi-transparent dark)
        int bgColor = 0xAA000000;
        fill(poseStack,
                detailsX, detailsY,
                detailsX + detailsWidth, detailsY + detailsHeight,
                bgColor);

        int x = detailsX + 8;
        int y = detailsY + 8;

        // Name (or default "Companion")
        Component name = stack.getHoverName();
        String nameStr = name.getString();
        if (nameStr == null || nameStr.isEmpty()) {
            nameStr = "Companion";
        }
        mc.font.draw(poseStack, nameStr, x, y, 0xFFFFFF);
        y += 14;

        // Big item icon (like the companion head)
        int iconX = detailsX + 8;
        int iconY = y;
        mc.getItemRenderer().renderAndDecorateItem(stack, iconX, iconY);
        y += 20;

        try {
            // Hearts
            int hearts = CompanionItem.getCompanionHearts(stack);
            int maxHearts = CompanionItem.getCompanionMaxHearts(stack);
            mc.font.draw(poseStack,
                    "Hearts: " + hearts + " / " + maxHearts,
                    x, y, 0xFF5555);
            y += 10;

            // Level + XP
            int level = CompanionItem.getCompanionLevel(stack);
            int xp = CompanionItem.getCompanionXP(stack);
            int xpNext = Math.max(1, CompanionItem.getXPRequiredForNextLevel(stack));
            mc.font.draw(poseStack,
                    "Level: " + level,
                    x, y, 0xFFFFAA);
            y += 10;

            float progress = (float) xp / (float) xpNext;

            // Simple XP bar
            int barWidth = detailsWidth - 16;
            int barHeight = 6;
            int barX = detailsX + 8;
            int barY = y + 2;

            // background bar
            fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            // progress bar
            int filled = (int) (barWidth * progress);
            fill(poseStack, barX, barY, barX + filled, barY + barHeight, 0xFF00FF00);

            mc.font.draw(poseStack,
                    xp + " / " + xpNext + " XP",
                    barX, barY + barHeight + 2, 0xFFFFFF);
            y += barHeight + 12;

            // Optional: vault runs, age, cooldown etc. (mirroring VH)
            int runs = CompanionItem.getVaultRuns(stack);
            mc.font.draw(poseStack,
                    "Vault runs: " + runs,
                    x, y, 0xAAAAAA);
            y += 10;

            // You can add more fields as you want using CompanionItem APIs.
            // For example, hatched date, cooldown, etc.
            // mc.font.draw(...)

        } catch (Throwable t) {
            // If CompanionItem calls break for any reason, at least the panel won't crash the game
            mc.font.draw(poseStack, "Error reading companion data", x, y, 0xFF0000);
        }
    }
}


