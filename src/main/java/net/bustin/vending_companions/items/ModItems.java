package net.bustin.vending_companions.items;

import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VendingCompanions.MOD_ID);

//    public static final RegistryObject<Item> COMPANION_VENDING_MACHINE_ITEM =
//            ITEMS.register("companion_vending_machine",
//                    () -> new CompanionVendingMachineBlockItem(
//                            ModBlocks.COMPANION_VENDING_MACHINE.get(),
//                            new Item.Properties().tab(CreativeModeTab.TAB_MISC)
//                    ));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
