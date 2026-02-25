package weight;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class WeightPacket {
    private final Map<String, Double> weightMap;
    private static final Gson GSON = new Gson();
    private static final TypeToken<Map<String, Double>> MAP_TYPE = new TypeToken<>() {};

    public WeightPacket(Map<String, Double> weightMap) {
        this.weightMap = new HashMap<>(weightMap);
    }

    public void encode(FriendlyByteBuf buf) {
        String json = GSON.toJson(weightMap);
        buf.writeUtf(json);
    }

    public static WeightPacket decode(FriendlyByteBuf buf) {
        String json = buf.readUtf();
        Map<String, Double> map = GSON.fromJson(json, MAP_TYPE);
        return new WeightPacket(map);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                ItemWeightManager.setAllWeights(this.weightMap);
                WeightMod.LOGGER.info("客户端同步物品重量配置：{} 条", this.weightMap.size());
            }
        });
        context.setPacketHandled(true);
    }

    public Map<String, Double> getWeightMap() {
        return new HashMap<>(weightMap);
    }
}
