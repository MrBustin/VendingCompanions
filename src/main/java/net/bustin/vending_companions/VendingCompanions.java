package net.bustin.vending_companions;

import com.mojang.logging.LogUtils;
import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.entity.ModBlockEntites;
import net.bustin.vending_companions.items.ModItems;
import net.bustin.vending_companions.menu.ModMenuTypes;
import net.bustin.vending_companions.network.ModNetworks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(VendingCompanions.MOD_ID)
public class VendingCompanions {
    public static final String MOD_ID = "vending_companions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VendingCompanions() {
        // Mod event bus (lifecycle)
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::commonSetup);
        ModBlocks.register(eventBus);
        ModItems.register(eventBus);
        ModMenuTypes.register(eventBus);
        ModBlockEntites.register(eventBus);






        // Forge event bus (game events)
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetworks.onCommonSetup(event);
    }
}
