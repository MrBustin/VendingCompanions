package net.bustin.vending_companions.network.c2s;

import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectCompanionC2SPacket {

    private final BlockPos pos;
    private final int index;

    public SelectCompanionC2SPacket(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    // ✅ decoder ctor (this is what ::new uses)
    public SelectCompanionC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.index = buf.readInt();
    }

    // ✅ encoder (instance method)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.index);
    }

    // ✅ handler (instance method)
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof CompanionVendingMachineMenu menu)) return;

            // optional safety: must be same block
            if (!menu.getBlockPos().equals(this.pos)) return;

            menu.setSelectedIndexServer(this.index);
        });
        ctx.setPacketHandled(true);
    }
}

