package net.bustin.better_markers.networking.messages;

import net.bustin.better_markers.screen.MarkerSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenSelectionScreenMessage {

    private final BlockPos pos;

    // ctor used when sending the packet
    public OpenSelectionScreenMessage(BlockPos pos) {
        this.pos = pos;
    }

    // no-arg ctor no longer needed, but you can keep it private if mappings complain
    private OpenSelectionScreenMessage() {
        this.pos = BlockPos.ZERO;
    }

    public static OpenSelectionScreenMessage decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new OpenSelectionScreenMessage(pos);
    }

    public static void encode(OpenSelectionScreenMessage msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    // --- CLIENT-SIDE HANDLER ---
    public static void handle(OpenSelectionScreenMessage msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new MarkerSelectionScreen(msg.pos));
            });
        });
        ctx.setPacketHandled(true);
    }
}
