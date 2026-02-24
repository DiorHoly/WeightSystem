package weight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WeightMod.MOD_ID)
public class WeightMod {
    public static final String MOD_ID = "weight";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public WeightMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        WeightEnchantments.register(modEventBus);
        WeightAttributes.registerAttributes();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new WeightSpeedEvent());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(WeightHud.INSTANCE);
        }

        ItemWeightManager.loadAll();
        SpeedPenaltyConfig.INSTANCE.load();

        WeightNetwork.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            WeightHudConfig.load();
        }

        LOGGER.info("负重模组主类初始化完成");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("=== 负重模组通用初始化完成 ===");
        });
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            WeightNetwork.syncToPlayer(serverPlayer);
        }
    }
}