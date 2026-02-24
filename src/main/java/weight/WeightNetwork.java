package weight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class WeightNetwork {
    private static final String PROTOCOL = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(WeightMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void init() {
        CHANNEL.registerMessage(
                0,
                WeightPacket.class,
                WeightPacket::encode,
                WeightPacket::decode,
                WeightPacket::handle
        );
        WeightMod.LOGGER.info("网络通道初始化完成：{}", WeightMod.MOD_ID);
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        WeightPacket packet = new WeightPacket(ItemWeightManager.getAllWeights());
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void syncToAllPlayers() {
        WeightPacket packet = new WeightPacket(ItemWeightManager.getAllWeights());
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        WeightMod.LOGGER.info("已向所有在线玩家同步物品重量配置");
    }

    public static void syncToServerPlayers(MinecraftServer server) {
        if (server == null) return;
        WeightPacket packet = new WeightPacket(ItemWeightManager.getAllWeights());
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        WeightMod.LOGGER.info("已向服务器所有玩家同步物品重量配置");
    }
}