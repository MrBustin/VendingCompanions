package net.bustin.vending_companions.network.c2s;

import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuickEquipCompanionC2SPacket {

    private final BlockPos pos;
    private final int index;

    public QuickEquipCompanionC2SPacket(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public QuickEquipCompanionC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.index = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level;
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            if (!serverLevel.isLoaded(pos)) {
                return;
            }

            if (serverLevel.getBlockEntity(pos) instanceof CompanionVendingMachineBlockEntity be) {
                be.equipCompanion(player, index,true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
