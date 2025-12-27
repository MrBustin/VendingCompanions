package net.bustin.vending_companions.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.item.CompanionItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;


public class QuickEquipButton extends AbstractButton {

    private final CompanionDisplayButton parent;
    private final Runnable onEquip;

    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;

    private final ResourceLocation swapTex;
    private final ResourceLocation swapHoverTex;

    private final int relX;
    private final int relY;

    public Component getTooltip(){
        ItemStack equippedCompanion = getFirstCompanionFromCuriosClient();
        boolean willSwap = !equippedCompanion.isEmpty();
        if (willSwap) {
            return new TextComponent("Swap Companion");
        }else
            return new TextComponent("Equip Companion");
    }

    public QuickEquipButton(CompanionDisplayButton parent, int relX, int relY,
                            int w, int h, ResourceLocation normalTex, ResourceLocation hoverTex, ResourceLocation swapTex, ResourceLocation swapHoverTex, Runnable onEquip) {
        super(parent.x + relX, parent.y + relY, w, h, TextComponent.EMPTY);
        this.parent = parent;
        this.relX = relX;
        this.relY = relY;
        this.onEquip = onEquip;
        this.normalTex = normalTex;
        this.hoverTex = hoverTex;
        this.swapTex = swapTex;
        this.swapHoverTex = swapHoverTex;
    }

    public void syncToParent() {
        this.x = parent.x + relX;
        this.y = parent.y + relY;
        this.visible = parent.visible;
        this.active = parent.active;
    }

    @Override
    public void onPress() {
        onEquip.run();
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        ItemStack equippedCompanion = getFirstCompanionFromCuriosClient();
        boolean willSwap = !equippedCompanion.isEmpty();

        ResourceLocation tex;
        if (willSwap) {
            tex = this.isHoveredOrFocused() ? swapHoverTex : swapTex;
        } else {
            tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);
        RenderSystem.setShaderTexture(0, tex);

        blit(poseStack, this.x, this.y, 0, 0, 18, 18, 18, 18);
    }


    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(
                SimpleSoundInstance.forUI(
                        SoundEvents.ARMOR_EQUIP_DIAMOND,
                        1.0f,1.0f
                )
        );
    }

    private static ItemStack getFirstCompanionFromCuriosClient() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        final ItemStack[] found = new ItemStack[]{ ItemStack.EMPTY };

        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(curios -> {
            try {
                Object mapObj = curios.getClass().getMethod("getCurios").invoke(curios);
                if (!(mapObj instanceof Map<?, ?> map)) return;

                for (Object value : map.values()) {
                    if (!(value instanceof ICurioStacksHandler handler)) continue;

                    IDynamicStackHandler stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof CompanionItem) {
                            found[0] = stack;
                            return;
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {}
        });

        return found[0];
    }

    @Override
    public void updateNarration(NarrationElementOutput out) {}
}
