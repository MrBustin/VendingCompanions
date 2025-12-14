package net.bustin.vending_companions.blocks;

import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlock;
import net.bustin.vending_companions.blocks.custom.CompanionVendingMachineBlockItem;
import net.bustin.vending_companions.blocks.custom.ExampleBlock;
import net.bustin.vending_companions.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;


public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, VendingCompanions.MOD_ID);


    public static final RegistryObject<Block> COMPANION_VENDING_MACHINE = registerBlock("companion_vending_machine",
            ()-> new CompanionVendingMachineBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(9f).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()), CreativeModeTab.TAB_MISC);


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, CreativeModeTab tab) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block,
                                                                            CreativeModeTab tab) {
        return ModItems.ITEMS.register(name, () -> {
            Item.Properties props = new Item.Properties().tab(tab);

            if ("companion_vending_machine".equals(name)) {
                return new CompanionVendingMachineBlockItem(block.get(), props);
            }

            return new BlockItem(block.get(), props);
        });
    }

    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
}
