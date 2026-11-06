package com.narutotoallbijuu.net;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.client.ClientHooks;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.logic.Transform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ModNet {
    private static final String PROTO = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AllBijuuMod.MODID, "main"), () -> PROTO, PROTO::equals, PROTO::equals);

    private ModNet() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }

    public static void sync(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPacket(JinchuurikiData.of(p).save()));
    }

    public static void sendAction(int action) {
        CHANNEL.sendToServer(new ActionPacket(action));
    }

    public record ActionPacket(int action) {
        public void encode(FriendlyByteBuf b) {
            b.writeVarInt(action);
        }

        public static ActionPacket decode(FriendlyByteBuf b) {
            return new ActionPacket(b.readVarInt());
        }

        public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> {
                ServerPlayer p = c.getSender();
                if (p != null) {
                    Transform.handleAction(p, m.action);
                }
            });
            c.setPacketHandled(true);
        }
    }

    public record SyncPacket(CompoundTag tag) {
        public void encode(FriendlyByteBuf b) {
            b.writeNbt(tag);
        }

        public static SyncPacket decode(FriendlyByteBuf b) {
            return new SyncPacket(b.readNbt());
        }

        public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.sync(m.tag)));
            c.setPacketHandled(true);
        }
    }
}
