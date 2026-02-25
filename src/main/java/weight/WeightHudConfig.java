package weight;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

@OnlyIn(Dist.CLIENT)
public class WeightHudConfig {
    private static final File CONFIG = new File(FMLPaths.CONFIGDIR.get().toFile(), "weight_hud.properties");
    private static final Properties PROPS = new Properties();

    public static void load() {
        try {
            if (CONFIG.exists()) {
                try (FileReader reader = new FileReader(CONFIG)) {
                    PROPS.load(reader);
                    WeightHud.INSTANCE.xRatio = Float.parseFloat(PROPS.getProperty("xRatio", "0.05"));
                    WeightHud.INSTANCE.yRatio = Float.parseFloat(PROPS.getProperty("yRatio", "0.05"));
                    WeightHud.INSTANCE.scale = Float.parseFloat(PROPS.getProperty("scale", "1.0"));
                    WeightHud.INSTANCE.alpha = Float.parseFloat(PROPS.getProperty("alpha", "1.0"));
                    WeightHud.INSTANCE.enabled = Boolean.parseBoolean(PROPS.getProperty("enabled", "true"));
                    WeightMod.LOGGER.info("HUD配置已加载：xRatio={}, yRatio={}", WeightHud.INSTANCE.xRatio, WeightHud.INSTANCE.yRatio);
                }
            } else {
                save();
                WeightMod.LOGGER.info("HUD配置文件不存在，已创建默认配置");
            }
        } catch (Exception e) {
            WeightMod.LOGGER.error("加载HUD配置失败", e);
        }
    }

    public static void save() {
        try {
            PROPS.setProperty("xRatio", String.valueOf(WeightHud.INSTANCE.xRatio));
            PROPS.setProperty("yRatio", String.valueOf(WeightHud.INSTANCE.yRatio));
            PROPS.setProperty("scale", String.valueOf(WeightHud.INSTANCE.scale));
            PROPS.setProperty("alpha", String.valueOf(WeightHud.INSTANCE.alpha));
            PROPS.setProperty("enabled", String.valueOf(WeightHud.INSTANCE.enabled));

            if (!CONFIG.getParentFile().exists()) {
                CONFIG.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(CONFIG)) {
                PROPS.store(writer, "Weight HUD Config (ratio 0.0-1.0)");
            }
            WeightMod.LOGGER.info("HUD配置已保存：xRatio={}, yRatio={}", WeightHud.INSTANCE.xRatio, WeightHud.INSTANCE.yRatio);
        } catch (Exception e) {
            WeightMod.LOGGER.error("保存HUD配置失败", e);
        }
    }
}
