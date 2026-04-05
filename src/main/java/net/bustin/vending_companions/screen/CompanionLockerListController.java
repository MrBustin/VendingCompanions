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

    private final CompanionLockerScreen screen;
    private final List<CompanionDisplayButton> companionButtons = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();
    private final Map<UUID, Boolean> favouriteCache = new HashMap<>();
    private final Map<UUID, Long> favouriteTimeCache = new HashMap<>();

    private int scrollRowOffset = 0;
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
        scrollRowOffset = 0;
    }

    void renderScrollbar(PoseStack poseStack, int mouseX, int mouseY) {
        int maxScrollRows = getMaxScrollRows();
        int knobY = maxScrollRows > 0 ? getKnobY() : getBaseY();

        ResourceLocation texture;
        if (maxScrollRows <= 0) {
            texture = CompanionLockerTextures.SCROLLBAR_DISABLED;
        } else {
            texture = draggingScrollbar || isMouseOverKnob(mouseX, mouseY)
                    ? CompanionLockerTextures.SCROLLBAR_HOVER
                    : CompanionLockerTextures.SCROLLBAR;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, texture);

        float scaleX = (float) getKnobW() / 16.0F;
        float scaleY = (float) getKnobH() / 42.0F;

        poseStack.pushPose();
        poseStack.translate(getKnobX(), knobY, 200);
        poseStack.scale(scaleX, scaleY, 1.0F);
        GuiComponent.blit(poseStack, 0, 0, 0, 0, 16, 42, 16, 42);
        poseStack.popPose();
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

        int maxScrollRows = Math.max(0, filteredIndices.size() - CompanionLockerScreen.VISIBLE_ROWS);
        scrollRowOffset = Mth.clamp(scrollRowOffset, 0, maxScrollRows);

        for (int row = scrollRowOffset; row < Math.min(filteredIndices.size(), scrollRowOffset + CompanionLockerScreen.VISIBLE_ROWS); row++) {
            int realIndex = filteredIndices.get(row);
            int visualRow = row - scrollRowOffset;
            int buttonY = screen.listY() + visualRow * (BUTTON_HEIGHT + BUTTON_SPACING);

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
    }

    void refreshUi() {
        rebuildFilteredList();

        int maxScrollRows = Math.max(0, filteredIndices.size() - CompanionLockerScreen.VISIBLE_ROWS);
        scrollRowOffset = Mth.clamp(scrollRowOffset, 0, maxScrollRows);

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

        int maxScrollRows = getMaxScrollRows();
        if (maxScrollRows <= 0 || delta == 0) {
            return false;
        }

        int direction = (int) -Math.signum(delta);
        scrollRowOffset = Mth.clamp(scrollRowOffset + direction, 0, maxScrollRows);
        rebuildButtons();
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMaxScrollRows() > 0) {
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

    private int getMaxScrollRows() {
        return Math.max(0, filteredIndices.size() - CompanionLockerScreen.VISIBLE_ROWS);
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
        return 32;
    }

    private int getTrackLenPx() {
        return (screen.listHeight() - getKnobH()) + 3;
    }

    private int getKnobY() {
        int maxScrollRows = getMaxScrollRows();
        if (maxScrollRows <= 0) {
            return getBaseY();
        }

        float progress = (float) scrollRowOffset / (float) maxScrollRows;
        return getBaseY() + (int) (getTrackLenPx() * progress);
    }

    private boolean isMouseOverKnob(double mouseX, double mouseY) {
        int x = getKnobX();
        int y = getKnobY();
        return mouseX >= x && mouseX < x + getKnobW() && mouseY >= y && mouseY < y + getKnobH();
    }

    private void setScrollFromMouse(double mouseY) {
        int maxScrollRows = getMaxScrollRows();
        if (maxScrollRows <= 0) {
            return;
        }

        double knobTop = mouseY - getBaseY() - scrollbarGrabOffsetY;
        double progress = Mth.clamp((float) (knobTop / (double) getTrackLenPx()), 0.0F, 1.0F);
        int newOffset = Mth.clamp((int) Math.round(progress * maxScrollRows), 0, maxScrollRows);

        if (newOffset != scrollRowOffset) {
            scrollRowOffset = newOffset;
            rebuildButtons();
        }
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
