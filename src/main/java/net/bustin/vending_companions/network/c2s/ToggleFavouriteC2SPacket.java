package net.bustin.vending_companions.network.c2s;

import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ToggleFavouriteC2SPacket {
    private final BlockPos pos;
    private final int index;
    private final boolean fav;

    public ToggleFavouriteC2SPacket(BlockPos pos, int index, boolean fav) {
        this.pos = pos;
        this.index = index;
        this.fav = fav;
    }

    public static void encode(ToggleFavouriteC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.index);
        buf.writeBoolean(msg.fav);
    }

    public static ToggleFavouriteC2SPacket decode(FriendlyByteBuf buf) {
        return new ToggleFavouriteC2SPacket(buf.readBlockPos(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(ToggleFavouriteC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Level level = player.level;
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof CompanionVendingMachineBlockEntity machine)) return;

            List<ItemStack> comps = machine.getCompanions();
            if (msg.index < 0 || msg.index >= comps.size()) return;

            ItemStack stack = comps.get(msg.index);
            if (stack.isEmpty()) return;

            // IMPORTANT: write into the actual stored stack
            stack.getOrCreateTag().putBoolean("vc_favourite", msg.fav);

            machine.setChanged();
            level.sendBlockUpdated(msg.pos, machine.getBlockState(), machine.getBlockState(), 3);
        });

        context.setPacketHandled(true);
    }
}
