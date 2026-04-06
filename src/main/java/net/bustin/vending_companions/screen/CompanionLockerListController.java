package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.menu.CompanionSearchBar;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.bustin.vending_companions.network.ModNetworks;
import net.bustin.vending_companions.network.c2s.ToggleFavouriteC2SPacket;
import net.bustin.vending_companions.screen.buttons.CompanionDisplayButton;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CompanionLockerListController {

    private static final String FAV_TAG = "vc_favourite";
    private static final String FAV_TIME_TAG = "vc_favourite_time";
    private static final int BUTTON_HEIGHT = 55;
    private static final int BUTTON_SPACING = 2;
    private static final int BUTTON_STRIDE = BUTTON_HEIGHT + BUTTON_SPACING;
    private static final int MOUSE_WHEEL_SCROLL_STEP = 20;
    private static final int MIN_SCROLLBAR_HEIGHT = 18;
    private static final int SCROLLBAR_TEX_W = 16;
    private static final int SCROLLBAR_TEX_H = 42;
    private static final int SCROLLBAR_CAP_H = 4;

    private final CompanionLockerScreen screen;
    private final List<CompanionDisplayButton> companionButtons = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();
    private final Map<UUID, Boolean> favouriteCache = new HashMap<>();
    private final Map<UUID, Long> favouriteTimeCache = new HashMap<>();

    private int scrollOffsetPx = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarGrabOffsetY = 0;

    CompanionLockerListController(CompanionLockerScreen screen) {
        this.screen = screen;
    }

    List<CompanionDisplayButton> buttons() {
        return companionButtons;
    }

    List<Integer> filteredIndices() {
        return filteredIndices;
    }

    void clearTrackedButtons() {
        companionButtons.clear();
    }

    void resetScroll() {
        scrollOffsetPx = 0;
    }

    void renderScrollbar(PoseStack poseStack, int mouseX, int mouseY) {
        int maxScrollPixels = getMaxScrollPixels();
        int knobY = maxScrollPixels > 0 ? getKnobY() : getBaseY();

        ResourceLocation texture;
        if (maxScrollPixels <= 0) {
            texture = CompanionLockerTextures.SCROLLBAR_DISABLED;
        } else {
            texture = draggingScrollbar || isMouseOverKnob(mouseX, mouseY)
                    ? CompanionLockerTextures.SCROLLBAR_HOVER
                    : CompanionLockerTextures.SCROLLBAR;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, texture);

        renderScrollbarKnob(poseStack, getKnobX(), knobY, getKnobW(), getKnobH());
    }

    void rebuildFilteredList() {
        List<ItemStack> companions = screen.menu().getCompanions();
        filteredIndices.clear();

        CompanionSearchBar searchBar = screen.searchBar();
        if (searchBar == null) {
            for (int i = 0; i < companions.size(); i++) {
                filteredIndices.add(i);
            }
        } else {
            filteredIndices.addAll(searchBar.filter(companions));
        }

        filteredIndices.sort((a, b) -> {
            boolean favouriteA = isFavouriteIndex(a);
            boolean favouriteB = isFavouriteIndex(b);
            if (favouriteA != favouriteB) {
                return favouriteA ? -1 : 1;
            }

            if (favouriteA) {
                long favouriteTimeA = getFavouriteTimeIndex(a);
                long favouriteTimeB = getFavouriteTimeIndex(b);
                if (favouriteTimeA != favouriteTimeB) {
                    return Long.compare(favouriteTimeB, favouriteTimeA);
                }
            }

            return Integer.compare(a, b);
        });

        if (screen.getSelectedIndex() >= 0 && !filteredIndices.contains(screen.getSelectedIndex())) {
            screen.setSelectedIndexLocal(-1);
        }

        if (screen.getSelectedIndex() == -1) {
            screen.setSelectedIndexLocal(filteredIndices.isEmpty() ? -1 : filteredIndices.get(0));
        }
    }

    void rebuildButtons() {
        removeButtons();

        scrollOffsetPx = Mth.clamp(scrollOffsetPx, 0, getMaxScrollPixels());

        for (int row = 0; row < filteredIndices.size(); row++) {
            int realIndex = filteredIndices.get(row);
            int buttonY = screen.listY() + row * BUTTON_STRIDE - scrollOffsetPx;

            CompanionDisplayButton button = new CompanionDisplayButton(
                    screen.listX(),
                    buttonY,
                    screen.listWidth(),
                    BUTTON_HEIGHT,
                    CompanionLockerTextures.DISPLAY_BUTTON,
                    CompanionLockerTextures.DISPLAY_BUTTON_HOVER,
                    CompanionLockerTextures.DISPLAY_BUTTON_SELECTED,
                    screen.menu(),
                    realIndex,
                    screen
            );

            companionButtons.add(button);
            screen.addControl(button);
            screen.addControl(button.quickEquipButton);
            screen.addControl(button.favouriteButton);
        }

        updateButtonPositions();
    }

    void refreshUi() {
        rebuildFilteredList();
        scrollOffsetPx = Mth.clamp(scrollOffsetPx, 0, getMaxScrollPixels());

        if (screen.getSelectedIndex() >= 0 && !filteredIndices.contains(screen.getSelectedIndex())) {
            screen.setSelectedIndexLocal(filteredIndices.isEmpty() ? -1 : filteredIndices.get(0));
        }

        rebuildButtons();
    }

    void selectDefaultFromFiltered() {
        if (filteredIndices.isEmpty()) {
            screen.setSelectedIndexLocal(-1);
            return;
        }

        int first = filteredIndices.get(0);
        if (screen.getSelectedIndex() != first) {
            screen.setSelectedIndexLocal(first);
        }
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean overList = mouseX >= screen.listX() && mouseX < screen.listX() + screen.listWidth()
                && mouseY >= screen.listY() && mouseY < screen.listY() + screen.listHeight();
        if (!overList) {
            return false;
        }

        if (getMaxScrollPixels() <= 0 || delta == 0) {
            return false;
        }

        int direction = (int) -Math.signum(delta);
        setScrollOffset(scrollOffsetPx + direction * MOUSE_WHEEL_SCROLL_STEP);
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMaxScrollPixels() > 0) {
            if (isMouseOverKnob(mouseX, mouseY)) {
                draggingScrollbar = true;
                scrollbarGrabOffsetY = (int) (mouseY - getKnobY());
                return true;
            }

            int trackX = getKnobX();
            int trackY = getBaseY();
            int trackH = getTrackLenPx() + getKnobH();
            boolean overTrack = mouseX >= trackX && mouseX < trackX + getKnobW()
                    && mouseY >= trackY && mouseY < trackY + trackH;
            if (overTrack) {
                scrollbarGrabOffsetY = getKnobH() / 2;
                setScrollFromMouse(mouseY);
                draggingScrollbar = true;
                return true;
            }
        }

        Slot hoveredSlot = screen.hoveredSlotValue();
        if (button == 0 && CompanionLockerScreen.hasShiftDown() && hoveredSlot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot) {
            if (relicSlot.isUnlocked() && relicSlot.hasItem() && !screen.hasEnoughVaultGoldForHoveredRelicRemoval()) {
                return true;
            }
        }
        if (button == 0 && CompanionLockerScreen.hasShiftDown() && hoveredSlot instanceof CompanionVendingMachineMenu.AncientRelicSlot ancientRelicSlot) {
            if (ancientRelicSlot.isUnlocked() && ancientRelicSlot.hasItem() && !screen.hasEnoughVaultGoldForHoveredRelicRemoval()) {
                return true;
            }
        }

        if (button == 1) {
            EditBox searchWidget = screen.searchBar() == null ? null : screen.searchBar().widget();
            if (searchWidget != null && searchWidget.isMouseOver(mouseX, mouseY)) {
                screen.searchBar().clear();
                rebuildFilteredList();
                rebuildButtons();
                return true;
            }
        }

        for (CompanionDisplayButton companionButton : companionButtons) {
            if (companionButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!draggingScrollbar || button != 0) {
            return false;
        }

        setScrollFromMouse(mouseY);
        return true;
    }

    boolean mouseReleased(int button) {
        if (button != 0 || !draggingScrollbar) {
            return false;
        }

        draggingScrollbar = false;
        return true;
    }

    void toggleFavourite(int realIndex) {
        List<ItemStack> companions = screen.menu().getCompanions();
        if (realIndex < 0 || realIndex >= companions.size()) {
            return;
        }

        ItemStack stack = companions.get(realIndex);
        if (stack.isEmpty()) {
            return;
        }

        UUID id = getId(stack);
        if (id == null) {
            return;
        }

        boolean newValue = !isFavouriteIndex(realIndex);
        favouriteCache.put(id, newValue);
        if (newValue) {
            favouriteTimeCache.put(id, System.currentTimeMillis());
        } else {
            favouriteTimeCache.remove(id);
        }

        ModNetworks.CHANNEL.sendToServer(new ToggleFavouriteC2SPacket(screen.menu().getBlockPos(), realIndex, newValue));
        rebuildFilteredList();
        rebuildButtons();
    }

    boolean isFavourite(int realIndex) {
        return isFavouriteIndex(realIndex);
    }

    private void removeButtons() {
        for (CompanionDisplayButton button : companionButtons) {
            screen.removeControl(button);
            screen.removeControl(button.favouriteButton);
            screen.removeControl(button.quickEquipButton);
        }
        companionButtons.clear();
    }

    boolean[] hideButtonsForSuperRender() {
        if (companionButtons.isEmpty()) {
            return null;
        }

        boolean[] previousVisibility = new boolean[companionButtons.size() * 3];
        int index = 0;
        for (CompanionDisplayButton button : companionButtons) {
            previousVisibility[index++] = button.visible;
            button.visible = false;

            previousVisibility[index++] = button.quickEquipButton.visible;
            button.quickEquipButton.visible = false;

            previousVisibility[index++] = button.favouriteButton.visible;
            button.favouriteButton.visible = false;
        }
        return previousVisibility;
    }

    void restoreButtonVisibility(boolean[] previousVisibility) {
        if (previousVisibility == null) {
            return;
        }

        int index = 0;
        for (CompanionDisplayButton button : companionButtons) {
            button.visible = previousVisibility[index++];
            button.quickEquipButton.visible = previousVisibility[index++];
            button.favouriteButton.visible = previousVisibility[index++];
        }
    }

    void renderClippedButtons(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        if (companionButtons.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0, 0, 150);
        enableListScissor();
        for (CompanionDisplayButton button : companionButtons) {
            if (!button.visible) {
                continue;
            }

            button.render(poseStack, mouseX, mouseY, delta);
            if (button.quickEquipButton.visible) {
                button.quickEquipButton.render(poseStack, mouseX, mouseY, delta);
            }
            if (button.favouriteButton.visible) {
                button.favouriteButton.render(poseStack, mouseX, mouseY, delta);
            }
        }
        disableScissor();
        poseStack.popPose();
    }

    private int getMaxScrollPixels() {
        return Math.max(0, getContentHeight() - screen.listHeight());
    }

    private int getContentHeight() {
        if (filteredIndices.isEmpty()) {
            return 0;
        }
        return filteredIndices.size() * BUTTON_HEIGHT + Math.max(0, filteredIndices.size() - 1) * BUTTON_SPACING;
    }

    private int getKnobX() {
        return screen.listX() - 18;
    }

    private int getBaseY() {
        return screen.listY() - 3;
    }

    private int getKnobW() {
        return 10;
    }

    private int getKnobH() {
        int contentHeight = getContentHeight();
        if (contentHeight <= 0) {
            return 32;
        }

        int knobHeight = (int) Math.round((screen.listHeight() * (double) screen.listHeight()) / (double) contentHeight);
        return Mth.clamp(knobHeight, MIN_SCROLLBAR_HEIGHT, screen.listHeight());
    }

    private int getTrackLenPx() {
        return (screen.listHeight() - getKnobH()) + 3;
    }

    private int getKnobY() {
        int maxScrollPixels = getMaxScrollPixels();
        if (maxScrollPixels <= 0) {
            return getBaseY();
        }

        float progress = (float) scrollOffsetPx / (float) maxScrollPixels;
        return getBaseY() + (int) (getTrackLenPx() * progress);
    }

    private boolean isMouseOverKnob(double mouseX, double mouseY) {
        int x = getKnobX();
        int y = getKnobY();
        return mouseX >= x && mouseX < x + getKnobW() && mouseY >= y && mouseY < y + getKnobH();
    }

    private void setScrollFromMouse(double mouseY) {
        int maxScrollPixels = getMaxScrollPixels();
        if (maxScrollPixels <= 0) {
            return;
        }

        double knobTop = mouseY - getBaseY() - scrollbarGrabOffsetY;
        double progress = Mth.clamp((float) (knobTop / (double) getTrackLenPx()), 0.0F, 1.0F);
        int newOffset = Mth.clamp((int) Math.round(progress * maxScrollPixels), 0, maxScrollPixels);
        setScrollOffset(newOffset);
    }

    private void setScrollOffset(int newOffset) {
        int clamped = Mth.clamp(newOffset, 0, getMaxScrollPixels());
        if (clamped == scrollOffsetPx) {
            return;
        }

        scrollOffsetPx = clamped;
        updateButtonPositions();
    }

    private void updateButtonPositions() {
        for (int row = 0; row < companionButtons.size(); row++) {
            CompanionDisplayButton button = companionButtons.get(row);
            int buttonY = screen.listY() + row * BUTTON_STRIDE - scrollOffsetPx;
            boolean intersectsViewport = buttonY + BUTTON_HEIGHT > screen.listY()
                    && buttonY < screen.listY() + screen.listHeight();

            button.x = screen.listX();
            button.y = buttonY;
            button.visible = intersectsViewport;
            button.active = intersectsViewport;

            if (button.quickEquipButton != null) {
                button.quickEquipButton.syncToParent();
            }
            if (button.favouriteButton != null) {
                button.favouriteButton.syncToParent();
            }
        }
    }

    private void enableListScissor() {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();

        int x = (int) Math.floor(screen.listX() * scale);
        int y = (int) Math.floor((screen.heightValue() - (screen.listY() + screen.listHeight())) * scale);
        int width = (int) Math.ceil(screen.listWidth() * scale);
        int height = (int) Math.ceil(screen.listHeight() * scale);

        if (width <= 0 || height <= 0) {
            RenderSystem.disableScissor();
            return;
        }

        RenderSystem.enableScissor(x, y, width, height);
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    private void renderScrollbarKnob(PoseStack poseStack, int x, int y, int width, int height) {
        int middleSrcH = SCROLLBAR_TEX_H - (SCROLLBAR_CAP_H * 2);
        int middleDestH = Math.max(0, height - (SCROLLBAR_CAP_H * 2));

        renderScrollbarSlice(poseStack, x, y, width, SCROLLBAR_CAP_H, 0, 0, SCROLLBAR_TEX_W, SCROLLBAR_CAP_H);

        if (middleDestH > 0) {
            renderScrollbarSlice(
                    poseStack,
                    x,
                    y + SCROLLBAR_CAP_H,
                    width,
                    middleDestH,
                    0,
                    SCROLLBAR_CAP_H,
                    SCROLLBAR_TEX_W,
                    middleSrcH
            );
        }

        renderScrollbarSlice(
                poseStack,
                x,
                y + height - SCROLLBAR_CAP_H,
                width,
                SCROLLBAR_CAP_H,
                0,
                SCROLLBAR_TEX_H - SCROLLBAR_CAP_H,
                SCROLLBAR_TEX_W,
                SCROLLBAR_CAP_H
        );
    }

    private void renderScrollbarSlice(PoseStack poseStack, int x, int y, int width, int height, int u, int v, int srcW, int srcH) {
        if (width <= 0 || height <= 0 || srcW <= 0 || srcH <= 0) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, 200);
        poseStack.scale((float) width / (float) srcW, (float) height / (float) srcH, 1.0F);
        GuiComponent.blit(poseStack, 0, 0, u, v, srcW, srcH, SCROLLBAR_TEX_W, SCROLLBAR_TEX_H);
        poseStack.popPose();
    }

    private boolean isFavouriteIndex(int realIndex) {
        List<ItemStack> companions = screen.menu().getCompanions();
        if (realIndex < 0 || realIndex >= companions.size()) {
            return false;
        }

        ItemStack stack = companions.get(realIndex);
        UUID id = getId(stack);
        if (id != null && favouriteCache.containsKey(id)) {
            return favouriteCache.get(id);
        }

        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(FAV_TAG);
    }

    private long getFavouriteTimeIndex(int realIndex) {
        List<ItemStack> companions = screen.menu().getCompanions();
        if (realIndex < 0 || realIndex >= companions.size()) {
            return 0L;
        }

        ItemStack stack = companions.get(realIndex);
        UUID id = getId(stack);
        if (id != null && favouriteTimeCache.containsKey(id)) {
            return favouriteTimeCache.get(id);
        }

        return stack.hasTag() && stack.getTag().contains(FAV_TIME_TAG) ? stack.getTag().getLong(FAV_TIME_TAG) : 0L;
    }

    @Nullable
    private UUID getId(ItemStack stack) {
        return stack.isEmpty() ? null : CompanionItem.getCompanionUUID(stack);
    }
}
