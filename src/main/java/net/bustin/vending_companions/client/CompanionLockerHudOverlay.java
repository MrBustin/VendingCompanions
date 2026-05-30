package net.bustin.vending_companions.client;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlock;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = VendingCompanions.MOD_ID, value = Dist.CLIENT)
public class CompanionLockerHudOverlay {

    private static final TextComponent STORE_HINT = new TextComponent("Shift-right click to store companion");

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !hasEquippedCompanion(player)) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos lockerPos = getLockerPos(minecraft, blockHitResult);
        if (lockerPos == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(lockerPos);
        if (!(blockEntity instanceof CompanionVendingMachineBlockEntity locker) || !locker.isOwner(player)) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = (width - minecraft.font.width(STORE_HINT)) / 2;
        int y = height / 2 + 14;

        minecraft.font.drawShadow(event.getMatrixStack(), STORE_HINT, x, y, 0xFFFFFF);
    }

    private static boolean hasEquippedCompanion(LocalPlayer player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack -> !stack.isEmpty() && stack.getItem() instanceof CompanionItem)
                .isPresent();
    }

    private static BlockPos getLockerPos(Minecraft minecraft, BlockHitResult blockHitResult) {
        BlockPos hitPos = blockHitResult.getBlockPos();
        BlockState hitState = minecraft.level.getBlockState(hitPos);
        if (!(hitState.getBlock() instanceof CompanionVendingMachineBlock)) {
            return null;
        }

        if (hitState.hasProperty(CompanionVendingMachineBlock.HALF)
                && hitState.getValue(CompanionVendingMachineBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = hitPos.below();
            BlockState lowerState = minecraft.level.getBlockState(lowerPos);
            if (!(lowerState.getBlock() instanceof CompanionVendingMachineBlock)) {
                return null;
            }
            return lowerPos;
        }

        return hitPos;
    }
}
