package net.bustin.vending_companions.screen;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import iskallia.vault.client.gui.framework.render.Tooltips;
import iskallia.vault.client.gui.helper.UIHelper;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.entity.entity.PetEntity;
import iskallia.vault.entity.entity.pet.PetHelper;
import iskallia.vault.init.ModEntities;
import iskallia.vault.item.CompanionItem;
import iskallia.vault.item.CompanionParticleTrailItem;
import iskallia.vault.item.CompanionPetManager;
import iskallia.vault.item.CompanionSeries;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class CompanionLockerDetailsRenderer {

    private static final Cache<UUID, LivingEntity> CACHED_COMPANIONS = CacheBuilder.newBuilder()
            .maximumSize(10L)
            .expireAfterAccess(2L, TimeUnit.MINUTES)
            .build();

    private static final int TEX_WIDTH = 370;
    private static final int TEX_HEIGHT = 300;
    private static final int XP_BAR_WIDTH = 64;
    private static final int XP_BAR_HEIGHT = 8;

    private final CompanionLockerScreen screen;
    private int companionRenderSize = 30;

    CompanionLockerDetailsRenderer(CompanionLockerScreen screen) {
        this.screen = screen;
    }

    void invalidatePreview(UUID uuid) {
        CACHED_COMPANIONS.invalidate(uuid);
    }

    void renderBackground(PoseStack poseStack, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CompanionLockerTextures.BACKGROUND);

        int x = (screen.widthValue() - screen.imageWidthValue()) / 2;
        int y = (screen.heightValue() - screen.imageHeightValue()) / 2;
        GuiComponent.blit(poseStack, x - 65, y - 10, 0, 0, screen.imageWidthValue(), screen.imageHeightValue(), TEX_WIDTH, TEX_HEIGHT);

        int boxX1 = screen.detailsX() + screen.previewOffX();
        int boxY1 = screen.detailsY() + screen.previewOffY();
        int boxX2 = boxX1 + screen.previewWidth();
        int boxY2 = boxY1 + screen.previewHeight();
        GuiComponent.fill(poseStack, boxX1, boxY1, boxX2, boxY2, 0xFF000000);

        renderRelicSlotBackgrounds(poseStack);
        renderTrailSlotBackgrounds(poseStack);
    }

    void renderCompanionDetails(PoseStack poseStack, int mouseX, int mouseY) {
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

        int panelX = screen.detailsX();
        int panelY = screen.detailsY();

        renderCompanionName(poseStack, stack, panelX, panelY);
        renderCompanionHearts(poseStack, stack, panelX, panelY);
        renderCompanionXpBar(poseStack, stack, panelX, panelY, mouseX, mouseY);
        renderCompanionStats(poseStack, stack, panelX, panelY);
        renderTemporalModifier(poseStack, stack, panelX, panelY, mouseX, mouseY);
    }

    void renderCompanionPreviewEntity(PoseStack poseStack, int mouseX, int mouseY) {
        if (screen.menu().getCompanions().isEmpty()) {
            return;
        }

        if (screen.getSelectedIndex() < 0 || screen.getSelectedIndex() >= screen.menu().getCompanions().size()) {
            screen.setSelectedIndexLocal(0);
        }

        ItemStack stack = screen.menu().getCompanion(screen.getSelectedIndex());
        if (stack.isEmpty()) {
            return;
        }

        LivingEntity entity = getOrCreateCompanionEntity(stack);
        if (entity == null) {
            return;
        }

        int centerX = screen.detailsX() + screen.previewOffX() + screen.previewWidth() / 2;
        int centerY = screen.detailsY() + screen.previewOffY() + screen.previewHeight() - 18;

        renderEntityLikeInventory(
                centerX,
                centerY,
                companionRenderSize,
                (float) centerX - mouseX,
                (float) centerY - mouseY,
                entity
        );
    }

    private void renderRelicSlotBackgrounds(PoseStack poseStack) {
        for (int i = 0; i < CompanionVendingMachineMenu.RELIC_SLOT_COUNT; i++) {
            int slotX = screen.leftPosValue() + screen.relicSlotOffX();
            int slotY = screen.topPosValue() + screen.relicSlotOffY() + i * 18;

            Slot slot = screen.menu().slots.get(i);
            boolean unlocked = !(slot instanceof CompanionVendingMachineMenu.RelicSlot relicSlot) || relicSlot.isUnlocked();
            boolean filled = unlocked && slot.hasItem();

            ResourceLocation texture;
            if (!unlocked) {
                texture = CompanionLockerTextures.RELIC_SLOT_LOCKED;
            } else if (filled) {
                texture = CompanionLockerTextures.RELIC_SLOT_FILLED;
            } else {
                texture = CompanionLockerTextures.RELIC_SLOT_UNLOCKED;
            }

            RenderSystem.setShaderTexture(0, texture);
            GuiComponent.blit(poseStack, slotX, slotY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void renderTrailSlotBackgrounds(PoseStack poseStack) {
        int firstTrailIndex = CompanionVendingMachineMenu.RELIC_SLOT_COUNT;
        for (int i = 0; i < CompanionVendingMachineMenu.TRAIL_SLOT_COUNT; i++) {
            int slotX = screen.leftPosValue() + screen.trailSlotOffX();
            int slotY = screen.topPosValue() + screen.trailSlotOffY() + i * 18;

            Slot slot = screen.menu().slots.get(firstTrailIndex + i);
            boolean unlocked = !(slot instanceof CompanionVendingMachineMenu.TrailSlot trailSlot) || trailSlot.isUnlocked();

            RenderSystem.setShaderTexture(0, unlocked ? CompanionLockerTextures.TRAIL_SLOT_UNLOCKED : CompanionLockerTextures.TRAIL_SLOT_LOCKED);
            GuiComponent.blit(poseStack, slotX - 1, slotY - 1, 0, 0, 18, 18, 18, 18);
        }
    }

    private void renderCompanionName(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        String name = CompanionItem.getPetName(stack);
        if (name == null || name.isEmpty()) {
            name = "Companion";
        }
        screen.fontRenderer().draw(poseStack, name, panelX + screen.nameOffX(), panelY + screen.nameOffY(), 0x404040);
    }

    private void renderCompanionHearts(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int hearts = CompanionItem.getCompanionHearts(stack);
        int maxHearts = CompanionItem.getCompanionMaxHearts(stack);

        RenderSystem.setShaderTexture(0, CompanionLockerTextures.GUI_ICONS);
        poseStack.pushPose();
        poseStack.translate(0, 0, 10);

        int x = panelX + screen.heartsOffX();
        int y = panelY + screen.heartsOffY();

        for (int i = 0; i < maxHearts; ++i) {
            GuiComponent.blit(poseStack, x + i * 8, y, 16, 0, 9, 9, 256, 256);
            if (i < hearts) {
                GuiComponent.blit(poseStack, x + i * 8, y, 52, 0, 9, 9, 256, 256);
            }
        }

        poseStack.popPose();
    }

    private void renderTemporalModifier(PoseStack poseStack, ItemStack stack, int panelX, int panelY, int mouseX, int mouseY) {
        Optional<ResourceLocation> temporalOpt = CompanionItem.getTemporalModifier(stack);
        if (temporalOpt.isEmpty()) {
            return;
        }

        ResourceLocation temporalId = temporalOpt.get();
        Optional<VaultModifier<?>> modifierOpt = VaultModifierRegistry.getOpt(temporalId);
        if (modifierOpt.isEmpty()) {
            return;
        }

        VaultModifier<?> modifier = modifierOpt.get();
        ResourceLocation texture = CompanionLockerTextures.temporalModifier(temporalId.getPath());

        int iconX = panelX + screen.temporalIconOffX();
        int iconY = panelY + screen.temporalIconOffY();

        poseStack.pushPose();
        poseStack.translate(0, 0, 200);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(poseStack, iconX, iconY, 0, 0, 32, 32, 32, 32);
        poseStack.popPose();

        if (mouseX >= iconX && mouseX <= iconX + 32 && mouseY >= iconY && mouseY <= iconY + 32) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(new TextComponent(modifier.getDisplayName()).withStyle(Style.EMPTY.withColor(modifier.getDisplayTextColor())));
            tooltip.add(new TextComponent("Duration: " + UIHelper.formatTimeString(CompanionItem.getTemporalDuration(stack))).withStyle(ChatFormatting.WHITE));
            screen.showComponentTooltip(poseStack, tooltip, mouseX, mouseY);
        }
    }

    private void renderCompanionXpBar(PoseStack poseStack, ItemStack stack, int panelX, int panelY, int mouseX, int mouseY) {
        int level = CompanionItem.getCompanionLevel(stack);
        int xp = CompanionItem.getCompanionXP(stack);
        int xpReq = Math.max(1, CompanionItem.getXPRequiredForNextLevel(stack));
        float progress = (float) xp / (float) xpReq;

        int barX = panelX + screen.xpOffX();
        int barY = panelY + screen.xpOffY();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CompanionLockerTextures.XP_BAR);
        GuiComponent.blit(poseStack, barX, barY, 0, 0, XP_BAR_WIDTH, XP_BAR_HEIGHT, XP_BAR_WIDTH, XP_BAR_HEIGHT);

        int filledWidth = (int) (progress * XP_BAR_WIDTH);
        if (filledWidth > 0) {
            RenderSystem.setShaderTexture(0, CompanionLockerTextures.XP_BAR_FILL);
            GuiComponent.blit(poseStack, barX, barY, 0, 0, filledWidth, XP_BAR_HEIGHT, XP_BAR_WIDTH, XP_BAR_HEIGHT);
        }

        String levelStr = String.valueOf(level);
        int textX = (barX + (XP_BAR_WIDTH - screen.fontRenderer().width(levelStr)) / 2) - 1;
        int textY = barY - 2;

        poseStack.pushPose();
        poseStack.translate(textX, textY, 0);
        poseStack.scale(1.5f, 1.5f, 1.0f);
        screen.fontRenderer().draw(poseStack, levelStr, -1, 0, 0xFF000000);
        screen.fontRenderer().draw(poseStack, levelStr, 1, 0, 0xFF000000);
        screen.fontRenderer().draw(poseStack, levelStr, 0, -1, 0xFF000000);
        screen.fontRenderer().draw(poseStack, levelStr, 0, 1, 0xFF000000);
        screen.fontRenderer().draw(poseStack, levelStr, 0, 0, 0xFFF0B100);
        poseStack.popPose();

        if (mouseX >= barX && mouseX <= barX + XP_BAR_WIDTH && mouseY >= barY && mouseY <= barY + XP_BAR_HEIGHT) {
            if (Screen.hasShiftDown()) {
                screen.showComponentTooltip(poseStack, List.of(
                        new TextComponent("Companion Experience"),
                        new TextComponent("Experience: " + xp + "/" + xpReq).withStyle(ChatFormatting.GRAY),
                        new TextComponent("Level: " + level).withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
            } else {
                screen.showComponentTooltip(poseStack, List.of(
                        new TextComponent("Companion Experience"),
                        Tooltips.DEFAULT_HOLD_SHIFT_COMPONENT
                ), mouseX, mouseY);
            }
        }
    }

    private void renderCompanionStats(PoseStack poseStack, ItemStack stack, int panelX, int panelY) {
        int sx = panelX + screen.statsOffX();
        int sy = panelY + screen.statsOffY();

        long days = 0;
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(CompanionItem.getHatchedDate(stack));
            days = java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDate.now());
        } catch (Exception ignored) {
        }

        int hearts = CompanionItem.getCompanionHearts(stack);
        int cooldown = CompanionItem.getCurrentCooldown(stack);

        String status;
        if (hearts <= 0) {
            status = "Retired";
        } else if (cooldown <= 0) {
            status = "Ready";
        } else {
            status = UIHelper.formatTimeString((long) cooldown * 20L);
        }

        screen.fontRenderer().draw(poseStack, CompanionItem.getVaultRuns(stack) + " vaults", sx, sy, 0x404040);
        screen.fontRenderer().draw(poseStack, days + " days", sx, sy + 12, 0x404040);
        screen.fontRenderer().draw(poseStack, status, sx, sy + 24, 0x404040);
    }

    @Nullable
    private LivingEntity getOrCreateCompanionEntity(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        UUID uuid = CompanionItem.getCompanionUUID(stack);
        if (uuid == null) {
            return null;
        }

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
        PetEntity pet = ModEntities.PET.create(level);
        if (pet == null) {
            return null;
        }

        pet.setCompanionData(stack);

        if (series == CompanionSeries.PET) {
            PetHelper.getVariant(type).ifPresent(variant -> {
                if (variant.traits() != null) {
                    variant.traits().apply(pet);
                }
            });
        }

        CompoundTag spawnData = CompanionItem.getSpawnData(stack);
        if (spawnData != null) {
            pet.setVanillaEntityData(spawnData);
        }

        List<Integer> colours = CompanionItem.getAllCosmeticColours(stack);
        List<CompanionParticleTrailItem.TrailType> trails = CompanionItem.getAllCosmeticTrailTypes(stack);
        List<Integer> validColours = new ArrayList<>();
        List<CompanionParticleTrailItem.TrailType> validTrails = new ArrayList<>();
        for (int i = 0; i < colours.size(); i++) {
            if (i < trails.size() && colours.get(i) != -1) {
                validColours.add(colours.get(i));
                validTrails.add(trails.get(i));
            }
        }

        pet.setParticleColours(validColours);
        pet.setParticleTrailTypes(validTrails);
        CompanionPetManager.applySkinsToEntity(pet, stack);
        pet.setNoAi(true);
        pet.setInvulnerable(true);
        pet.setSilent(true);

        CACHED_COMPANIONS.put(uuid, pet);
        return pet;
    }

    private static void renderEntityLikeInventory(int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity) {
        float rotX = (float) Math.atan(mouseX / 40.0F);
        float rotY = (float) Math.atan(mouseY / 40.0F);

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x, y, 1050.0D);
        modelViewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack modelStack = new PoseStack();
        modelStack.translate(0.0D, 0.0D, 1000.0D);
        modelStack.scale(scale, scale, scale);

        Quaternion zRot = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion xRot = Vector3f.XP.rotationDegrees(rotY * 20.0F);
        zRot.mul(xRot);
        modelStack.mulPose(zRot);

        float bodyRotY = entity.yBodyRot;
        float yRot = entity.getYRot();
        float xRotOld = entity.getXRot();
        float headRotY0 = entity.yHeadRotO;
        float headRotY = entity.yHeadRot;

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
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, modelStack, buffer, 15728880));
        buffer.endBatch();

        dispatcher.setRenderShadow(true);

        entity.yBodyRot = bodyRotY;
        entity.setYRot(yRot);
        entity.setXRot(xRotOld);
        entity.yHeadRotO = headRotY0;
        entity.yHeadRot = headRotY;

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
