package net.bustin.vending_companions.screen;


import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.network.ModNetworks;
import net.bustin.vending_companions.network.c2s.ChangeCompanionVariantC2SPacket;
import net.bustin.vending_companions.network.c2s.EquipCompanionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;


import iskallia.vault.item.CompanionParticleTrailItem;
import iskallia.vault.item.CompanionPetManager;
import iskallia.vault.item.CompanionSeries;
import iskallia.vault.entity.entity.PetEntity;
import iskallia.vault.entity.entity.pet.PetHelper;
import iskallia.vault.init.ModEntities;


import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;




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


    private static final int XP_BAR_WIDTH  = 64;
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
    private int portraitOffY = 110;

    // preview black box behind the companion
    private int previewOffX = 15;
    private int previewOffY = 30;
    private int previewWidth  = 80;
    private int previewHeight = 120;


    // --- variant slide-out menu state ---
    private boolean variantsOpen = false;   // is the menu open?
    private float variantsAnim = 0.0f;      // 0 = closed, 1 = fully open

    private final List<Button> variantButtons = new ArrayList<>();
    private VariantToggleButton changeModelButton;

    private Button equipButton;


    // entity render cache (similar to VH CompanionHomeScreen)
    private static final Cache<UUID, LivingEntity> CACHED_COMPANIONS =
            CacheBuilder.newBuilder()
                    .maximumSize(10L)
                    .expireAfterAccess(2L, java.util.concurrent.TimeUnit.MINUTES)
                    .build();

    // where the entity is drawn in the black box
    private int companionRenderSize = 30;   // "zoom" of the entity

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

        if (changeModelButton.isMouseOverButton(mouseX, mouseY)) {
            renderTooltip(poseStack, changeModelButton.getTooltip(), mouseX, mouseY);
        }

        renderVariantOverlay(poseStack);

        renderCompanionDetails(poseStack);

        renderCompanionPreviewEntity(poseStack, mouseX, mouseY);

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

        // 🔄 reset widgets & local list
        companionButtons.clear();
        this.clearWidgets();

        // ---------------- CHANGE MODEL BUTTON ----------------
        int cmX = detailsX + detailsWidth - 9;
        int cmY = detailsY ;

        ResourceLocation CYCLE = new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/cycle.png");
        ResourceLocation CYCLE_HOVER = new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/cycle_highlight.png");

        this.changeModelButton = new VariantToggleButton(
                cmX, cmY,
                10, 10,
                CYCLE,
                CYCLE_HOVER,
                new TextComponent("Change Model"),
                btn -> toggleVariantMenu()
        );
        this.addRenderableWidget(this.changeModelButton);
        // -----------------------------------------------------

        // ---------------- EQUP BUTTON ----------------
        int equipWidth  = 60;
        int equipHeight = 20;

// center it under the preview box
        int equipX = detailsX + previewOffX + (previewWidth - equipWidth) / 2;
        int equipY = detailsY + previewOffY + previewHeight + 6;

        this.equipButton = new Button(
                equipX,
                equipY,
                equipWidth,
                equipHeight,
                new TextComponent("Equip"),
                btn -> onEquipClicked()        // <--- callback below
        );
        this.addRenderableWidget(this.equipButton);

        // --------- rebuild companion list buttons ----------
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
            rebuildVariantButtons();
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
        rebuildVariantButtons();
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
        float scale =1.5f;
        String levelStr = String.valueOf(level);


        int textWidth = (int)(this.font.width(levelStr) * scale);
        int textX = (barX + (XP_BAR_WIDTH - this.font.width(levelStr)) / 2)-1;
        int textY = barY -2;
        poseStack.pushPose();
        poseStack.translate(textX, textY, 0);
        poseStack.scale(scale, scale, 1.0f);
        this.font.drawShadow(poseStack, levelStr, 0, 0, 0xFFF0B100);
        poseStack.popPose();
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


    @Nullable
    private LivingEntity getOrCreateCompanionEntity(ItemStack stack) {
        if (stack.isEmpty()) return null;

        UUID uuid = CompanionItem.getCompanionUUID(stack);
        if (uuid == null) {
            return null;
        }

        // cached?
        LivingEntity cached = CACHED_COMPANIONS.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        CompanionSeries series = CompanionItem.getPetSeries(stack);
        String type = CompanionItem.getPetType(stack);

        EntityType<PetEntity> entityType;
        if (series == CompanionSeries.PET) {
            entityType = PetHelper.getVariant(type)
                    .map(PetHelper.PetVariant::entityType)
                    .orElse(ModEntities.PET);
        } else {
            entityType = ModEntities.PET;
        }

        PetEntity pet = entityType.create(level);
        if (pet == null) {
            return null;
        }

        // Apply variant traits / data similar to VH
        if (series == CompanionSeries.PET) {
            PetHelper.getVariant(type).ifPresent(variant -> {
                if (variant.traits() != null) {
                    variant.traits().apply(pet);
                }
            });
        } else {
            pet.setCompanionData(stack);
        }

        // vanilla spawn data
        CompoundTag spawnData = CompanionItem.getSpawnData(stack);
        if (spawnData != null) {
            pet.setVanillaEntityData(spawnData);
        }

        // particle trails / colours
        List<Integer> cols = CompanionItem.getAllCosmeticColours(stack);
        List<CompanionParticleTrailItem.TrailType> types = CompanionItem.getAllCosmeticTrailTypes(stack);
        List<Integer> validCols = new ArrayList<>();
        List<CompanionParticleTrailItem.TrailType> validTypes = new ArrayList<>();

        for (int i = 0; i < cols.size(); ++i) {
            if (i < types.size() && cols.get(i) != -1) {
                validCols.add(cols.get(i));
                validTypes.add(types.get(i));
            }
        }

        pet.setParticleColours(validCols);
        pet.setParticleTrailTypes(validTypes);

        CompanionPetManager.applySkinsToEntity(pet, stack);

        CACHED_COMPANIONS.put(uuid, pet);
        return pet;
    }

    private void renderCompanionPreviewEntity(PoseStack poseStack, int mouseX, int mouseY) {
        // No companions? nothing to draw
        if (this.menu.getCompanions().isEmpty()) {
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= this.menu.getCompanions().size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            return;
        }

        LivingEntity entity = getOrCreateCompanionEntity(stack);
        if (entity == null) {
            return;
        }

        // ---- position inside your black preview box ----
        // center of the box
        int centerX = detailsX + previewOffX + previewWidth  / 2;
        // a bit above the bottom edge so the feet aren't cut off
        int centerY = detailsY + previewOffY + previewHeight - 8 -10;

        // vanilla uses the *difference* between the model position and mouse pos
        float relMouseX = (float) centerX - mouseX;
        float relMouseY = (float) centerY - mouseY;

        // scale/zoom – you already have a knob for this
        int scale = companionRenderSize;

        renderEntityLikeInventory(centerX, centerY, scale, relMouseX, relMouseY, entity);
    }

    private static void renderEntityLikeInventory(
            int x, int y, int scale,
            float mouseX, float mouseY,
            LivingEntity entity
    ) {
        float rotX = (float) Math.atan(mouseX / 40.0F);
        float rotY = (float) Math.atan(mouseY / 40.0F);

        PoseStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushPose();
        mvStack.translate(x, y, 1050.0D);
        mvStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack modelStack = new PoseStack();
        modelStack.translate(0.0D, 0.0D, 1000.0D);
        modelStack.scale(scale, scale, scale);

        Quaternion zRot = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion xRot = Vector3f.XP.rotationDegrees(rotY * 20.0F);
        zRot.mul(xRot);
        modelStack.mulPose(zRot);

        // save original rotations
        float bodyRotY    = entity.yBodyRot;
        float yRot        = entity.getYRot();
        float xRotOld     = entity.getXRot();
        float headRotY0   = entity.yHeadRotO;
        float headRotY    = entity.yHeadRot;

        // apply mouse-based rotation just like the player preview
        entity.yBodyRot = 180.0F + rotX * 20.0F;
        entity.setYRot(180.0F + rotX * 40.0F);
        entity.setXRot(-rotY * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        xRot.conj();
        dispatcher.overrideCameraOrientation(xRot);
        dispatcher.setRenderShadow(false);

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> dispatcher.render(
                entity,
                0.0D, 0.0D, 0.0D,
                0.0F,
                1.0F,
                modelStack,
                buffer,
                15728880
        ));
        buffer.endBatch();

        dispatcher.setRenderShadow(true);

        // restore original rotations
        entity.yBodyRot = bodyRotY;
        entity.setYRot(yRot);
        entity.setXRot(xRotOld);
        entity.yHeadRotO = headRotY0;
        entity.yHeadRot  = headRotY;

        mvStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    private void onChangeModelClicked() {
        Minecraft mc = Minecraft.getInstance();
        try {
            List<ItemStack> companions = this.menu.getCompanions();
            if (companions == null || companions.isEmpty()) {
                return;
            }

            // safety clamp
            if (selectedIndex < 0 || selectedIndex >= companions.size()) {
                selectedIndex = 0;
            }

            ItemStack stack = companions.get(selectedIndex);
            if (stack.isEmpty()) return;

            CompanionSeries series = CompanionItem.getPetSeries(stack);
            String currentType = CompanionItem.getPetType(stack);

            if (currentType == null || currentType.isEmpty()) {
                return; // nothing we can do
            }

            String nextType = null;

            // -------- PET series: cycle through unlocked variants --------
            if (series == CompanionSeries.PET) {
                PetHelper.PetModelType modelType = PetHelper.getModel(currentType).orElse(null);
                if (modelType == null) {
                    return;
                }

                if (mc.player == null) return;

                List<PetHelper.PetVariant> variants = modelType.getVariants(mc.player.getUUID());
                if (variants == null || variants.isEmpty()) {
                    return;
                }

                // find current
                int currentIndex = 0;
                for (int i = 0; i < variants.size(); i++) {
                    if (variants.get(i).type().equalsIgnoreCase(currentType)) {
                        currentIndex = i;
                        break;
                    }
                }

                // pick next variant (wrap)
                int nextIndex = (currentIndex + 1) % variants.size();
                PetHelper.PetVariant nextVariant = variants.get(nextIndex);
                nextType = nextVariant.type();

                // if name still matches old variant default name, update to new displayName
                String currentName = CompanionItem.getPetName(stack);
                String previousDisplayName = variants.get(currentIndex).displayName();
                if (currentName == null || currentName.equalsIgnoreCase(previousDisplayName)) {
                    CompanionItem.setPetName(stack, nextVariant.displayName());
                }
            }
            // -------- LEGEND series: cycle hard-coded forms like VH --------
            else if (series == CompanionSeries.LEGEND) {
                String[] possible = new String[] { "eternal", "giant", "minion", "antlion" };

                int idx = 0;
                for (int i = 0; i < possible.length; i++) {
                    if (possible[i].equalsIgnoreCase(currentType)) {
                        idx = i;
                        break;
                    }
                }

                nextType = possible[(idx + 1) % possible.length];
            }

            // nothing to change
            if (nextType == null || nextType.equalsIgnoreCase(currentType)) {
                return;
            }

            // ---- apply change ONLY to the stack; no list.set(...) ----
            CompanionItem.setPetType(stack, nextType);

            // invalidate cached entity so preview updates
            UUID uuid = CompanionItem.getCompanionUUID(stack);
            if (uuid != null) {
                CACHED_COMPANIONS.invalidate(uuid);
            }

            // debug toast so we know it ran
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Changed model to: " + nextType),
                        true
                );
            }

            BlockPos pos = this.menu.getBlockPos();
            int index = this.selectedIndex;
            String variantType = nextType;

            ModNetworks.CHANNEL.sendToServer(
                    new ChangeCompanionVariantC2SPacket(pos, index, variantType)
            );

        } catch (Exception e) {
            // prevent hard crash and tell us what went wrong
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Error changing model: " + e.getClass().getSimpleName()),
                        true
                );
            }
        }
    }

    private void toggleVariantMenu() {
        this.variantsOpen = !this.variantsOpen;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        // simple open/close tween
        float speed = 0.1f; // higher = snappier

        if (variantsOpen) {
            if (variantsAnim < 1.0f) {
                variantsAnim = Math.min(1.0f, variantsAnim + speed);
            }
        } else {
            if (variantsAnim > 0.0f) {
                variantsAnim = Math.max(0.0f, variantsAnim - speed);
            }
        }

        updateVariantButtonPositions();
    }

    private void rebuildVariantButtons() {
        // remove old ones from the screen
        for (Button b : variantButtons) {
            this.removeWidget(b);
        }
        variantButtons.clear();

        List<ItemStack> companions = this.menu.getCompanions();
        if (companions == null || companions.isEmpty()) return;

        if (selectedIndex < 0 || selectedIndex >= companions.size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) return;

        CompanionSeries series = CompanionItem.getPetSeries(stack);
        String currentType = CompanionItem.getPetType(stack);
        if (currentType == null || currentType.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // collect variant types we want to show
        List<String> variantTypes = new ArrayList<>();

        if (series == CompanionSeries.PET) {
            PetHelper.PetModelType modelType = PetHelper.getModel(currentType).orElse(null);
            if (modelType == null) return;

            List<PetHelper.PetVariant> variants = modelType.getVariants(mc.player.getUUID());
            for (PetHelper.PetVariant v : variants) {
                variantTypes.add(v.type());
            }
        } else if (series == CompanionSeries.LEGEND) {
            // same 4 as VH
            variantTypes.add("eternal");
            variantTypes.add("giant");
            variantTypes.add("minion");
            variantTypes.add("antlion");
        } else {
            // other series don't have variants
            return;
        }

        // base anchor (right side of details panel)
        int baseX = detailsX + detailsWidth + 2 ;
        int baseY = detailsY + 5;

        for (int i = 0; i < variantTypes.size(); i++) {
            String type = variantTypes.get(i);

            // label = first letter (like VH), fallback to "?" if somehow blank
            String labelText;
            if (type.isEmpty()) {
                labelText = "?";
            } else {
                labelText = type.substring(0, 1).toUpperCase();
            }

            int btnWidth = 18;
            int btnHeight = 18;

            // y spacing: 20px between buttons
            int y = baseY + i * 20;

            Button b = new Button(
                    baseX,       // x will be adjusted by animation
                    y,
                    btnWidth,
                    btnHeight,
                    new TextComponent(labelText),
                    btn -> changeVariant(type)   // <-- Click changes to this variant
            );

            variantButtons.add(b);
            this.addRenderableWidget(b);
        }

        // start them hidden if menu is currently closed
        updateVariantButtonPositions();
    }

    private void updateVariantButtonPositions() {
        if (variantButtons.isEmpty()) return;

        int slideDistance = 24; // how far to the right they start when closed

        // final aligned position on the RIGHT of the details panel
        int baseX = detailsX + detailsWidth + 2;
        int baseY = detailsY + 15;

        float perButtonDelay = 0.07f; // delay between buttons for the wave

        for (int i = 0; i < variantButtons.size(); i++) {
            Button b = variantButtons.get(i);

            int y = baseY + i * 20;

            // global animation: 0 -> 1
            // each button starts later by i * perButtonDelay
            float delay = i * perButtonDelay;
            float t;

            if (variantsAnim <= delay) {
                t = 0.0f;
            } else {
                t = (variantsAnim - delay) / (1.0f - delay);
            }

            t = Mth.clamp(t, 0.0f, 1.0f); // per-button eased progress

            // t = 0  -> fully closed (pushed right)
            // t = 1  -> fully open (aligned at baseX)
            int offset = (int) ((1.0f - t) * slideDistance);

            b.x = baseX - offset;  // slide in from the right towards baseX
            b.y = y;

            // keep them clickable while animating open/closed
            b.visible = variantsAnim > 0.02f;
        }
    }





    private void changeVariant(String variantType) {
        Minecraft mc = Minecraft.getInstance();
        try {
            List<ItemStack> companions = this.menu.getCompanions();
            if (companions == null || companions.isEmpty()) return;

            if (selectedIndex < 0 || selectedIndex >= companions.size()) {
                selectedIndex = 0;
            }

            ItemStack stack = this.menu.getCompanion(selectedIndex);
            if (stack.isEmpty()) return;

            CompanionSeries series = CompanionItem.getPetSeries(stack);
            String currentType = CompanionItem.getPetType(stack);

            if (variantType == null || variantType.isEmpty()) return;
            if (currentType != null && variantType.equalsIgnoreCase(currentType)) return;

            // PET: adjust name based on default display names
            if (series == CompanionSeries.PET) {
                PetHelper.PetVariant newVariant = PetHelper.getVariant(variantType).orElse(null);
                if (newVariant == null) return;

                if (currentType != null) {
                    PetHelper.PetVariant previousVariant = PetHelper.getVariant(currentType).orElse(null);
                    if (previousVariant != null) {
                        String currentName = CompanionItem.getPetName(stack);
                        String prevDisplay = previousVariant.displayName();
                        if (currentName == null || currentName.equalsIgnoreCase(prevDisplay)) {
                            CompanionItem.setPetName(stack, newVariant.displayName());
                        }
                    }
                }
            }
            // LEGEND: we just trust variantType is valid

            // apply change locally
            CompanionItem.setPetType(stack, variantType);

            // invalidate cached entity so preview updates
            UUID uuid = CompanionItem.getCompanionUUID(stack);
            if (uuid != null) {
                CACHED_COMPANIONS.invalidate(uuid);
            }

            // debug toast
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Changed model to: " + variantType),
                        true
                );
            }

            // send to server
            ModNetworks.CHANNEL.sendToServer(
                    new ChangeCompanionVariantC2SPacket(
                            this.menu.getBlockPos(),
                            this.selectedIndex,
                            variantType
                    )
            );

        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Error changing model: " + e.getClass().getSimpleName()),
                        true
                );
            }
        }
    }

    private void renderVariantOverlay(PoseStack poseStack) {
        // Screen-space overlay placement
        int overlayX = detailsX + detailsWidth - 22;
        int overlayY = detailsY + 15;
        int overlayWidth  = 27;
        int overlayHeight = 78;

        // Texture-space placement (corresponds to the same region in your GUI texture)
        int panelTexU = 175; // details panel left in texture
        int panelTexV = 4;   // details panel top in texture

        int u = panelTexU + (detailsWidth - 22); // mirror overlayX relative to panel
        int v = panelTexV + 15;

        poseStack.pushPose();
        poseStack.translate(0, 0, 200); // draw over widgets

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        this.blit(
                poseStack,
                overlayX,
                overlayY,
                u,
                v,
                overlayWidth,
                overlayHeight,
                TEX_WIDTH,
                TEX_HEIGHT
        );

        poseStack.popPose();
    }

    private int findNextNonEmptyCompanionIndex() {
        List<ItemStack> comps = this.menu.getCompanions();
        if (comps == null || comps.isEmpty()) {
            return -1; // nothing left
        }

        // 1) try from current index forward
        for (int i = this.selectedIndex; i < comps.size(); i++) {
            if (!comps.get(i).isEmpty()) {
                return i;
            }
        }

        // 2) then backwards
        for (int i = this.selectedIndex - 1; i >= 0; i--) {
            if (!comps.get(i).isEmpty()) {
                return i;
            }
        }

        // 3) no non-empty entries at all
        return -1;
    }

    private void onEquipClicked() {
        Minecraft mc = Minecraft.getInstance();

        // basic client-side sanity
        List<ItemStack> companions = this.menu.getCompanions();
        if (companions == null || companions.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("No companions to equip."),
                        true
                );
            }
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= companions.size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Selected companion is empty."),
                        true
                );
            }
            return;
        }

        BlockPos pos = this.menu.getBlockPos();

        // 1) send to server
        ModNetworks.CHANNEL.sendToServer(
                new EquipCompanionC2SPacket(pos, selectedIndex)
        );

        // 2) mirror removal on client so the button disappears
        this.menu.removeCompanionClient(selectedIndex);

        // 3) fix up selectedIndex after the list shrank
        List<ItemStack> updated = this.menu.getCompanions();
        if (updated.isEmpty()) {
            this.selectedIndex = -1; // nothing left
        } else if (this.selectedIndex >= updated.size()) {
            this.selectedIndex = updated.size() - 1; // clamp to last
        }

        // 4) rebuild all buttons / layout
        this.init();

        if (mc.player != null) {
            mc.player.displayClientMessage(
                    new TextComponent("Equipped companion."),
                    true
            );
        }
    }



}


