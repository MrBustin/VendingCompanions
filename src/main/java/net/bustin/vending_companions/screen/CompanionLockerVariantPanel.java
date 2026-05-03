package net.bustin.vending_companions.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.framework.ScreenTextures;
import iskallia.vault.init.ModSounds;
import iskallia.vault.entity.entity.pet.PetHelper;
import iskallia.vault.entity.entity.pet.PetModelType;
import iskallia.vault.item.CompanionItem;
import iskallia.vault.item.CompanionSeries;
import net.bustin.vending_companions.network.ModNetworks;
import net.bustin.vending_companions.network.c2s.ChangeCompanionVariantC2SPacket;
import net.bustin.vending_companions.network.c2s.ReleaseCompanionC2SPacket;
import net.bustin.vending_companions.screen.buttons.ReleaseCompanionButton;
import net.bustin.vending_companions.screen.buttons.VariantItemButton;
import net.bustin.vending_companions.screen.buttons.VariantTextButton;
import net.bustin.vending_companions.screen.buttons.VariantToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CompanionLockerVariantPanel {

    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;

    private final CompanionLockerScreen screen;
    private final List<Button> variantButtons = new ArrayList<>();
    private final List<Button> releaseButtons = new ArrayList<>();
    private static final Component RELEASE_CONFIRM_LABEL = new TextComponent("Releasing will destroy all relics and trails stored inside.");

    private VariantToggleButton changeModelButton;
    private ReleaseCompanionButton releaseCompanionButton;
    private VariantTextButton confirmReleaseButton;
    private VariantTextButton cancelReleaseButton;
    private boolean variantsOpen = false;
    private float variantsAnim = 0.0f;
    private boolean releaseConfirmOpen = false;

    CompanionLockerVariantPanel(CompanionLockerScreen screen) {
        this.screen = screen;
    }

    List<Button> buttons() {
        return variantButtons;
    }

    VariantToggleButton changeModelButton() {
        return changeModelButton;
    }

    ReleaseCompanionButton releaseCompanionButton() {
        return releaseCompanionButton;
    }

    float animationProgress() {
        return variantsAnim;
    }

    void clearTrackedButtons() {
        variantButtons.clear();
        releaseButtons.clear();
        changeModelButton = null;
        releaseCompanionButton = null;
        confirmReleaseButton = null;
        cancelReleaseButton = null;
        releaseConfirmOpen = false;
    }

    void initControls() {
        changeModelButton = new VariantToggleButton(
                screen.detailsX() + screen.detailsWidth() - 9,
                screen.detailsY(),
                10,
                10,
                CompanionLockerTextures.CYCLE,
                CompanionLockerTextures.CYCLE_HOVER,
                new TextComponent("Change Model"),
                button -> toggleMenu()
        );
        releaseCompanionButton = new ReleaseCompanionButton(
                screen.detailsX() + screen.detailsWidth() - 15,
                screen.detailsY() + screen.detailsHeight() - 15,
                15, 15,
                CompanionLockerTextures.RELEASE_BUTTON,
                CompanionLockerTextures.RELEASE_BUTTON_HOVER,
                new TextComponent("Release Companion"),
                button -> toggleReleaseConfirmation()
        );
        confirmReleaseButton = new VariantTextButton(
                releaseCompanionButton.x,
                releaseCompanionButton.y,
                90,
                20,
                new TextComponent("Confirm").withStyle(ChatFormatting.RED),
                new TextComponent("Release Companion"),
                ignored -> confirmRelease()
        ) {
            @Override
            public void playDownSound(SoundManager soundManager) {
                soundManager.play(
                        SimpleSoundInstance.forUI(
                                ModSounds.BYE_BYE,
                                1.0f,
                                1.0f
                        )
                );
            }
        };
        cancelReleaseButton = new VariantTextButton(
                releaseCompanionButton.x,
                releaseCompanionButton.y,
                90,
                20,
                new TextComponent("Cancel"),
                new TextComponent("Cancel Release"),
                ignored -> closeReleaseConfirmation()
        );
        confirmReleaseButton.visible = false;
        confirmReleaseButton.active = false;
        cancelReleaseButton.visible = false;
        cancelReleaseButton.active = false;
        releaseButtons.add(confirmReleaseButton);
        releaseButtons.add(cancelReleaseButton);
        screen.addControl(changeModelButton);
        screen.addControl(releaseCompanionButton);
        screen.addControl(confirmReleaseButton);
        screen.addControl(cancelReleaseButton);
    }
    void tickAnimation() {
        float speed = 0.05f;
        if (variantsOpen) {
            if (variantsAnim < 1.0f) {
                variantsAnim = Math.min(1.0f, variantsAnim + speed);
            }
        } else if (variantsAnim > 0.0f) {
            variantsAnim = Math.max(0.0f, variantsAnim - speed);
        }
    }

    void rebuildButtons() {
        removeButtons();
        closeReleaseConfirmation();

        List<ItemStack> companions = screen.menu().getCompanions();
        if (companions.isEmpty()) {
            return;
        }

        if (screen.getSelectedIndex() < 0 || screen.getSelectedIndex() >= companions.size()) {
            screen.setSelectedIndexLocal(0);
        }

        ItemStack baseStack = screen.menu().getCompanion(screen.getSelectedIndex());
        if (baseStack.isEmpty()) {
            return;
        }

        CompanionSeries series = CompanionItem.getPetSeries(baseStack);
        String currentType = CompanionItem.getPetType(baseStack);
        if (currentType == null || currentType.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int baseX = screen.detailsX() + screen.detailsWidth() + 2;
        int baseY = screen.detailsY() + 5;

        if (series == CompanionSeries.PET) {
            PetModelType modelType = PetHelper.getModel(currentType).orElse(null);
            if (modelType == null) {
                return;
            }

            List<PetHelper.PetVariant> variants = modelType.getVariants(minecraft.player.getUUID());
            for (int i = 0; i < variants.size(); i++) {
                PetHelper.PetVariant variant = variants.get(i);
                String type = variant.type();
                if (type == null || type.isEmpty()) {
                    continue;
                }

                ItemStack icon = baseStack.copy();
                CompanionItem.setPetType(icon, type);

                Component tooltip = new TextComponent(variant.displayName());
                Button button;
                if (hasRenderableAssets(minecraft, variant)) {
                    button = new VariantItemButton(
                            baseX,
                            baseY + i * 20,
                            20,
                            20,
                            icon,
                            tooltip,
                            ignored -> changeVariant(type)
                    );
                } else {
                    button = new VariantTextButton(
                            baseX,
                            baseY + i * 20,
                            20,
                            20,
                            new TextComponent(variant.displayName().substring(0, 1).toUpperCase()),
                            tooltip,
                            ignored -> changeVariant(type)
                    );
                }
                variantButtons.add(button);
                screen.addControl(button);
            }
        } else if (series == CompanionSeries.LEGEND) {
            String[] types = {"eternal", "giant", "minion", "antlion"};
            for (int i = 0; i < types.length; i++) {
                String type = types[i];
                Component tooltip = new TextComponent(type.substring(0, 1).toUpperCase() + type.substring(1));

                Button button = new VariantTextButton(
                        baseX,
                        baseY + i * 20,
                        20,
                        20,
                        new TextComponent(type.substring(0, 1).toUpperCase()),
                        tooltip,
                        ignored -> changeVariant(type)
                );
                variantButtons.add(button);
                screen.addControl(button);
            }
        }

        updateButtonPositions();
    }

    void updateButtonPositions() {
        updateVariantButtonPositions();
        updateReleaseButtonPositions();
    }

    boolean[] hideButtonsForSuperRender() {
        List<Button> layeredButtons = getLayeredButtons();
        if (layeredButtons.isEmpty()) {
            return null;
        }

        boolean[] previousVisibility = new boolean[layeredButtons.size()];
        for (int i = 0; i < layeredButtons.size(); i++) {
            Button button = layeredButtons.get(i);
            previousVisibility[i] = button.visible;
            button.visible = false;
        }
        return previousVisibility;
    }

    void restoreButtonVisibility(boolean[] previousVisibility) {
        if (previousVisibility == null) {
            return;
        }

        List<Button> layeredButtons = getLayeredButtons();
        for (int i = 0; i < previousVisibility.length; i++) {
            layeredButtons.get(i).visible = previousVisibility[i];
        }
    }

    void renderOverlay(PoseStack poseStack) {
        if (variantsAnim > 0.02f && !variantButtons.isEmpty()) {
            int overlayX = screen.detailsX() + screen.detailsWidth() - 22;
            int overlayY = screen.detailsY() + 15;
            int textureU = 175 + (screen.detailsWidth() - 22);
            int textureV = 4 + 15;

            poseStack.pushPose();
            poseStack.translate(0, 0, 200);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, CompanionLockerTextures.BACKGROUND);
            GuiComponent.blit(poseStack, overlayX, overlayY, textureU, textureV, 27, 78, TEX_WIDTH, TEX_HEIGHT);
            poseStack.popPose();
        }

        renderReleaseConfirmationOverlay(poseStack);
    }

    void renderClippedButtons(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        if (variantsAnim > 0.02f && !variantButtons.isEmpty()) {
            int seamX = screen.detailsX() + screen.detailsWidth() + 6;

            poseStack.pushPose();
            poseStack.translate(0, 0, 150);
            enableVerticalOnlyScissor(seamX);
            for (Button button : variantButtons) {
                if (button.visible) {
                    button.render(poseStack, mouseX, mouseY, delta);
                }
            }
            disableScissor();
            poseStack.popPose();
        }

        if (releaseConfirmOpen) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 210);
            for (Button button : releaseButtons) {
                if (button.visible) {
                    button.render(poseStack, mouseX, mouseY, delta);
                }
            }
            poseStack.popPose();
        }
    }

    private void toggleMenu() {
        if (variantsOpen) {
            closeVariantMenu();
            return;
        }

        closeReleaseConfirmation();
        variantsOpen = true;
    }

    private void changeVariant(String variantType) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            List<ItemStack> companions = screen.menu().getCompanions();
            if (companions.isEmpty()) {
                return;
            }

            if (screen.getSelectedIndex() < 0 || screen.getSelectedIndex() >= companions.size()) {
                screen.setSelectedIndexLocal(0);
            }

            ItemStack stack = screen.menu().getCompanion(screen.getSelectedIndex());
            if (stack.isEmpty()) {
                return;
            }

            CompanionSeries series = CompanionItem.getPetSeries(stack);
            String currentType = CompanionItem.getPetType(stack);
            if (variantType == null || variantType.isEmpty() || (currentType != null && variantType.equalsIgnoreCase(currentType))) {
                return;
            }

            if (series == CompanionSeries.PET) {
                PetHelper.PetVariant newVariant = PetHelper.getVariant(variantType).orElse(null);
                if (newVariant == null) {
                    return;
                }

                if (currentType != null) {
                    PetHelper.PetVariant previousVariant = PetHelper.getVariant(currentType).orElse(null);
                    if (previousVariant != null) {
                        String currentName = CompanionItem.getPetName(stack);
                        String previousDisplay = previousVariant.displayName();
                        if (currentName == null || currentName.equalsIgnoreCase(previousDisplay)) {
                            CompanionItem.setPetName(stack, newVariant.displayName());
                        }
                    }
                }
            }

            CompanionItem.setPetType(stack, variantType);

            UUID uuid = CompanionItem.getCompanionUUID(stack);
            if (uuid != null) {
                screen.invalidatePreviewEntity(uuid);
            }

            ModNetworks.CHANNEL.sendToServer(
                    new ChangeCompanionVariantC2SPacket(screen.menu().getBlockPos(), screen.getSelectedIndex(), variantType)
            );
        } catch (Exception exception) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        new TextComponent("Error changing model: " + exception.getClass().getSimpleName()),
                        true
                );
            }
        }
    }

    private void toggleReleaseConfirmation() {
        if (screen.getSelectedIndex() < 0) {
            closeReleaseConfirmation();
            return;
        }

        if (releaseConfirmOpen) {
            closeReleaseConfirmation();
            return;
        }

        closeVariantMenu();
        releaseConfirmOpen = true;
    }

    private void closeReleaseConfirmation() {
        releaseConfirmOpen = false;
        for (Button button : releaseButtons) {
            button.active = false;
        }
    }

    private void closeVariantMenu() {
        variantsOpen = false;
        variantsAnim = 0.0f;
        for (Button button : variantButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    private void confirmRelease() {
        closeReleaseConfirmation();
        if (screen.getSelectedIndex() < 0) {
            return;
        }

        ModNetworks.CHANNEL.sendToServer(
                new ReleaseCompanionC2SPacket(screen.menu().getBlockPos(), screen.getSelectedIndex())
        );
    }

    private void removeButtons() {
        for (Button button : variantButtons) {
            screen.removeControl(button);
        }
        variantButtons.clear();
    }

    private void updateVariantButtonPositions() {
        if (variantButtons.isEmpty()) {
            return;
        }

        int baseX = screen.detailsX() + screen.detailsWidth() + 6;
        int baseY = screen.detailsY() + 15;
        int slideDistance = 30;
        float perButtonDelay = 0.1f;

        for (int i = 0; i < variantButtons.size(); i++) {
            Button button = variantButtons.get(i);
            float delay = i * perButtonDelay;
            float progress = variantsAnim <= delay ? 0.0f : (variantsAnim - delay) / (1.0f - delay);
            progress = Mth.clamp(progress, 0.0f, 1.0f);

            button.x = baseX - (int) ((1.0f - progress) * slideDistance);
            button.y = baseY + i * 22;
            button.visible = variantsAnim > 0.02f;
            button.active = variantsOpen;
        }
    }

    private void updateReleaseButtonPositions() {
        if (releaseCompanionButton == null) {
            return;
        }

        releaseCompanionButton.x = screen.detailsX() + screen.detailsWidth() - 15;
        releaseCompanionButton.y = screen.detailsY() + screen.detailsHeight() - 15;

        if (releaseButtons.isEmpty()) {
            return;
        }

        int confirmButtonX = screen.detailsX() + screen.detailsWidth() + 10;
        int confirmButtonY = releaseCompanionButton.y -28;
        int cancelButtonX = screen.detailsX() + screen.detailsWidth() + 10;
        int cancelButtonY = releaseCompanionButton.y - 5;

        confirmReleaseButton.x = confirmButtonX;
        confirmReleaseButton.y = confirmButtonY;
        confirmReleaseButton.visible = releaseConfirmOpen;
        confirmReleaseButton.active = releaseConfirmOpen;

        cancelReleaseButton.x = cancelButtonX;
        cancelReleaseButton.y = cancelButtonY;
        cancelReleaseButton.visible = releaseConfirmOpen;
        cancelReleaseButton.active = releaseConfirmOpen;
    }

    private void renderReleaseConfirmationOverlay(PoseStack poseStack) {
        if (!releaseConfirmOpen || confirmReleaseButton == null || cancelReleaseButton == null) {
            return;
        }

        int popupTextPaddingX = 4;
        int popupTextPaddingTop = 4;
        int popupX = screen.detailsX() + screen.detailsWidth() + 5;
        int popupY = releaseCompanionButton.y - 72;
        int popupWidth = 100;
        int popupHeight = 90;
        int popupTextWidth = popupWidth - (popupTextPaddingX * 2);
        List<FormattedCharSequence> wrappedLabel = screen.fontRenderer().split(RELEASE_CONFIRM_LABEL, popupTextWidth);
        int labelHeight = wrappedLabel.size() * screen.fontRenderer().lineHeight;
        int renderedPopupHeight = Math.max(popupHeight, popupTextPaddingTop + labelHeight + 6);

        poseStack.pushPose();
        poseStack.translate(0, 0, 205);
        GuiComponent.fill(poseStack, popupX, popupY, popupX + popupWidth, popupY + renderedPopupHeight, 0xBE6A2A2A);
        GuiComponent.fill(poseStack, popupX + 1, popupY + 1, popupX + popupWidth - 1, popupY + renderedPopupHeight - 1, 0x96180808);
        int textY = popupY + popupTextPaddingTop;
        for (FormattedCharSequence line : wrappedLabel) {
            screen.fontRenderer().draw(poseStack, line, popupX + popupTextPaddingX, textY, 0xFFFFFF);
            textY += screen.fontRenderer().lineHeight;
        }
        poseStack.popPose();
    }

    private List<Button> getLayeredButtons() {
        List<Button> buttons = new ArrayList<>(variantButtons.size() + releaseButtons.size());
        buttons.addAll(variantButtons);
        buttons.addAll(releaseButtons);
        return buttons;
    }

    private boolean hasRenderableAssets(Minecraft minecraft, PetHelper.PetVariant variant) {
        PetHelper.PetRenderData renderData = variant.renderData();
        return hasResource(minecraft, renderData.modelLocation())
                && hasResource(minecraft, renderData.textureLocation())
                && hasResource(minecraft, renderData.animationLocation());
    }

    private boolean hasResource(Minecraft minecraft, ResourceLocation location) {
        return minecraft.getResourceManager().hasResource(location);
    }

    private void enableVerticalOnlyScissor(int seamGuiX) {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double scale = window.getGuiScale();

        int x = (int) Math.floor(seamGuiX * scale);
        int w = (int) Math.ceil((screen.widthValue() - seamGuiX) * scale);
        if (w <= 0) {
            RenderSystem.disableScissor();
            return;
        }

        RenderSystem.enableScissor(x, 0, w, window.getHeight());
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }
}
