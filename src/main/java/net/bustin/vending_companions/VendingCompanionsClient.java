package net.bustin.vending_companions;

import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineRenderer;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.menu.ModMenuTypes;
import net.bustin.vending_companions.screen.CompanionVendingMachineScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VendingCompanions.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class VendingCompanionsClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(
                    ModMenuTypes.COMPANION_VENDING_MACHINE_MENU.get(),
                    CompanionVendingMachineScreen::new
            );

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.COMPANION_VENDING_MACHINE.get(),
                    RenderType.translucent()
            );

            BlockEntityRenderers.register(
                    ModBlockEntites.COMPANION_VENDING_MACHINE_BLOCK_ENTITY.get(),
                    CompanionVendingMachineRenderer::new
            );
        });
    }
}
