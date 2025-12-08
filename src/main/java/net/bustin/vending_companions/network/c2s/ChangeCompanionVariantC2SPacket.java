package net.bustin.vending_companions.network.c2s;

import iskallia.vault.item.CompanionItem;
import net.bustin.vending_companions.blocks.entity.custom.CompanionVendingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ChangeCompanionVariantC2SPacket {
    private final BlockPos pos;
    private final int index;
    private final String variantType;

    public ChangeCompanionVariantC2SPacket(BlockPos pos, int index, String variantType) {
        this.pos = pos;
        this.index = index;
        this.variantType = variantType;
    }

    public ChangeCompanionVariantC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.index = buf.readVarInt();
        this.variantType = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(index);
        buf.writeUtf(variantType);
    }

    public static void handle(ChangeCompanionVariantC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.getLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof CompanionVendingMachineBlockEntity vending)) {
                return;
            }

            List<ItemStack> companions = vending.getCompanions(); // add getter if needed
            if (companions == null || msg.index < 0 || msg.index >= companions.size()) {
                return;
            }

            ItemStack stack = companions.get(msg.index);
            if (stack.isEmpty()) return;

            // actually apply the type on the server
            CompanionItem.setPetType(stack, msg.variantType);

            // if you also rename on server, do it here (optional)

            // mark BE dirty & sync
            vending.setChanged();
            // if your BE has a custom sync packet, send that here too

            // also force container to sync back to client
            player.containerMenu.broadcastChanges();
        });
        ctx.setPacketHandled(true);
    }
}

