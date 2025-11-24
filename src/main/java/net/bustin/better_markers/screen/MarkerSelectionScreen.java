package net.bustin.better_markers.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bustin.better_markers.BetterMarkers;
import net.bustin.better_markers.util.MarkerIconType;
import net.bustin.better_markers.util.MarkerIconTypeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


public class MarkerSelectionScreen extends Screen {

    private final BlockPos markerPos;
    public MarkerSelectionScreen(BlockPos markerPos) {
        super(new TextComponent("Marker Selection Screen"));
        this.markerPos = markerPos;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 80;
        int buttonHeight = 20;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Close button
        int closeX = centerX - buttonWidth / 2;
        int closeY = centerY - buttonHeight / 2;

        this.addRenderableWidget(new Button(
                closeX, closeY,
                buttonWidth, buttonHeight,
                new TextComponent("Close"),
                new Button.OnPress() {
                    @Override
                    public void onPress(Button b) {
                        onClose();
                    }
                }
        ));

        // Test icon button, just below the close button
        int iconButtonY = closeY + buttonHeight + 5;

        this.addRenderableWidget(new Button(
                closeX, iconButtonY,
                buttonWidth, buttonHeight,
                new TextComponent("Set CHEST Icon"),
                new Button.OnPress() {
                    @Override
                    public void onPress(Button b) {
                        testSetChestIcon();
                        onClose();
                    }
                }
        ));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null); // closes the GUI
    }

    private void testSetChestIcon() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(markerPos);
        if (be == null) {
            BetterMarkers.LOGGER.info(
                    "[BetterMarkers][TEST] No BE at markerPos {} when trying to set CHEST icon",
                    markerPos
            );
            return;
        }

        if (be instanceof MarkerIconTypeHolder iconHolder) {
            iconHolder.bm$setIconType(MarkerIconType.CHEST);
            be.setChanged();

            BetterMarkers.LOGGER.info(
                    "[BetterMarkers][TEST] Set CHEST icon on marker BE at {}",
                    markerPos
            );
        } else {
            BetterMarkers.LOGGER.info(
                    "[BetterMarkers][TEST] BE at {} is not MarkerIconTypeHolder, it is {}",
                    markerPos, be.getClass().getName()
            );
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }
}