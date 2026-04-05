package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.menu.CompanionSearchBar;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.network.ModNetworks;
import net.bustin.vending_companions.network.c2s.EquipCompanionC2SPacket;
import net.bustin.vending_companions.network.c2s.HealCompanionC2SPacket;
import net.bustin.vending_companions.network.c2s.QuickEquipCompanionC2SPacket;
import net.bustin.vending_companions.network.c2s.SelectCompanionC2SPacket;
import net.bustin.vending_companions.screen.buttons.CompanionDisplayButton;
import net.bustin.vending_companions.screen.buttons.HealButton;
import net.bustin.vending_companions.screen.buttons.VariantItemButton;
import net.bustin.vending_companions.screen.buttons.VariantTextButton;
import net.bustin.vending_companions.screen.buttons.VariantToggleButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class CompanionLockerScreen extends AbstractContainerScreen<CompanionVendingMachineMenu> {

    static final int VISIBLE_ROWS = 4;

    private final CompanionLockerListController companionList;
    private final CompanionLockerVariantPanel variantPanel;
    private final CompanionLockerDetailsRenderer detailsRenderer;

    private HealButton healButton;
    private int lastCompanionHash = 0;
    private boolean companionsDirty = true;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    private int detailsX;
    private int detailsY;
    private int detailsWidth;
    private int detailsHeight;
    private int selectedIndex = -1;

    private CompanionSearchBar searchBar;
    private List<Component> queuedPriorityTooltip;
    private int queuedPriorityTooltipMouseX;
    private int queuedPriorityTooltipMouseY;

    private int relicSlotOffX = 106;
    private int relicSlotOffY = 92;
    private int trailSlotOffX = 207;
    private int trailSlotOffY = 93;
    private int nameOffX = 0;
    private int nameOffY = 7;
    private int heartsOffX = 16;
    private int heartsOffY = 51;
    private int xpOffX = 22;
    private int xpOffY = 160;
    private int statsOffX = 120;
    private int statsOffY = 128;
    private int previewOffX = 15;
    private int previewOffY = 50;
    private int previewWidth = 80;
    private int previewHeight = 120;
    private int temporalIconOffX = 117;
    private int temporalIconOffY = 30;
    private int modifierIconsOffX = 111;
    private int modifierIconsOffY = 65;

    public CompanionLockerScreen(CompanionVendingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 370;
        this.imageHeight = 300;
        this.companionList = new CompanionLockerListController(this);
        this.variantPanel = new CompanionLockerVariantPanel(this);
        this.detailsRenderer = new CompanionLockerDetailsRenderer(this);

        if (!menu.getCompanions().isEmpty()) {
            this.selectedIndex = -1;
        }
    }

    @Override
    protected void init() {
        super.init();
        initLabels();
        initLayout();

        this.clearWidgets();
        companionList.clearTrackedButtons();
        variantPanel.clearTrackedButtons();

        variantPanel.initControls();
        initHealButton();
        initSearchBar();

        companionList.rebuildFilteredList();
        if (!companionList.filteredIndices().isEmpty()) {
            setSelectedCompanionIndex(companionList.filteredIndices().get(0));
        } else {
            setSelectedIndexLocal(-1);
        }

        companionList.selectDefaultFromFiltered();
        companionList.rebuildButtons();
        variantPanel.rebuildButtons();

        companionsDirty = true;
        updateHealButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (searchBar != null) {
            searchBar.tick();
        }

        int companionHash = computeCompanionHash();
        if (companionsDirty || companionHash != lastCompanionHash) {
            companionsDirty = false;
            lastCompanionHash = companionHash;
            refreshCompanionUi();
        }

        variantPanel.tickAnimation();

        if (healButton != null) {
            healButton.setHasSnacks(hasSnacks());
            ItemStack companion = menu.getCompanion(selectedIndex);
            boolean needsHeal = needsHealing(companion);
            healButton.visible = needsHeal;
            healButton.active = needsHeal;
        }

        variantPanel.updateButtonPositions();
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        detailsRenderer.renderBackground(poseStack, mouseX, mouseY);
        companionList.renderScrollbar(poseStack, mouseX, mouseY);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        clearPriorityTooltip();

        boolean healWasVisible = healButton != null && healButton.visible;
        if (healButton != null) {
            healButton.visible = false;
        }

        boolean[] previousVariantVisibility = variantPanel.hideButtonsForSuperRender();
        super.render(poseStack, mouseX, mouseY, delta);
        variantPanel.restoreButtonVisibility(previousVariantVisibility);

        variantPanel.renderOverlay(poseStack);
        detailsRenderer.renderCompanionDetails(poseStack, mouseX, mouseY);
        detailsRenderer.renderCompanionPreviewEntity(poseStack, mouseX, mouseY);

        if (healButton != null) {
            healButton.visible = healWasVisible;
        }

        if (healButton != null && healButton.visible) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 500);
            healButton.render(poseStack, mouseX, mouseY, delta);
            poseStack.popPose();
        }

        variantPanel.renderClippedButtons(poseStack, mouseX, mouseY, delta);
        renderHoverTooltips(poseStack, mouseX, mouseY);
        renderPriorityTooltip(poseStack);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
        this.font.draw(poseStack, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040);
    }

    @Override
    protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        if (hoveredSlot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot && !relicSlot.isUnlocked()) {
            this.renderComponentTooltip(poseStack, relicSlot.getUnlockTooltip(), mouseX, mouseY);
            return;
        }

        if (hoveredSlot instanceof CompanionVendingMachineMenu.AncientRelicSlot ancientRelicSlot && !ancientRelicSlot.isUnlocked()) {
            this.renderComponentTooltip(poseStack, ancientRelicSlot.getUnlockTooltip(), mouseX, mouseY);
            return;
        }

        if (hoveredSlot instanceof CompanionVendingMachineMenu.TrailSlot trailSlot && !trailSlot.isUnlocked()) {
            this.renderComponentTooltip(poseStack, trailSlot.getUnlockTooltip(), mouseX, mouseY);
            return;
        }

        super.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    public List<Component> getTooltipFromItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromItem(stack);

        if ((hoveredSlot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot && relicSlot.isUnlocked() && relicSlot.hasItem())
                || (hoveredSlot instanceof CompanionVendingMachineMenu.AncientRelicSlot ancientRelicSlot && ancientRelicSlot.isUnlocked() && ancientRelicSlot.hasItem())) {
            tooltip.add(TextComponent.EMPTY);

            ItemStack selectedCompanion = selectedIndex >= 0 ? menu.getCompanion(selectedIndex) : ItemStack.EMPTY;
            if (!selectedCompanion.isEmpty() && CompanionItem.getCompanionHearts(selectedCompanion) <= 0) {
                tooltip.add(new TextComponent("You cannot take this relic out").withStyle(ChatFormatting.RED));
                return tooltip;
            }

            int cost = iskallia.vault.init.ModConfigs.COMPANIONS.getRelicRemovalCost();
            boolean hasEnough = hasEnoughVaultGoldForHoveredRelicRemoval();

            tooltip.add(new TextComponent("Removal Cost: ").withStyle(ChatFormatting.WHITE)
                    .append(new TextComponent(cost + " Vault Gold").withStyle(hasEnough ? ChatFormatting.GREEN : ChatFormatting.RED)));
            tooltip.add(TextComponent.EMPTY);
            tooltip.add(new TextComponent("Shift left click to remove relic").withStyle(ChatFormatting.GRAY));
        }

        return tooltip;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return companionList.mouseScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (companionList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (companionList.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (companionList.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar != null) {
            EditBox box = searchBar.widget();
            if (keyCode == 256 && box.isFocused()) {
                box.setFocus(false);
                return true;
            }
            if (box.isFocused()) {
                if (box.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBar != null) {
            EditBox box = searchBar.widget();
            if (box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    public void setSelectedCompanionIndex(int index) {
        setSelectedIndexLocal(index);
        updateHealButtonState();

        ModNetworks.CHANNEL.sendToServer(new SelectCompanionC2SPacket(menu.getBlockPos(), index));
        variantPanel.rebuildButtons();
    }

    public void onEquipClicked() {
        Minecraft minecraft = Minecraft.getInstance();
        List<ItemStack> companions = menu.getCompanions();
        if (companions.isEmpty()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(new TextComponent("No companions to equip."), true);
            }
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= companions.size()) {
            selectedIndex = 0;
        }

        ItemStack stack = menu.getCompanion(selectedIndex);
        if (stack.isEmpty()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(new TextComponent("Selected companion is empty."), true);
            }
            return;
        }

        BlockPos pos = menu.getBlockPos();
        ModNetworks.CHANNEL.sendToServer(new EquipCompanionC2SPacket(pos, selectedIndex));

        menu.removeCompanionClient(selectedIndex);
        List<ItemStack> updated = menu.getCompanions();
        if (updated.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= updated.size()) {
            selectedIndex = updated.size() - 1;
        }

        init();
    }

    public void quickEquipFromRow(int realIndex) {
        List<ItemStack> companions = menu.getCompanions();
        if (realIndex < 0 || realIndex >= companions.size()) {
            return;
        }

        ItemStack stack = menu.getCompanion(realIndex);
        if (stack.isEmpty()) {
            return;
        }

        ModNetworks.CHANNEL.sendToServer(new QuickEquipCompanionC2SPacket(menu.getBlockPos(), realIndex));

        menu.removeCompanionClient(realIndex);
        List<ItemStack> updated = menu.getCompanions();
        if (updated.isEmpty()) {
            selectedIndex = -1;
        } else {
            if (selectedIndex >= updated.size()) {
                selectedIndex = updated.size() - 1;
            }
            if (selectedIndex < 0) {
                selectedIndex = 0;
            }
            menu.setSelectedIndex(selectedIndex);
        }

        init();
        companionList.rebuildButtons();
        companionList.rebuildFilteredList();
    }

    public void toggleFavourite(int realIndex) {
        companionList.toggleFavourite(realIndex);
    }

    public boolean isFavourite(int realIndex) {
        return companionList.isFavourite(realIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean needsHealing(ItemStack stack) {
        return CompanionItem.getCompanionHearts(stack) != CompanionItem.getCompanionMaxHearts(stack);
    }

    public boolean hasSnacks() {
        return !menu.getBlockEntity().getSnackHandler().getStackInSlot(0).isEmpty();
    }

    public void onHealPressed(int companionIndex) {
        ModNetworks.CHANNEL.sendToServer(new HealCompanionC2SPacket(menu.getBlockPos(), companionIndex));
    }

    boolean hasEnoughVaultGoldForHoveredRelicRemoval() {
        if (Minecraft.getInstance().player == null) {
            return false;
        }

        int cost = iskallia.vault.init.ModConfigs.COMPANIONS.getRelicRemovalCost();
        var allItems = iskallia.vault.util.InventoryUtil.findAllItems(Minecraft.getInstance().player);
        ItemStack currency = new ItemStack(iskallia.vault.init.ModBlocks.VAULT_GOLD, cost);
        return iskallia.vault.util.CoinDefinition.hasEnoughCurrency(allItems, currency);
    }

    void setSelectedIndexLocal(int index) {
        selectedIndex = index;
        menu.setSelectedIndex(index);
    }

    void invalidatePreviewEntity(UUID uuid) {
        detailsRenderer.invalidatePreview(uuid);
    }

    void showComponentTooltip(PoseStack poseStack, List<Component> tooltip, int mouseX, int mouseY) {
        this.renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
    }

    void queuePriorityTooltip(List<Component> tooltip, int mouseX, int mouseY) {
        this.queuedPriorityTooltip = tooltip;
        this.queuedPriorityTooltipMouseX = mouseX;
        this.queuedPriorityTooltipMouseY = mouseY;
    }

    Font fontRenderer() {
        return this.font;
    }

    CompanionVendingMachineMenu menu() {
        return this.menu;
    }

    CompanionSearchBar searchBar() {
        return searchBar;
    }

    int listX() {
        return listX;
    }

    int listY() {
        return listY;
    }

    int listWidth() {
        return listWidth;
    }

    int listHeight() {
        return listHeight;
    }

    int detailsX() {
        return detailsX;
    }

    int detailsY() {
        return detailsY;
    }

    int detailsWidth() {
        return detailsWidth;
    }

    int detailsHeight() {
        return detailsHeight;
    }

    int relicSlotOffX() {
        return relicSlotOffX;
    }

    int relicSlotOffY() {
        return relicSlotOffY;
    }

    int trailSlotOffX() {
        return trailSlotOffX;
    }

    int trailSlotOffY() {
        return trailSlotOffY;
    }

    int nameOffX() {
        return nameOffX;
    }

    int nameOffY() {
        return nameOffY;
    }

    int heartsOffX() {
        return heartsOffX;
    }

    int heartsOffY() {
        return heartsOffY;
    }

    int xpOffX() {
        return xpOffX;
    }

    int xpOffY() {
        return xpOffY;
    }

    int statsOffX() {
        return statsOffX;
    }

    int statsOffY() {
        return statsOffY;
    }

    int previewOffX() {
        return previewOffX;
    }

    int previewOffY() {
        return previewOffY;
    }

    int previewWidth() {
        return previewWidth;
    }

    int previewHeight() {
        return previewHeight;
    }

    int temporalIconOffX() {
        return temporalIconOffX;
    }

    int temporalIconOffY() {
        return temporalIconOffY;
    }

    int modifierIconsOffX(){
        return modifierIconsOffX;
    }

    int modifierIconsOffY(){
        return modifierIconsOffY;
    }

    int widthValue() {
        return this.width;
    }

    int heightValue() {
        return this.height;
    }

    public int fullWidth() {
        return this.width;
    }

    public int fullHeight() {
        return this.height;
    }

    int leftPosValue() {
        return this.leftPos;
    }

    int topPosValue() {
        return this.topPos;
    }

    int imageWidthValue() {
        return this.imageWidth;
    }

    int imageHeightValue() {
        return this.imageHeight;
    }

    Slot hoveredSlotValue() {
        return this.hoveredSlot;
    }

    <T extends net.minecraft.client.gui.components.AbstractWidget> T addControl(T widget) {
        return this.addRenderableWidget(widget);
    }

    void removeControl(GuiEventListener widget) {
        this.removeWidget(widget);
    }

    private void clearPriorityTooltip() {
        this.queuedPriorityTooltip = null;
    }

    private void renderPriorityTooltip(PoseStack poseStack) {
        if (this.queuedPriorityTooltip == null || this.queuedPriorityTooltip.isEmpty()) {
            return;
        }

        this.renderComponentTooltip(poseStack, this.queuedPriorityTooltip, this.queuedPriorityTooltipMouseX, this.queuedPriorityTooltipMouseY);
        this.queuedPriorityTooltip = null;
    }

    private void initLabels() {
        this.titleLabelX = -40;
        this.titleLabelY = -5;
        this.inventoryLabelX = 110;
        this.inventoryLabelY = 176;
    }

    private void initLayout() {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        int textureLeft = guiLeft - 65;
        int textureTop = guiTop - 10;

        this.detailsWidth = 165;
        this.detailsHeight = 175;
        this.detailsX = textureLeft + 175;
        this.detailsY = textureTop + 4;

        this.listX = textureLeft + 39;
        this.listY = textureTop + 41;
        this.listWidth = 122;
        this.listHeight = VISIBLE_ROWS * (55 + 2);
    }

    private void initHealButton() {
        this.healButton = new HealButton(
                detailsX - 5,
                detailsY + 55,
                18,
                18,
                CompanionLockerTextures.HEAL_BUTTON,
                CompanionLockerTextures.HEAL_BUTTON_HOVER,
                CompanionLockerTextures.HEAL_BUTTON_DISABLED,
                () -> onHealPressed(selectedIndex)
        );
        this.addRenderableWidget(healButton);
    }

    private void initSearchBar() {
        this.searchBar = new CompanionSearchBar(this.font, listX - 8, listY - 22, listWidth, 10);
        this.searchBar.setOnChange(() -> {
            companionList.resetScroll();
            companionList.rebuildFilteredList();
            companionList.rebuildButtons();
        });
        this.addRenderableWidget(searchBar.widget());
    }

    private void renderHoverTooltips(PoseStack poseStack, int mouseX, int mouseY) {
        for (CompanionDisplayButton button : companionList.buttons()) {
            if (button.quickEquipButton != null && button.quickEquipButton.isMouseOver(mouseX, mouseY)) {
                Component tooltip = button.quickEquipButton.getTooltip();
                if (tooltip != null) {
                    this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                }
                return;
            }
        }

        for (CompanionDisplayButton button : companionList.buttons()) {
            if (button.favouriteButton != null && button.favouriteButton.isMouseOver(mouseX, mouseY)) {
                Component tooltip = button.favouriteButton.getTooltip();
                if (tooltip != null) {
                    this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                }
                return;
            }
        }

        VariantToggleButton changeModelButton = variantPanel.changeModelButton();
        if (changeModelButton != null && changeModelButton.isMouseOverButton(mouseX, mouseY)) {
            this.renderTooltip(poseStack, changeModelButton.getTooltip(), mouseX, mouseY);
            return;
        }

        if (healButton != null && healButton.visible && healButton.isMouseOver(mouseX, mouseY)) {
            Component tooltip = healButton.getTooltip();
            if (tooltip != null && !tooltip.getString().isEmpty()) {
                this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                return;
            }
        }

        if (searchBar != null && searchBar.widget().isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = searchBar.getTooltipLines();
            if (tooltip != null && !tooltip.isEmpty() && !tooltip.get(0).getString().isEmpty()) {
                this.renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
            }
        }

        if (variantPanel.animationProgress() >= 1.0F) {
            for (Button button : variantPanel.buttons()) {
                if (button == null || !button.visible || !button.isMouseOver(mouseX, mouseY)) {
                    continue;
                }

                if (button instanceof VariantItemButton itemButton) {
                    Component tooltip = itemButton.getTooltip();
                    if (tooltip != null && !tooltip.getString().isEmpty()) {
                        this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                    }
                } else if (button instanceof VariantTextButton textButton) {
                    Component tooltip = textButton.getTooltip();
                    if (tooltip != null && !tooltip.getString().isEmpty()) {
                        this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                    }
                } else if (button.getMessage() != null && !button.getMessage().getString().isEmpty()) {
                    this.renderTooltip(poseStack, button.getMessage(), mouseX, mouseY);
                }
                return;
            }
        }

        for (CompanionDisplayButton button : companionList.buttons()) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                Component tooltip = button.getToolTip();
                if (tooltip != null && !tooltip.getString().isEmpty()) {
                    this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }

        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    private void updateHealButtonState() {
        if (healButton == null) {
            return;
        }

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
        if (needsHeal) {
            healButton.setHasSnacks(hasSnacks());
        }

        healButton.visible = needsHeal;
        healButton.active = needsHeal;
    }

    private int computeCompanionHash() {
        int hash = 1;
        for (ItemStack stack : menu.getCompanions()) {
            if (stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }

            UUID id = CompanionItem.getCompanionUUID(stack);
            hash = 31 * hash + (id == null ? 0 : id.hashCode());

            String type = CompanionItem.getPetType(stack);
            hash = 31 * hash + (type == null ? 0 : type.hashCode());
            hash = 31 * hash + CompanionItem.getCompanionHearts(stack);
            hash = 31 * hash + CompanionItem.getCompanionLevel(stack);
        }
        return hash;
    }

    private void refreshCompanionUi() {
        companionList.refreshUi();
        variantPanel.rebuildButtons();
        updateHealButtonState();
    }
}
