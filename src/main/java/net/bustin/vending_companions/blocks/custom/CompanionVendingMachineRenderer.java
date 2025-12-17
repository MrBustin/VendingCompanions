package net.bustin.vending_companions.blocks.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

public class CompanionVendingMachineRenderer implements BlockEntityRenderer<CompanionVendingMachineBlockEntity> {

    public CompanionVendingMachineRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CompanionVendingMachineBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        Level level = be.getLevel();
        if (level == null) return;

        BlockState state = be.getBlockState();

        // only render from LOWER half so you don't double-draw
        if (state.hasProperty(CompanionVendingMachineBlock.HALF)
                && state.getValue(CompanionVendingMachineBlock.HALF) == DoubleBlockHalf.UPPER) {
            return;
        }

        Direction facing = state.hasProperty(CompanionVendingMachineBlock.FACING)
                ? state.getValue(CompanionVendingMachineBlock.FACING)
                : Direction.NORTH;

        // get first 3 companions safely
        List<ItemStack> comps = be.getCompanions();
        ItemStack s0 = ItemStack.EMPTY, s1 = ItemStack.EMPTY, s2 = ItemStack.EMPTY;

        if (comps != null && !comps.isEmpty()) {
            // build indices 0..n-1, sort like screen: favourites first, then original index
            List<Integer> idx = new java.util.ArrayList<>();
            for (int i = 0; i < comps.size(); i++) idx.add(i);

            idx.sort((a, b) -> {
                boolean fa = isFav(comps.get(a));
                boolean fb = isFav(comps.get(b));
                if (fa != fb) return fa ? -1 : 1;
                return Integer.compare(a, b); // preserve original order
            });

            if (idx.size() > 0) s0 = comps.get(idx.get(0));
            if (idx.size() > 1) s1 = comps.get(idx.get(1));
            if (idx.size() > 2) s2 = comps.get(idx.get(2));
        }

        BlockPos bePos = be.getBlockPos();

        poseStack.pushPose();

        // Rotate the whole "shelf space" around the block center.
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(getYRotDegrees(facing)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        // positions are in block-local coordinates (0..1 for x/z, y is in blocks)
        renderShelfItem(s0, poseStack, buffer, level, bePos, packedOverlay, 0.50f, 1.65f, 0.30f);
        renderShelfItem(s1, poseStack, buffer, level, bePos, packedOverlay, 0.50f, 1.30f, 0.30f);
        renderShelfItem(s2, poseStack, buffer, level, bePos, packedOverlay, 0.50f, 0.82f, 0.285f);

        poseStack.popPose();
    }

    private static float getYRotDegrees(Direction facing) {
        // Usual blockstate -> world rotation mapping
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST  -> 90f;
            case EAST  -> -90f;
            case NORTH -> 0f;
            default    -> 0f;
        };
    }

    private static void renderShelfItem(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, Level level, BlockPos bePos, int packedOverlay,
                                        float x, float y, float z) {
        if (stack == null || stack.isEmpty()) return;

        poseStack.pushPose();

        // move to shelf spot
        poseStack.translate(x, y, z);

        // Rotate the ITEM itself so it faces forward (optional, tweak to taste)
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180f));

        // scale (tweak)
        float scale = 0.45f;
        poseStack.scale(scale, scale, scale);

        // Use world light at the shelf position (better than the BE's packedLight)
        BlockPos lightPos = bePos.offset(
                (int) Math.floor((x - 0.5f) * 2f),
                (int) Math.floor(y),
                (int) Math.floor((z - 0.5f) * 2f)
        );
        int light = LevelRenderer.getLightColor(level, lightPos);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemTransforms.TransformType.FIXED,
                light,
                packedOverlay,
                poseStack,
                buffer,
                0
        );

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CompanionVendingMachineBlockEntity be) {
        return true;
    }

    private static boolean isFav(ItemStack s) {
        return s != null && !s.isEmpty() && s.hasTag() && s.getTag().getBoolean("vc_favourite");
    }
}