package net.bustin.vending_companions.blocks.entity;

import net.bustin.vending_companions.VendingCompanions;
import net.bustin.vending_companions.blocks.ModBlocks;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModBlockEntites {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, VendingCompanions.MOD_ID);

    public static final RegistryObject<BlockEntityType<CompanionVendingMachineBlockEntity>> COMPANION_VENDING_MACHINE_BLOCK_ENTITY =
            BLOCK_ENTITES.register("companion_vending_machine_block_entity", () ->
                    BlockEntityType.Builder.of(
                            CompanionVendingMachineBlockEntity::new, ModBlocks.COMPANION_VENDING_MACHINE.get()).build(null));


    public static void register(IEventBus eventBus){
        BLOCK_ENTITES.register(eventBus);
    }
}
