package net.bustin.vending_companions.screen;


import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import iskallia.vault.client.gui.helper.UIHelper;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.menu.CompanionSearchBar;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.network.ModNetworks;
import net.bustin.vending_companions.network.c2s.*;
import net.bustin.vending_companions.screen.buttons.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
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
import java.util.Optional;
import java.util.UUID;




public class CompanionVendingMachineScreen extends AbstractContainerScreen<CompanionVendingMachineMenu> {


    // -------------------------------------------------------------------
    // Constants / Resources
    // -------------------------------------------------------------------

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_vending_machine_gui.png");

    private static final ResourceLocation GUI_ICONS_LOCATION =
            new ResourceLocation("textures/gui/icons.png");

    private static final ResourceLocation SCROLLBAR_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/scrollbar.png");

    private static final ResourceLocation RELIC_SLOT_BG_UNLOCKED =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/relic_slot_unlocked.png");
    private static final ResourceLocation RELIC_SLOT_BG_LOCKED =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/relic_slot_locked.png");
    private static final ResourceLocation RELIC_SLOT_BG_FILLED =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/relic_slot_filled.png");

    private static final ResourceLocation TRAIL_SLOT_BG_UNLOCKED =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/trail_slot_unlocked.png");
    private static final ResourceLocation TRAIL_SLOT_BG_LOCKED =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/trail_slot_locked.png");

    private static final ResourceLocation XP_BAR_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar.png");

    private static final ResourceLocation XP_BAR_FILL_TEX =
            new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_xp_bar_progress.png");

    private static final String FAV_TAG = "vc_favourite";

    private HealButton healButton;

    private static final int XP_BAR_WIDTH  = 64;
    private static final int XP_BAR_HEIGHT = 8;

    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;

    private static final int VISIBLE_ROWS = 4;
    private static final int TEMPORAL_ICON_SIZE = 16;

    // -------------------------------------------------------------------
    // State / Layout
    // -------------------------------------------------------------------

    private final List<CompanionDisplayButton> companionButtons = new ArrayList<>();
    private int scrollRowOffset = 0;

    // Scroll area config (box where buttons live)
    private int listX, listY, listWidth, listHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Right-side details panel
    private int detailsX, detailsY, detailsWidth, detailsHeight;
    private int selectedIndex = -1;

    // Search Bar
    private CompanionSearchBar searchBar;
    private List<Integer> filteredIndices = new ArrayList<>();

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

    private int temporalIconOffX = 120; // tweak to taste
    private int temporalIconOffY = 40;

    // -------------------------------------------------------------------
    // Variant slide-out menu state
    // -------------------------------------------------------------------

    private boolean variantsOpen = false;   // is the menu open?
    private float variantsAnim = 0.0f;      // 0 = closed, 1 = fully open

    private final List<Button> variantButtons = new ArrayList<>();
    private VariantToggleButton changeModelButton;

    private Button equipButton;

    // -------------------------------------------------------------------
    // Entity preview cache (similar to VH CompanionHomeScreen)
    // -------------------------------------------------------------------

    private static final Cache<UUID, LivingEntity> CACHED_COMPANIONS =
            CacheBuilder.newBuilder()
                    .maximumSize(10L)
                    .expireAfterAccess(2L, java.util.concurrent.TimeUnit.MINUTES)
                    .build();

    private int companionRenderSize = 30;   // "zoom" of the entity

    // -------------------------------------------------------------------
    // Constructor
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

    // -------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------

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

        int btnWidth = 122;
        int btnHeight = 55;
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



        // reset widgets & local list
        companionButtons.clear();
        this.clearWidgets();

        // ---------------- CHANGE MODEL BUTTON ----------------
        int cmX = detailsX + detailsWidth - 9;
        int cmY = detailsY;

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

        // ---------------- HEAL BUTTON ----------------

        int hbX = detailsX - 5;
        int hbY = detailsY + 35;

        this.healButton = new HealButton(
                hbX, hbY,
                18,18,
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/heal_button.png"),
                new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/heal_button_highlighted.png"),
                new TextComponent("Heal Companion"),
                () -> onHealPressed(this.selectedIndex)

        );


        this.addRenderableWidget(this.healButton);
        // -----------------------------------------------------

        // ---------------- EQUIP BUTTON ----------------
        int equipWidth  = 60;
        int equipHeight = 20;

        int equipX = detailsX + previewOffX + (previewWidth - equipWidth) / 2;
        int equipY = detailsY + previewOffY + previewHeight + 6;

        this.equipButton = new Button(
                equipX,
                equipY,
                equipWidth,
                equipHeight,
                new TextComponent("Equip"),
                btn -> onEquipClicked()
        );
        this.addRenderableWidget(this.equipButton);
        // -----------------------------------------------------

        // ---------------- SEARCH BAR ----------------
        int sbX = listX + 6;
        int sbY = listY - 20;     // above the list
        int sbW = listWidth - 14;
        int sbH = 14;

        this.searchBar = new CompanionSearchBar(this.font, sbX, sbY, sbW, sbH);
        this.searchBar.setOnChange(() -> {
            this.scrollRowOffset = 0;
            rebuildFilteredList();
            rebuildCompanionButtonsOnly();
        });
        this.addRenderableWidget(this.searchBar.widget());
        // -----------------------------------------------------

        // Build filtered list + buttons
        rebuildFilteredList();
        rebuildCompanionButtonsOnly();

        // Variant buttons depend on selection, so do it once at end
        rebuildVariantButtons();

        updateHealButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.searchBar != null) {
            this.searchBar.tick();
        }

        // keep your existing variant tween code below...
        float speed = 0.05f;

        if (variantsOpen) {
            if (variantsAnim < 1.0f) variantsAnim = Math.min(1.0f, variantsAnim + speed);
        } else {
            if (variantsAnim > 0.0f) variantsAnim = Math.max(0.0f, variantsAnim - speed);
        }

        updateVariantButtonPositions();
    }

    // -------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------

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
        for (int i = 0; i < CompanionVendingMachineMenu.RELIC_SLOT_COUNT; i++) {
            int slotX = this.leftPos + relicSlotOffX;
            int slotY = this.topPos  + relicSlotOffY + i * 18;

            Slot slot = this.menu.slots.get(i);

            boolean unlocked = true;
            if (slot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot) {
                unlocked = relicSlot.isUnlocked();
            }

            boolean filled = unlocked && slot.hasItem();

            ResourceLocation tex;
            if (!unlocked) {
                tex = RELIC_SLOT_BG_LOCKED;
            } else if (filled) {
                tex = RELIC_SLOT_BG_FILLED;
            } else {
                tex = RELIC_SLOT_BG_UNLOCKED;
            }

            RenderSystem.setShaderTexture(0, tex);
            this.blit(poseStack, slotX, slotY, 0, 0, 18, 18, 18, 18);
        }

        // --- trail slot backgrounds on right panel ---
        int firstTrailIndex = CompanionVendingMachineMenu.RELIC_SLOT_COUNT; // 4
        for (int i = 0; i < CompanionVendingMachineMenu.TRAIL_SLOT_COUNT; i++) {
            int slotX = this.leftPos + trailSlotOffX;
            int slotY = this.topPos  + trailSlotOffY + i * 18;

            Slot slot = this.menu.slots.get(firstTrailIndex + i);
            boolean unlocked = true;

            if (slot instanceof CompanionVendingMachineMenu.TrailSlot trailSlot) {
                unlocked = trailSlot.isUnlocked();
            }

            ResourceLocation tex = unlocked ? TRAIL_SLOT_BG_UNLOCKED : TRAIL_SLOT_BG_LOCKED;
            RenderSystem.setShaderTexture(0, tex);

            this.blit(poseStack, slotX - 1, slotY - 1, 0, 0, 18, 18, 18, 18);
        }

        // --- draw scrollbar ---
        int maxRows = filteredIndices.size();
        int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);

        if (maxScrollRows > 0) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShaderTexture(0, SCROLLBAR_TEX);

            int barX = listX + listWidth + 4;
            int barY = listY;

            // your on-screen knob size
            int knobW = 8;
            int knobH = 12;

            float t = (float) scrollRowOffset / (float) maxScrollRows;
            int knobY = barY + (int) ((listHeight - knobH) * t);

            // IMPORTANT: texture size is 16x42
            this.blit(poseStack, barX, knobY, 0, 0, knobW, knobH, 16, 42);
        }

    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);

        // hide during super.render so it doesn't get drawn UNDER the hearts
        boolean healWasVisible = this.healButton != null && this.healButton.visible;
        if (this.healButton != null) this.healButton.visible = false;

        super.render(poseStack, mouseX, mouseY, delta);

        // --- YOUR custom renders (hearts etc.) ---
        renderVariantOverlay(poseStack);
        renderCompanionDetails(poseStack, mouseX, mouseY);
        renderCompanionPreviewEntity(poseStack, mouseX, mouseY);

        // restore visibility state
        if (this.healButton != null) this.healButton.visible = healWasVisible;

        // draw heal button on TOP of hearts
        if (this.healButton != null && this.healButton.visible) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 500); // big Z so it sits on top
            this.healButton.render(poseStack, mouseX, mouseY, delta);
            poseStack.popPose();
        }

        // -------- tooltips (after everything is drawn) --------

        // quick equip tooltips
        for (CompanionDisplayButton b : companionButtons) {
            if (b.quickEquipButton != null && b.quickEquipButton.isMouseOver(mouseX, mouseY)) {
                Component tip = b.quickEquipButton.getTooltip();
                if (tip != null) this.renderTooltip(poseStack, tip, mouseX, mouseY);
                return;
            }
        }

        // change-model tooltip
        if (changeModelButton != null && changeModelButton.isMouseOverButton(mouseX, mouseY)) {
            this.renderTooltip(poseStack, changeModelButton.getTooltip(), mouseX, mouseY);
            return;
        }

        // heal tooltip
        if (healButton != null && healButton.visible && healButton.isMouseOver(mouseX, mouseY)) {
            Component tip = healButton.getTooltip(); // no cast
            if (tip != null && !tip.getString().isEmpty()) {
                this.renderTooltip(poseStack, tip, mouseX, mouseY);
                return;
            }
        }

        // variant button tooltips...
        if (variantsAnim >= 1) {
            for (Button b : variantButtons) {
                if (b == null || !b.visible) continue;
                if (b.isMouseOver(mouseX, mouseY)) {
                    if (b instanceof VariantItemButton vib) {
                        Component tip = vib.getTooltip();
                        if (tip != null && !tip.getString().isEmpty()) this.renderTooltip(poseStack, tip, mouseX, mouseY);
                    } else if (b instanceof VariantTextButton vtb) {
                        Component tip = vtb.getTooltip();
                        if (tip != null && !tip.getString().isEmpty()) this.renderTooltip(poseStack, tip, mouseX, mouseY);
                    } else {
                        Component msg = b.getMessage();
                        if (msg != null && !msg.getString().isEmpty()) this.renderTooltip(poseStack, msg, mouseX, mouseY);
                    }
                    return;
                }
            }
        }

        // vanilla slot/item tooltips last
        this.renderTooltip(poseStack, mouseX, mouseY);
    }


    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
    }

    @Override
    protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        // Check for locked relic / trail slots first
        if (this.hoveredSlot != null) {
            Slot slot = this.hoveredSlot;

            if (slot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot) {
                if (!relicSlot.isUnlocked()) {
                    this.renderComponentTooltip(poseStack, relicSlot.getUnlockTooltip(), mouseX, mouseY);
                    return;
                }
            }

            if (slot instanceof CompanionVendingMachineMenu.TrailSlot trailSlot) {
                if (!trailSlot.isUnlocked()) {
                    this.renderComponentTooltip(poseStack, trailSlot.getUnlockTooltip(), mouseX, mouseY);
                    return;
                }
            }
        }

        // otherwise normal behaviour (item tooltips etc.)
        super.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    public List<Component> getTooltipFromItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromItem(stack);

        if (this.hoveredSlot instanceof CompanionVendingMachineMenu.RelicSlot relic) {
            if (relic.isUnlocked() && relic.hasItem()) {
                tooltip.add(TextComponent.EMPTY);

                ItemStack comp = (selectedIndex >= 0) ? this.menu.getCompanion(selectedIndex) : ItemStack.EMPTY;

                if (!comp.isEmpty() && CompanionItem.getCompanionHearts(comp) <= 0) {
                    tooltip.add(new TextComponent("You cannot take this relic out").withStyle(ChatFormatting.RED));
                    return tooltip;
                }

                int cost = iskallia.vault.init.ModConfigs.COMPANIONS.getRelicRemovalCost();

                boolean hasEnough = false;
                if (Minecraft.getInstance().player != null) {
                    // pouch-aware: includes coin pouch + inventory
                    List<iskallia.vault.util.InventoryUtil.ItemAccess> allItems =
                            iskallia.vault.util.InventoryUtil.findAllItems(Minecraft.getInstance().player);

                    ItemStack currency = new ItemStack(iskallia.vault.init.ModBlocks.VAULT_GOLD, cost);
                    hasEnough = iskallia.vault.util.CoinDefinition.hasEnoughCurrency(allItems, currency);
                }

                tooltip.add(new TextComponent("Removal Cost: ").withStyle(ChatFormatting.WHITE)
                        .append(new TextComponent(cost + " Vault Gold")
                                .withStyle(hasEnough ? ChatFormatting.GREEN : ChatFormatting.RED)));

                tooltip.add(TextComponent.EMPTY);
                tooltip.add(new TextComponent("Shift left click to remove relic").withStyle(ChatFormatting.GRAY));
            }
        }

        return tooltip;
    }

    // ------------------- right-panel rendering -------------------

    private void renderCompanionDetails(PoseStack poseStack, int mouseX, int mouseY) {
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
        renderTemporalModifier(poseStack, stack, panelX, panelY, mouseX, mouseY);
    }

    private void renderCompanionName(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        String name = CompanionItem.getPetName(stack);
        if (name == null || name.isEmpty()) name = "Companion";
        this.font.draw(poseStack, name, panelX + nameOffX, panelY + nameOffY, 0x404040);
    }

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

        Minecraft.getInstance().font.draw(poseStack, "", separatorX, textY, 0x333333);
    }

    private void renderTemporalModifier(PoseStack poseStack, ItemStack stack, int panelX, int panelY, int mouseX, int mouseY) {
        Optional<ResourceLocation> temporalOpt = CompanionItem.getTemporalModifier(stack);
        if (temporalOpt.isEmpty()) return;

        ResourceLocation temporalId = temporalOpt.get();

        Optional<VaultModifier<?>> modifierOpt = VaultModifierRegistry.getOpt(temporalId);
        if (modifierOpt.isEmpty()) return;

        VaultModifier<?> modifier = modifierOpt.get();

        ResourceLocation tex = new ResourceLocation(
                VendingCompanions.MOD_ID,
                "textures/gui/temporal_modifiers/" + temporalId.getPath() + ".png"
        );

        int iconX = panelX + temporalIconOffX;
        int iconY = panelY + temporalIconOffY;

        int size  = 16;
        int scale = 2;           // draw 32x32 like VH
        int drawW = size * scale;
        int drawH = size * scale;

        // draw icon
        poseStack.pushPose();
        poseStack.translate(0, 0, 200);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, tex);

        GuiComponent.blit(
                poseStack,
                iconX, iconY,
                0, 0,
                drawW, drawH,
                drawW, drawH
        );

        poseStack.popPose();

        // hover tooltip
        if (mouseX >= iconX && mouseX <= iconX + drawW &&
                mouseY >= iconY && mouseY <= iconY + drawH) {

            List<Component> tooltip = new ArrayList<>();

            tooltip.add(new TextComponent(modifier.getDisplayName())
                    .withStyle(Style.EMPTY.withColor(modifier.getDisplayTextColor())));

            int durationTicks = CompanionItem.getTemporalDuration(stack);
            String durationStr = "Duration: " + UIHelper.formatTimeString(durationTicks);
            tooltip.add(new TextComponent(durationStr).withStyle(ChatFormatting.WHITE));

            this.renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
        }
    }

    private void renderCompanionXpBar(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int level = CompanionItem.getCompanionLevel(stack);
        int xp    = CompanionItem.getCompanionXP(stack);
        int xpReq = Math.max(1, CompanionItem.getXPRequiredForNextLevel(stack));
        float progress = (float) xp / (float) xpReq;

        int barX = panelX + xpOffX;
        int barY = panelY + xpOffY;

        // background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, XP_BAR_TEX);

        GuiComponent.blit(
                poseStack,
                barX, barY,
                0, 0,
                XP_BAR_WIDTH, XP_BAR_HEIGHT,
                XP_BAR_WIDTH, XP_BAR_HEIGHT
        );

        // fill
        int filledWidth = (int) (progress * XP_BAR_WIDTH);
        if (filledWidth > 0) {
            RenderSystem.setShaderTexture(0, XP_BAR_FILL_TEX);
            GuiComponent.blit(
                    poseStack,
                    barX, barY,
                    0, 0,
                    filledWidth, XP_BAR_HEIGHT,
                    XP_BAR_WIDTH, XP_BAR_HEIGHT
            );
        }

        // level number
        float scale = 1.5f;
        String levelStr = String.valueOf(level);

        int textWidth = (int)(this.font.width(levelStr) * scale);
        int textX = (barX + (XP_BAR_WIDTH - this.font.width(levelStr)) / 2) - 1;
        int textY = barY - 2;

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
            status = iskallia.vault.client.gui.helper.UIHelper.formatTimeString((long) cooldown * 20L);
        }

        this.font.draw(poseStack, vaultRuns + " vaults", sx, sy,      0x404040);
        this.font.draw(poseStack, days      + " days",   sx, sy + 12, 0x404040);
        this.font.draw(poseStack, status,                sx, sy + 24, 0x404040);
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

    // -------------------------------------------------------------------
    // Entity preview rendering
    // -------------------------------------------------------------------

    @Nullable
    private LivingEntity getOrCreateCompanionEntity(ItemStack stack) {
        if (stack.isEmpty()) return null;

        UUID uuid = CompanionItem.getCompanionUUID(stack);
        if (uuid == null) return null;

        LivingEntity cached = CACHED_COMPANIONS.getIfPresent(uuid);
        if (cached != null) return cached;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

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
        if (pet == null) return null;

        if (series == CompanionSeries.PET) {
            PetHelper.getVariant(type).ifPresent(variant -> {
                if (variant.traits() != null) {
                    variant.traits().apply(pet);
                }
            });
        } else {
            pet.setCompanionData(stack);
        }

        CompoundTag spawnData = CompanionItem.getSpawnData(stack);
        if (spawnData != null) {
            pet.setVanillaEntityData(spawnData);
        }

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
        if (this.menu.getCompanions().isEmpty()) return;

        if (selectedIndex < 0 || selectedIndex >= this.menu.getCompanions().size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) return;

        LivingEntity entity = getOrCreateCompanionEntity(stack);
        if (entity == null) return;

        int centerX = detailsX + previewOffX + previewWidth  / 2;
        int centerY = detailsY + previewOffY + previewHeight - 8 - 10;

        float relMouseX = (float) centerX - mouseX;
        float relMouseY = (float) centerY - mouseY;

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

        float bodyRotY    = entity.yBodyRot;
        float yRot        = entity.getYRot();
        float xRotOld     = entity.getXRot();
        float headRotY0   = entity.yHeadRotO;
        float headRotY    = entity.yHeadRot;

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

        entity.yBodyRot = bodyRotY;
        entity.setYRot(yRot);
        entity.setXRot(xRotOld);
        entity.yHeadRotO = headRotY0;
        entity.yHeadRot  = headRotY;

        mvStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    // -------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean overList = mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight;

        if (overList) {
            int maxRows = filteredIndices.size();
            int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);

            if (maxScrollRows > 0 && delta != 0) {
                int dir = (int) -Math.signum(delta);
                scrollRowOffset = Mth.clamp(scrollRowOffset + dir, 0, maxScrollRows);

                rebuildCompanionButtonsOnly();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // --- PREVENT "flicker" when not enough Vault Gold (client prediction) ---
        if (button == 0 && hasShiftDown() && this.hoveredSlot instanceof CompanionVendingMachineMenu.RelicSlot relic) {
            if (relic.isUnlocked() && relic.hasItem()) {
                int cost = iskallia.vault.init.ModConfigs.COMPANIONS.getRelicRemovalCost();

                boolean hasEnough = false;
                if (Minecraft.getInstance().player != null) {
                    var allItems = iskallia.vault.util.InventoryUtil.findAllItems(Minecraft.getInstance().player);
                    ItemStack currency = new ItemStack(iskallia.vault.init.ModBlocks.VAULT_GOLD, cost);
                    hasEnough = iskallia.vault.util.CoinDefinition.hasEnoughCurrency(allItems, currency);
                }

                if (!hasEnough) {
                    return true;
                }
            }
        }

        // right-click clear search bar
        if (button == 1 && this.searchBar != null) {
            EditBox box = this.searchBar.widget();
            if (box.isMouseOver(mouseX, mouseY)) {
                this.searchBar.clear();
                rebuildFilteredList();
                rebuildCompanionButtonsOnly();
                return true;
            }
        }

        for (CompanionDisplayButton compBtn : companionButtons) {
            if (compBtn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBar != null) {
            var box = this.searchBar.widget();

            if (keyCode == 256 && box.isFocused()) { // GLFW_KEY_ESCAPE
                box.setFocus(false);
                return true;
            }
            if (box.isFocused()) {
                if (box.keyPressed(keyCode, scanCode, modifiers)) return true;
                return true; // swallow inventory binds while typing
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBar != null) {
            var box = this.searchBar.widget();
            if (box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    // -------------------------------------------------------------------
    // Companion selection / equip actions
    // -------------------------------------------------------------------

    public void setSelectedCompanionIndex(int index) {
        this.selectedIndex = index;

        // update client immediately
        this.menu.setSelectedIndex(index);

        updateHealButtonState();

        // tell server which companion we're editing
        ModNetworks.CHANNEL.sendToServer(
                new SelectCompanionC2SPacket(this.menu.getBlockPos(), index)
        );

        rebuildVariantButtons();
    }

    private int findNextNonEmptyCompanionIndex() {
        List<ItemStack> comps = this.menu.getCompanions();
        if (comps == null || comps.isEmpty()) {
            return -1;
        }

        for (int i = this.selectedIndex; i < comps.size(); i++) {
            if (!comps.get(i).isEmpty()) return i;
        }

        for (int i = this.selectedIndex - 1; i >= 0; i--) {
            if (!comps.get(i).isEmpty()) return i;
        }

        return -1;
    }

    private void onEquipClicked() {
        Minecraft mc = Minecraft.getInstance();

        List<ItemStack> companions = this.menu.getCompanions();
        if (companions == null || companions.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent("No companions to equip."), true);
            }
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= companions.size()) {
            selectedIndex = 0;
        }

        ItemStack stack = this.menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent("Selected companion is empty."), true);
            }
            return;
        }

        BlockPos pos = this.menu.getBlockPos();

        ModNetworks.CHANNEL.sendToServer(new EquipCompanionC2SPacket(pos, selectedIndex));

        this.menu.removeCompanionClient(selectedIndex);

        List<ItemStack> updated = this.menu.getCompanions();
        if (updated.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex >= updated.size()) {
            this.selectedIndex = updated.size() - 1;
        }

        this.init();

        if (mc.player != null) {
            mc.player.displayClientMessage(new TextComponent("Equipped companion."), true);
        }
    }

    public void quickEquipFromRow(int realIndex) {
        List<ItemStack> companions = this.menu.getCompanions();
        if (companions == null || companions.isEmpty()) return;
        if (realIndex < 0 || realIndex >= companions.size()) return;

        ItemStack stack = this.menu.getCompanion(realIndex);
        if (stack.isEmpty()) return;

        BlockPos pos = this.menu.getBlockPos();

        ModNetworks.CHANNEL.sendToServer(new EquipCompanionC2SPacket(pos, realIndex));

        this.menu.removeCompanionClient(realIndex);

        List<ItemStack> updated = this.menu.getCompanions();
        if (updated.isEmpty()) {
            this.selectedIndex = -1;
        } else {
            if (this.selectedIndex >= updated.size()) this.selectedIndex = updated.size() - 1;
            if (this.selectedIndex < 0) this.selectedIndex = 0;
            this.menu.setSelectedIndex(this.selectedIndex);
        }

        this.init();
    }

    // -------------------------------------------------------------------
    // Healing
    // -------------------------------------------------------------------

    public boolean needsHealing(ItemStack stack){
        int hearts    = CompanionItem.getCompanionHearts(stack);
        int maxHearts = CompanionItem.getCompanionMaxHearts(stack);

        return hearts != maxHearts;
    }

    private void updateHealButtonState() {
        if (healButton == null) return;

        if (selectedIndex < 0 || selectedIndex >= menu.getCompanions().size()) {
            healButton.visible = false;
            return;
        }

        ItemStack stack = menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            healButton.visible = false;
            return;
        }

        boolean needsHeal = needsHealing(stack);

        healButton.visible = needsHeal;
        healButton.active  = needsHeal;
    }

    public void onHealPressed(int companionIndex) {
        ModNetworks.CHANNEL.sendToServer(
                new HealCompanionC2SPacket(this.menu.getBlockPos(),companionIndex)
        );

    }

    // -------------------------------------------------------------------
    // Variant menu actions
    // -------------------------------------------------------------------

    private void toggleVariantMenu() {
        this.variantsOpen = !this.variantsOpen;
    }

    private void onChangeModelClicked() {
        Minecraft mc = Minecraft.getInstance();
        try {
            List<ItemStack> companions = this.menu.getCompanions();
            if (companions == null || companions.isEmpty()) {
                return;
            }

            if (selectedIndex < 0 || selectedIndex >= companions.size()) {
                selectedIndex = 0;
            }

            ItemStack stack = companions.get(selectedIndex);
            if (stack.isEmpty()) return;

            CompanionSeries series = CompanionItem.getPetSeries(stack);
            String currentType = CompanionItem.getPetType(stack);

            if (currentType == null || currentType.isEmpty()) {
                return;
            }

            String nextType = null;

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

                int currentIndex = 0;
                for (int i = 0; i < variants.size(); i++) {
                    if (variants.get(i).type().equalsIgnoreCase(currentType)) {
                        currentIndex = i;
                        break;
                    }
                }

                int nextIndex = (currentIndex + 1) % variants.size();
                PetHelper.PetVariant nextVariant = variants.get(nextIndex);
                nextType = nextVariant.type();

                String currentName = CompanionItem.getPetName(stack);
                String previousDisplayName = variants.get(currentIndex).displayName();
                if (currentName == null || currentName.equalsIgnoreCase(previousDisplayName)) {
                    CompanionItem.setPetName(stack, nextVariant.displayName());
                }
            } else if (series == CompanionSeries.LEGEND) {
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

            if (nextType == null || nextType.equalsIgnoreCase(currentType)) {
                return;
            }

            CompanionItem.setPetType(stack, nextType);

            UUID uuid = CompanionItem.getCompanionUUID(stack);
            if (uuid != null) {
                CACHED_COMPANIONS.invalidate(uuid);
            }

            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent("Changed model to: " + nextType), true);
            }

            BlockPos pos = this.menu.getBlockPos();
            int index = this.selectedIndex;
            String variantType = nextType;

            ModNetworks.CHANNEL.sendToServer(new ChangeCompanionVariantC2SPacket(pos, index, variantType));

        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponent("Error changing model: " + e.getClass().getSimpleName()),
                        true
                );
            }
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

            CompanionItem.setPetType(stack, variantType);

            UUID uuid = CompanionItem.getCompanionUUID(stack);
            if (uuid != null) {
                CACHED_COMPANIONS.invalidate(uuid);
            }

            if (mc.player != null) {
                mc.player.displayClientMessage(new TextComponent("Changed model to: " + variantType), true);
            }

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

    // -------------------------------------------------------------------
    // Variant menu building / animation
    // -------------------------------------------------------------------

    private void rebuildVariantButtons() {
        for (Button b : variantButtons) this.removeWidget(b);
        variantButtons.clear();

        List<ItemStack> companions = this.menu.getCompanions();
        if (companions == null || companions.isEmpty()) return;

        if (selectedIndex < 0 || selectedIndex >= companions.size()) selectedIndex = 0;

        ItemStack baseStack = this.menu.getCompanion(selectedIndex);
        if (baseStack.isEmpty()) return;

        CompanionSeries series = CompanionItem.getPetSeries(baseStack);
        String currentType = CompanionItem.getPetType(baseStack);
        if (currentType == null || currentType.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int baseX = detailsX + detailsWidth + 2;
        int baseY = detailsY + 5;

        int btnWidth = 18;
        int btnHeight = 18;

        if (series == CompanionSeries.PET) {
            PetHelper.PetModelType modelType = PetHelper.getModel(currentType).orElse(null);
            if (modelType == null) return;

            List<PetHelper.PetVariant> variants = modelType.getVariants(mc.player.getUUID());
            for (int i = 0; i < variants.size(); i++) {
                PetHelper.PetVariant v = variants.get(i);
                String type = v.type();
                if (type == null || type.isEmpty()) continue;

                int y = baseY + i * 20;

                ItemStack icon = baseStack.copy();
                CompanionItem.setPetType(icon, type);

                Component tip = new TextComponent(v.displayName());

                Button b = new VariantItemButton(
                        baseX, y, 18, 18,
                        icon,
                        tip,
                        btn -> changeVariant(type)
                );

                variantButtons.add(b);
                this.addRenderableWidget(b);
            }
        } else if (series == CompanionSeries.LEGEND) {
            String[] types = new String[] { "eternal", "giant", "minion", "antlion" };

            for (int i = 0; i < types.length; i++) {
                String type = types[i];
                int y = baseY + i * 20;

                String labelText = type.substring(0, 1).toUpperCase();
                Component tooltip = new TextComponent(type.substring(0, 1).toUpperCase() + type.substring(1));

                Button b = new VariantTextButton(
                        baseX,
                        y,
                        18,
                        18,
                        new TextComponent(labelText),
                        tooltip,
                        btn -> changeVariant(type)
                );

                variantButtons.add(b);
                this.addRenderableWidget(b);
            }
        } else {
            return;
        }

        updateVariantButtonPositions();
    }

    private void updateVariantButtonPositions() {
        if (variantButtons.isEmpty()) return;

        int slideDistance = 24;

        int baseX = detailsX + detailsWidth + 2;
        int baseY = detailsY + 15;

        float perButtonDelay = 0.07f;

        for (int i = 0; i < variantButtons.size(); i++) {
            Button b = variantButtons.get(i);

            int y = baseY + i * 20;

            float delay = i * perButtonDelay;
            float t;

            if (variantsAnim <= delay) {
                t = 0.0f;
            } else {
                t = (variantsAnim - delay) / (1.0f - delay);
            }

            t = Mth.clamp(t, 0.0f, 1.0f);

            int offset = (int) ((1.0f - t) * slideDistance);

            b.x = baseX - offset;
            b.y = y;

            b.visible = variantsAnim > 0.02f;
        }
    }

    // -------------------------------------------------------------------
    // Companion list / buttons rebuilding
    // -------------------------------------------------------------------

    private void rebuildFilteredList() {
        List<ItemStack> companions = this.menu.getCompanions();

        if (this.searchBar == null) {
            this.filteredIndices = new ArrayList<>();
            for (int i = 0; i < companions.size(); i++) this.filteredIndices.add(i);
        } else {
            this.filteredIndices = this.searchBar.filter(companions);
        }

        if (this.selectedIndex >= 0 && !this.filteredIndices.contains(this.selectedIndex)) {
            this.selectedIndex = this.filteredIndices.isEmpty() ? -1 : this.filteredIndices.get(0);
            this.menu.setSelectedIndex(this.selectedIndex);
        }

        if (this.selectedIndex == -1 && !this.filteredIndices.isEmpty()) {
            this.selectedIndex = this.filteredIndices.get(0);
            this.menu.setSelectedIndex(this.selectedIndex);
        }

        // favourites first (stable within each group)
        this.filteredIndices.sort((a, b) -> {
            boolean fa = isFavouriteIndex(a);
            boolean fb = isFavouriteIndex(b);
            if (fa != fb) return fa ? -1 : 1;
            return Integer.compare(a, b);
        });
    }

    private void rebuildCompanionButtonsOnly() {
        // remove old companion row widgets only (leave searchbar, equip button, etc)
        for (CompanionDisplayButton b : companionButtons) {
            this.removeWidget(b);
            this.removeWidget(b.favouriteButton);
        }

        for (CompanionDisplayButton b : companionButtons) {
            this.removeWidget(b);
            this.removeWidget(b.quickEquipButton);
        }

        companionButtons.clear();

        int btnX = this.listX;
        int firstBtnY = this.listY;
        int btnWidth = this.listWidth;
        int btnHeight = 55;
        int spacing = 2;

        int maxRows = filteredIndices.size();
        int maxScrollRows = Math.max(0, maxRows - VISIBLE_ROWS);
        scrollRowOffset = Mth.clamp(scrollRowOffset, 0, maxScrollRows);

        for (int row = scrollRowOffset; row < Math.min(maxRows, scrollRowOffset + VISIBLE_ROWS); row++) {
            int realIndex = filteredIndices.get(row);

            int visualRow = row - scrollRowOffset;
            int y = firstBtnY + visualRow * (btnHeight + spacing);

            CompanionDisplayButton button = new CompanionDisplayButton(
                    btnX,
                    y,
                    btnWidth,
                    btnHeight,
                    new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_display_button.png"),
                    new ResourceLocation(VendingCompanions.MOD_ID, "textures/gui/companion_display_button_highlighted.png"),
                    this.menu,
                    realIndex,
                    this
            );

            companionButtons.add(button);
            this.addRenderableWidget(button);
            this.addRenderableWidget(button.quickEquipButton);
            this.addRenderableWidget(button.favouriteButton);
        }
    }

    // -------------------------------------------------------------------
    // Favourites
    // -------------------------------------------------------------------

    private boolean isFavouriteIndex(int realIndex) {
        List<ItemStack> comps = this.menu.getCompanions();
        if (comps == null || realIndex < 0 || realIndex >= comps.size()) return false;

        ItemStack s = comps.get(realIndex);
        return !s.isEmpty() && s.hasTag() && s.getTag().getBoolean(FAV_TAG);
    }

    public void toggleFavourite(int realIndex) {
        List<ItemStack> comps = this.menu.getCompanions();
        if (comps == null || comps.isEmpty()) return;
        if (realIndex < 0 || realIndex >= comps.size()) return;

        ItemStack stack = comps.get(realIndex);
        if (stack.isEmpty()) return;

        boolean newFav = !isFavouriteIndex(realIndex);
        stack.getOrCreateTag().putBoolean(FAV_TAG, newFav);

        ModNetworks.CHANNEL.sendToServer(
                new ToggleFavouriteC2SPacket(this.menu.getBlockPos(), realIndex, newFav)
        );

        rebuildFilteredList();
        rebuildCompanionButtonsOnly();
    }

    public boolean isFavourite(int realIndex) {
        return isFavouriteIndex(realIndex);
    }
}


