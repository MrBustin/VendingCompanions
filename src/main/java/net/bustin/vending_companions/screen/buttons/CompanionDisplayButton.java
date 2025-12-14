package net.bustin.vending_companions.screen.buttons;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.screen.CompanionVendingMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;


public class CompanionDisplayButton extends AbstractButton {

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;
    private final CompanionVendingMachineMenu menu;
    private final int companionIndex;
    private final CompanionVendingMachineScreen parent;

    public QuickEquipButton quickEquipButton;
    public FavouriteButton favouriteButton;


    Minecraft mc = Minecraft.getInstance();
    Font font = mc.font;

    public final int baseY; // original Y before scroll

    private static final ResourceLocation XP_BAR_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar.png");

    private static final ResourceLocation XP_BAR_FILL_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar_progress.png");

    public CompanionDisplayButton(int x, int y, int width, int height,ResourceLocation normalTex,
                                  ResourceLocation hoverTex,
                                  CompanionVendingMachineMenu menu,
                                  int companionIndex, CompanionVendingMachineScreen parent) {
        super(x, y, width, height, new TextComponent(""));
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.menu = menu;
        this.companionIndex = companionIndex;
        this.parent = parent;
        this.baseY = y;

        this.quickEquipButton = new QuickEquipButton(
                this,
                100, 33,
                18, 18,
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/quick_equip_button.png"),
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/quick_equip_button_highlighted.png"),
                () -> this.parent.quickEquipFromRow(this.companionIndex)
        );

        this.favouriteButton = new FavouriteButton(
                this,
                108, 4,
                10,10,
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/favourite_button.png"),
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/favourite_button_highlighted.png"),
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/favourite_on_button.png"),
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/favourite_on_button_highlighted.png")
        );
    }

    @Override
    public void onPress() {
        parent.setSelectedCompanionIndex(companionIndex);

    }

    public void toggleFavourite() {
        parent.toggleFavourite(this.companionIndex);
    }

    public boolean isFavourite() {
        return parent.isFavourite(this.companionIndex);
    }


    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        if (quickEquipButton != null) {
            quickEquipButton.syncToParent();
        }

        if (favouriteButton != null) {
            favouriteButton.syncToParent();
        }
        // --- draw full button texture 1:1 ---
        final int TEX_W = 122;
        final int TEX_H = 55;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ResourceLocation tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        RenderSystem.setShaderTexture(0, tex);

        // Draw at the button size (should be 122x55 for Option A)
        blit(poseStack, this.x, this.y, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        RenderSystem.disableBlend();

        // --- render companion item in the slot area ---
        ItemStack stack = menu.getCompanion(companionIndex);
        if (!stack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();

            int itemX = this.x + 7;
            int itemY = this.y + ((this.height - 16) / 2) - 2;

            float scale = 1.75f; // try 1.15f - 1.35f

            RenderSystem.enableDepthTest();

            // Scale around the item position (itemX, itemY)
            PoseStack mv = RenderSystem.getModelViewStack();
            mv.pushPose();
            mv.translate(itemX, itemY, 0.0D);
            mv.scale(scale, scale, 1.0F);
            mv.translate(-itemX, -itemY, 0.0D);
            RenderSystem.applyModelViewMatrix();

            mc.getItemRenderer().renderAndDecorateItem(stack, itemX, itemY);
            mc.getItemRenderer().renderGuiItemDecorations(mc.font, stack, itemX, itemY);

            // restore
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
        }


        // --- render companion xp Bar ---
        renderCompanionLevel(poseStack, stack, this.x,this.y);
        renderCompanionHearts(poseStack, stack, this.x,this.y);
        renderCompanionName(poseStack, stack, this.x,this.y);
        renderTemporalModifier(poseStack,stack,this.x,this.y);
        renderCompanionModifiers(poseStack,stack,this.x,this.y);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // if clicking quick equip area, don't treat as selecting companion
        if (quickEquipButton != null && quickEquipButton.isMouseOver(mouseX, mouseY)) {
            return false; // let quickEquipButton handle it
        }
        if (favouriteButton != null && favouriteButton.isMouseOver(mouseX, mouseY)) {
            return false; // let the child widget handle it
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderCompanionLevel(PoseStack poseStack, ItemStack stack, int panelX, int panelY){

        int xpOffX = 45;
        int xpOffY = 45;

        int XP_BAR_WIDTH = 50;
        int XP_BAR_HEIGHT = 5;

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
        float scale = 1.0f;
        String levelStr = String.valueOf(level);


        int textWidth = (int)(this.font.width(levelStr) * scale);
        int textX = (barX + (XP_BAR_WIDTH - this.font.width(levelStr)) / 2);
        int textY = barY -2;
        poseStack.pushPose();
        poseStack.translate(textX, textY, 0);
        poseStack.scale(scale, scale, 1.0f);

        this.font.draw(poseStack, levelStr, -1,  0, 0xFF000000);
        this.font.draw(poseStack, levelStr,  1,  0, 0xFF000000);
        this.font.draw(poseStack, levelStr,  0, -1, 0xFF000000);
        this.font.draw(poseStack, levelStr,  0,  1, 0xFF000000);

        this.font.draw(poseStack, levelStr, 0, 0, 0xFFF0B100);
        poseStack.popPose();
    }

    private void renderCompanionHearts(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {

        int heartsOffX = 45;
        int heartsOffY = 33;

        int hearts    = CompanionItem.getCompanionHearts(stack);
        int maxHearts = CompanionItem.getCompanionMaxHearts(stack);

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

        Minecraft.getInstance().font.draw(poseStack, "", separatorX, textY, 0x333333);

    }

    private void renderCompanionName(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {

        int nameOffX = 5;
        int nameOffY = 4;
        String name = CompanionItem.getPetName(stack);
        if (name == null || name.isEmpty()) name = "Companion";

        this.font.draw(poseStack, name, panelX + nameOffX, panelY + nameOffY, 0x404040);
    }

    private void renderTemporalModifier(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {

        int temporalIconOffX = 43;
        int temporalIconOffY = 18;

        // 1) Check if this companion has a temporal modifier
        Optional<ResourceLocation> temporalOpt = CompanionItem.getTemporalModifier(stack);
        if (temporalOpt.isEmpty()) return;

        ResourceLocation temporalId = temporalOpt.get();

        // 2) Get VH modifier info (name, color, etc.)
        Optional<VaultModifier<?>> modifierOpt = VaultModifierRegistry.getOpt(temporalId);
        if (modifierOpt.isEmpty()) return;

        VaultModifier<?> modifier = modifierOpt.get(); // (unused right now, but fine)

        // 3) Texture path
        ResourceLocation tex = new ResourceLocation(
                VendingCompanions.MOD_ID,
                "textures/gui/temporal_modifiers/" + temporalId.getPath() + ".png"
        );

        int iconX = panelX + temporalIconOffX;
        int iconY = panelY + temporalIconOffY;

        final int size = 16;        // source icon size
        final float scale = 0.75f;  // <-- smaller icon (0.5f = half size)

        poseStack.pushPose();
        // move to where the icon should be, then scale
        poseStack.translate(iconX, iconY, 200);
        poseStack.scale(scale, scale, 1.0f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, tex);

        // draw at (0,0) because we translated already
        GuiComponent.blit(
                poseStack,
                0, 0,
                0, 0,
                size, size,
                size, size
        );

        poseStack.popPose();
    }

    private void renderCompanionModifiers(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        // anchor = same as temporal
        final int baseOffX = 43;
        final int baseOffY = 18;

        final int srcSize = 16;
        final int padPx = 2;
        final float maxScale = 0.75f;
        final float minScale = 0.45f;

        final int buttonW = 122;
        final int rightEdge = panelX + buttonW - 3;

        final boolean hasTemporal = CompanionItem.getTemporalModifier(stack).isPresent();

        // --- gather relic modifier ids ---
        Map<Integer, Pair<Integer, List<ResourceLocation>>> relicMap = CompanionItem.getAllRelics(stack);
        List<ResourceLocation> ids = new ArrayList<>();
        if (relicMap != null && !relicMap.isEmpty()) {
            for (Pair<Integer, List<ResourceLocation>> entry : relicMap.values()) {
                if (entry == null) continue;
                List<ResourceLocation> list = entry.getSecond();
                if (list != null && !list.isEmpty()) ids.addAll(list);
            }
        }

        if (ids.isEmpty()) return;

        // --- count duplicates (preserve order) ---
        LinkedHashMap<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation id : ids) counts.merge(id, 1, Integer::sum);

        List<ResourceLocation> unique = new ArrayList<>(counts.keySet());

        // companion_challenge last
        unique.sort((a, b) -> {
            boolean ac = a.getPath().equals("companion_challenge");
            boolean bc = b.getPath().equals("companion_challenge");
            if (ac == bc) return 0;
            return ac ? 1 : -1;
        });

        // --- compute scale so everything fits on one line ---
        // start beside temporal (if temporal is present)
        int startX = panelX + baseOffX + (hasTemporal ? (int)(srcSize * maxScale) + padPx : 0);
        int y = panelY + baseOffY;

        int iconCount = unique.size();
        int availableW = rightEdge - startX;

        // required width at scale=1
        int requiredW = iconCount * srcSize + (iconCount - 1) * padPx;

        float scale = Math.min(maxScale, (float) availableW / (float) requiredW);
        if (scale < minScale) scale = minScale;

        int iconW = (int)(srcSize * scale);
        int iconH = (int)(srcSize * scale);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        int x = startX;

        for (ResourceLocation id : unique) {
            int count = counts.getOrDefault(id, 1);
            ResourceLocation tex = getRelicModifierIconTex(id); // your mapping hook

            // icon
            poseStack.pushPose();
            poseStack.translate(x, y, 200);
            poseStack.scale(scale, scale, 1.0f);

            RenderSystem.setShaderTexture(0, tex);
            GuiComponent.blit(poseStack, 0, 0, 0, 0, srcSize, srcSize, srcSize, srcSize);

            poseStack.popPose();

            // xN overlay (half size, bottom-right, transparent bg)
            if (count > 1) {
                String s = "x" + count;
                float textScale = 0.75f;

                int tx = x + iconW - (int)(this.font.width(s) * textScale) + 1;
                int ty = y + iconH - (int)(8 * textScale) + 1;

                poseStack.pushPose();
                poseStack.translate(tx, ty, 300);
                poseStack.scale(textScale, textScale, 1.0f);
                this.font.drawShadow(poseStack, s, 0, 0, 0xFFFFFF);
                poseStack.popPose();
            }

            x += iconW + padPx;
        }

        RenderSystem.disableBlend();
    }
    private ResourceLocation getRelicModifierIconTex(ResourceLocation id) {
        String path = id.getPath();

        return new ResourceLocation(
                VendingCompanions.MOD_ID,
                "textures/gui/modifiers/" + path + ".png"
        );
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }
}






