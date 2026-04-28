package net.bustin.vending_companions.network.c2s;

import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.bustin.vending_companions.menu.CompanionVendingMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReleaseCompanionC2SPacket {
    private final BlockPos pos;
    private final int companionIndex;

    public ReleaseCompanionC2SPacket(BlockPos pos, int companionIndex) {
        this.pos = pos;
        this.companionIndex = companionIndex;
    }

    public static void encode(ReleaseCompanionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.companionIndex);
    }

    public static ReleaseCompanionC2SPacket decode(FriendlyByteBuf buf) {
        return new ReleaseCompanionC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ReleaseCompanionC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof CompanionVendingMachineMenu menu)) return;
            if (!menu.getBlockPos().equals(msg.pos)) return;

            BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof CompanionVendingMachineBlockEntity machine)) return;
            if (!machine.isOwner(player)) return;

            if (!machine.releaseCompanion(msg.companionIndex)) return;

            int companionCount = machine.getCompanions().size();
            if (companionCount <= 0) {
                menu.setSelectedIndexServer(-1);
            } else if (msg.companionIndex >= companionCount) {
                menu.setSelectedIndexServer(companionCount - 1);
            } else {
                menu.setSelectedIndexServer(msg.companionIndex);
            }
        });

        context.setPacketHandled(true);
    }
}
