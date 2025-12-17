package net.bustin.vending_companions.util;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.bustin.vending_companions.screen.CompanionVendingMachineScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class ModJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("vending_companions", "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGuiContainerHandler(
                CompanionVendingMachineScreen.class,
                new IGuiContainerHandler<CompanionVendingMachineScreen>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(CompanionVendingMachineScreen screen) {
                        // cover the whole screen => JEI hides overlay (including bookmarks)
                        return List.of(new Rect2i(0, 0, screen.width, screen.height));
                    }
                }
        );
    }
}

