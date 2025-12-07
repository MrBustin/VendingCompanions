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
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;


import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static iskallia.vault.block.render.CryoChamberRenderer.mc;


public class CompanionVendingMachineScreen extends AbstractContainerScreen<CompanionVendingMachineMenu> {


    //Resource Locations
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_vending_machine_gui.png");

    private static final ResourceLocation GUI_ICONS_LOCATION =
            new ResourceLocation("textures/gui/icons.png");

    private static final ResourceLocation SCROLLBAR_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/scrollbar.png");

    private static final ResourceLocation RELIC_SLOT_BG =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/relic_locked.png");

    private static final ResourceLocation TRAIL_SLOT_BG =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_trail_slot.png");

   private static final ResourceLocation XP_BAR_TEX =
           new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar.png");

    private static final ResourceLocation XP_BAR_FILL_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar_progress.png");


    private static final int XP_BAR_WIDTH  = 64; // <- your texture width
    private static final int XP_BAR_HEIGHT = 8;

    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;

    // how many buttons can be visible at once
    private static final int VISIBLE_ROWS = 5;

    private final List<CompanionDisplayButton> companionButtons = new ArrayList<>();
    private int scrollRowOffset = 0;

    // Scroll area config (box where buttons live)
    private int listX, listY, listWidth, listHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Right-side details panel
    private int detailsX, detailsY, detailsWidth, detailsHeight;
    private int selectedIndex = -1;

    // -------- layout knobs (change these to move stuff around) --------
    // all offsets are relative to (detailsX, detailsY)

    private int relicSlotOffX = 106;
    private int relicSlotOffY = 72;

    private int trailSlotOffX = 207;
    private int trailSlotOffY = 91;


    private int nameOffX = 0;
    private int nameOffY = 7;

    private int heartsOffX = 17;
    private int heartsOffY = 30;

    private int xpOffX = 22;
    private int xpOffY = 140;

    private int statsOffX = 127;
    private int statsOffY = 118;

    private int portraitOffX = 46;
    private int portraitOffY = 100;

    // preview black box behind the companion
    private int previewOffX = 15;
    private int previewOffY = 30;
    private int previewWidth  = 80;
    private int previewHeight = 120;

    // -------------------------------------------------------------------

    public CompanionVendingMachineScreen(CompanionVendingMachineMenu menu,
                                         Inventory inventory,
                                         Component title) {
        super(menu, inventory, title);
        this.imageWidth = 370;
        this.imageHeight = 300;

        if (!menu.getCompanions().isEmpty()) {
            this.selectedIndex = 0;
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.blit(poseStack, x - 65, y - 10, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);

        // --- black preview background behind companion ---
        int boxX1 = detailsX + previewOffX;
        int boxY1 = detailsY + previewOffY;
        int boxX2 = boxX1 + previewWidth;
        int boxY2 = boxY1 + previewHeight;

        // ARGB: 0xFF000000 = solid black
        fill(poseStack, boxX1, boxY1, boxX2, boxY2, 0xFF000000);

        // --- relic slot backgrounds on right panel ---
        RenderSystem.setShaderTexture(0, RELIC_SLOT_BG);

        for (int i = 0; i < 4; i++) {
            int slotX = this.leftPos + relicSlotOffX;
            int slotY = this.topPos  + relicSlotOffY + i * 18; // same spacing as menu

            // assuming relic_slot.png is 18x18
            this.blit(poseStack, slotX, slotY, 0, 0, 18, 18, 18, 18);
        }

        // --- trail slot backgrounds (horizontal row) ---
        RenderSystem.setShaderTexture(0, TRAIL_SLOT_BG);
        for (int i = 0; i < 3; i++) {
            int slotX = this.leftPos + trailSlotOffX;
            int slotY = this.topPos  + trailSlotOffY + i * 18;

            this.blit(poseStack, slotX - 1, slotY - 1, 0, 0, 18, 18, 18, 18);
        }

        // --- draw scrollbar using your PNG ---
        if (maxScroll > 0) {
            RenderSystem.setShaderTexture(0, SCROLLBAR_TEX);

            int barX = listX + listWidth + 4;
            int barY = listY;
            int barWidth = 8;
            int barHeight = listHeight;

            int knobHeight = 12;
            float t = (float) scrollOffset / (float) maxScroll;
            int knobY = barY + (int) ((barHeight - knobHeight) * t);

            this.blit(poseStack, barX, knobY, 0, 0, barWidth, knobHeight);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);

        renderCompanionDetails(poseStack);

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

        this.detailsX = texLeft + 175;
        this.detailsY = texTop + 4;

        this.listX = btnX;
        this.listY = firstBtnY;
        this.listWidth = btnWidth;
        this.listHeight = VISIBLE_ROWS * (btnHeight + spacing);

        companionButtons.clear();
        this.clearWidgets(); // clear previous buttons/widgets

        List<ItemStack> companions = this.menu.getCompanions();

        int maxRows = companions.size();
        int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);
        scrollRowOffset = Mth.clamp(scrollRowOffset, 0, maxScrollRows);

        for (int i = 0; i < companions.size(); i++) {
            int row = i;

            if (row < scrollRowOffset || row >= scrollRowOffset + VISIBLE_ROWS) {
                continue;
            }

            int visualRow = row - scrollRowOffset;
            int y = firstBtnY + visualRow * (btnHeight + spacing);

            CompanionDisplayButton button = new CompanionDisplayButton(
                    btnX,
                    y,
                    btnWidth,
                    btnHeight,
                    this.menu,
                    i,
                    this
            );
            companionButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean overList = mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight;

        if (overList) {
            List<ItemStack> companions = this.menu.getCompanions();

            int maxRows = companions.size();
            int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);

            if (maxScrollRows > 0 && delta != 0) {
                int dir = (int) -Math.signum(delta);
                scrollRowOffset = Mth.clamp(scrollRowOffset + dir, 0, maxScrollRows);

                this.init();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CompanionDisplayButton compBtn : companionButtons) {
            if (compBtn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setSelectedCompanionIndex(int index) {
        this.selectedIndex = index;
    }

    // ------------------- right-panel rendering -------------------

    private void renderCompanionDetails(PoseStack poseStack) {
        List<ItemStack> companions = this.menu.getCompanions();
        if (companions.isEmpty()) return;

        // clamp in case the list changed size
        if (selectedIndex < 0 || selectedIndex >= companions.size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) return;

        int panelX = detailsX;
        int panelY = detailsY;

        renderCompanionName(poseStack, stack, panelX, panelY);
        renderCompanionHeartsAndCooldown(poseStack, stack, panelX, panelY);
        renderCompanionXpBar(poseStack, stack, panelX, panelY);
        renderCompanionStats(poseStack, stack, panelX, panelY);
        renderCompanionPortrait(poseStack, stack, panelX, panelY);
    }

    // Name at top-left of the right panel
    private void renderCompanionName(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        String name = CompanionItem.getPetName(stack);
        if (name == null || name.isEmpty()) name = "Companion";

        this.font.draw(poseStack, name, panelX + nameOffX, panelY + nameOffY, 0x404040);
    }

    // Hearts row + "Ready / Resting / Retired"
    private void renderCompanionHeartsAndCooldown(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int hearts    = CompanionItem.getCompanionHearts(stack);
        int maxHearts = CompanionItem.getCompanionMaxHearts(stack);
        int cooldown  = CompanionItem.getCurrentCooldown(stack);

        RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
        poseStack.pushPose();
        poseStack.translate(0, 0, 10);

        int x = panelX + heartsOffX;
        int y = panelY + heartsOffY;

        for (int i = 0; i < maxHearts; ++i) {
            GuiComponent.blit(poseStack, x + i * 8, y, 16, 0, 9, 9, 256, 256);
            if (i < hearts) {
                GuiComponent.blit(poseStack, x + i * 8, y, 52, 0, 9, 9, 256, 256);
            }
        }

        int separatorX = x + maxHearts * 8 + 2;
        int textX = separatorX + 6;
        int textY = y + 1;

        Minecraft.getInstance().font.draw(poseStack, " | ", separatorX, textY, 0x333333);

        String statusText;
        if (hearts <= 0) {
            statusText = " Retired";
            Minecraft.getInstance().font.drawShadow(poseStack, statusText, textX, textY, 0xFFFFFF);
        } else if (cooldown <= 0) {
            statusText = " Ready";
            Minecraft.getInstance().font.drawShadow(poseStack, statusText, textX, textY, 0x92E27B);
        } else {
            statusText = " Resting " +
                    iskallia.vault.client.gui.helper.UIHelper.formatTimeString((long) cooldown * 20L);
            Minecraft.getInstance().font.draw(poseStack, statusText, textX, textY, 0x333333);
        }

        poseStack.popPose();
    }

    // XP bar + level number
    private void renderCompanionXpBar(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int level = CompanionItem.getCompanionLevel(stack);
        int xp    = CompanionItem.getCompanionXP(stack);
        int xpReq = Math.max(1, CompanionItem.getXPRequiredForNextLevel(stack));
        float progress = (float) xp / (float) xpReq;

        int barX = panelX + xpOffX;
        int barY = panelY + xpOffY;

        // 1) Draw full background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, XP_BAR_TEX);

        GuiComponent.blit(
                poseStack,
                barX, barY,
                0, 0,                    // u, v
                XP_BAR_WIDTH, XP_BAR_HEIGHT,
                XP_BAR_WIDTH, XP_BAR_HEIGHT  // full texture size
        );

        // 2) Draw the filled portion, cropped only in width
        int filledWidth = (int) (progress * XP_BAR_WIDTH);
        if (filledWidth > 0) {
            RenderSystem.setShaderTexture(0, XP_BAR_FILL_TEX);
            GuiComponent.blit(
                    poseStack,
                    barX, barY,
                    0, 0,                    // u, v
                    filledWidth, XP_BAR_HEIGHT,
                    XP_BAR_WIDTH, XP_BAR_HEIGHT
            );
        }

        // 3) Level number above the bar
        String levelStr = String.valueOf(level);
        int textX = barX + (XP_BAR_WIDTH - this.font.width(levelStr)) / 2;
        int textY = barY;
        this.font.drawShadow(poseStack, levelStr, textX, textY, 0xFFF0B100);
    }



    // Right-side stats: "X vaults", "Y days", "Ready"
    private void renderCompanionStats(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int sx = panelX + statsOffX;
        int sy = panelY + statsOffY;

        int vaultRuns = CompanionItem.getVaultRuns(stack);

        long days = 0;
        try {
            java.time.LocalDate date =
                    java.time.LocalDate.parse(CompanionItem.getHatchedDate(stack));
            days = java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDate.now());
        } catch (Exception ignored) {}

        int hearts   = CompanionItem.getCompanionHearts(stack);
        int cooldown = CompanionItem.getCurrentCooldown(stack);

        String status;
        if (hearts <= 0) {
            status = "Retired";
        } else if (cooldown <= 0) {
            status = "Ready";
        } else {
            status = "Resting";
        }

        this.font.draw(poseStack, vaultRuns + " vaults", sx, sy,      0x404040);
        this.font.draw(poseStack, days      + " days",   sx, sy + 12, 0x404040);
        this.font.draw(poseStack, status,                sx, sy + 24, 0x404040);
    }

    // Portrait: just render the item as an icon
    private void renderCompanionPortrait(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        if (stack.isEmpty()) return;

        int px = panelX + portraitOffX;
        int py = panelY + portraitOffY;

        Minecraft.getInstance().getItemRenderer()
                .renderAndDecorateItem(stack, px, py);
    }


}


