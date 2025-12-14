package net.bustin.vending_companions.network.c2s;

import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import iskallia.vault.item.CompanionItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HealCompanionC2SPacket {
    private final BlockPos pos;
    private final int companionIndex;

    public HealCompanionC2SPacket(BlockPos pos, int companionIndex) {
        this.pos = pos;
        this.companionIndex = companionIndex;
    }

    public static void encode(HealCompanionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.companionIndex);
    }

    public static HealCompanionC2SPacket decode(FriendlyByteBuf buf) {
        return new HealCompanionC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(HealCompanionC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Make sure the player is actually interacting with this menu (prevents spoofing)
            if (!(player.containerMenu instanceof CompanionVendingMachineMenu menu)) return;
            if (!menu.getBlockPos().equals(msg.pos)) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof CompanionVendingMachineBlockEntity machine)) return;

            if (msg.companionIndex < 0 || msg.companionIndex >= machine.getCompanions().size()) return;

            ItemStack stack = machine.getCompanion(msg.companionIndex);
            if (stack.isEmpty() || !(stack.getItem() instanceof CompanionItem)) return;

            int hearts = CompanionItem.getCompanionHearts(stack);
            int max    = CompanionItem.getCompanionMaxHearts(stack);
            if (hearts >= max) return; // already full

            // consume 1 snack
            boolean consumed = machine.consumeOneSnack(
                    iskallia.vault.init.ModItems.COMPANION_HEAL
            );
            if (!consumed) return; // no snack → no heal

            //apply heal
            int newHearts = Math.min(max, hearts + 1);
            CompanionItem.setCompanionHearts(stack, newHearts);

            // Persist + sync
            machine.setChanged();
            player.level.sendBlockUpdated(msg.pos, machine.getBlockState(), machine.getBlockState(), 3);
            menu.broadcastChanges();
        });

        context.setPacketHandled(true);
    }

}

