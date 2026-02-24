package weight;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.settings.KeyConflictContext;


@Mod.EventBusSubscriber(modid = WeightMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class WeightKeyBindings {
    public static final KeyMapping OPEN_HUD_SETTINGS = new KeyMapping(
            "key.weight.open_hud_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "category.weight.general"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HUD_SETTINGS);
        WeightMod.LOGGER.info("已注册HUD设置按键：H");
    }

    @Mod.EventBusSubscriber(modid = WeightMod.MOD_ID, value = Dist.CLIENT)
    public static class KeyPressHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            if (OPEN_HUD_SETTINGS.consumeClick() && net.minecraft.client.Minecraft.getInstance().screen == null) {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new WeightHudConfigScreen(null)
                );
            }
        }
    }
}