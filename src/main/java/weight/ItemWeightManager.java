package weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ItemWeightManager {
    private static final File CONFIG_DIR = new File(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().toFile(), "weight");
    private static final File CUSTOM_CONFIG = new File(CONFIG_DIR, "custom_items_weight.json");
    private static final File CUSTOM_RATIO_CONFIG = new File(CONFIG_DIR, "backpack_ratios.json");

    private static final double FALLBACK_WEIGHT = 0.1;
    private static final float FALLBACK_RATIO = 0.8f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType(); // 先解析为Object，再手动转换
    private static final Type RATIO_MAP_TYPE = new TypeToken<Map<String, Float>>() {}.getType();

    private static final Map<String, Double> ITEM_WEIGHTS = new HashMap<>();
    private static final Map<String, Double> DEFAULT_WEIGHTS = new HashMap<>();
    private static final Map<String, Float> BACKPACK_RATIOS = new HashMap<>();

    public static void loadAll() {
        WeightMod.LOGGER.info("========== 开始加载负重模组配置 ==========");

        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            WeightMod.LOGGER.error("无法创建配置目录：{}", CONFIG_DIR.getAbsolutePath());
        } else {
            WeightMod.LOGGER.info("配置目录：{}", CONFIG_DIR.getAbsolutePath());
        }

        loadBuiltinDefaults();
        loadBuiltinRatios();
        loadCustomConfig();
        loadCustomRatios();

        WeightMod.LOGGER.info("物品重量配置加载完成：{} 条（内置默认：{} 条）", ITEM_WEIGHTS.size(), DEFAULT_WEIGHTS.size());
        WeightMod.LOGGER.info("背包折算比例配置加载完成：{} 条", BACKPACK_RATIOS.size());

        if (!CUSTOM_CONFIG.exists()) {
            saveCustomWeights();
            WeightMod.LOGGER.info("已创建空的自定义物品重量配置文件");
        }
        if (!CUSTOM_RATIO_CONFIG.exists()) {
            saveCustomRatios();
            WeightMod.LOGGER.info("已创建空的自定义背包比例配置文件");
        }

        WeightMod.LOGGER.info("========== 负重模组配置加载完成 ==========");
    }

    public static double getWeight(Item item) {
        if (item == null || item == Items.AIR) return 0.0;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? FALLBACK_WEIGHT : ITEM_WEIGHTS.getOrDefault(key.toString(), FALLBACK_WEIGHT);
    }

    public static double getDefaultWeight(Item item) {
        if (item == null || item == Items.AIR) return 0.0;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? FALLBACK_WEIGHT : DEFAULT_WEIGHTS.getOrDefault(key.toString(), FALLBACK_WEIGHT);
    }

    public static float getBackpackRatio(Item backpackItem) {
        if (backpackItem == null || backpackItem == Items.AIR) return FALLBACK_RATIO;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(backpackItem);
        return key == null ? FALLBACK_RATIO : BACKPACK_RATIOS.getOrDefault(key.toString(), FALLBACK_RATIO);
    }

    public static void setBackpackRatio(String backpackId, float ratio) {
        BACKPACK_RATIOS.put(backpackId, ratio);
    }

    public static void setItemWeight(String itemId, double weight) {
        ITEM_WEIGHTS.put(itemId, weight);
    }

    public static void resetItemWeight(String itemId) {
        if (DEFAULT_WEIGHTS.containsKey(itemId)) {
            ITEM_WEIGHTS.put(itemId, DEFAULT_WEIGHTS.get(itemId));
        } else {
            ITEM_WEIGHTS.remove(itemId);
        }
    }

    public static void resetAllWeights() {
        ITEM_WEIGHTS.clear();
        ITEM_WEIGHTS.putAll(DEFAULT_WEIGHTS);
        WeightMod.LOGGER.info("已重置所有物品重量为内置默认值");
    }

    public static Map<String, Double> getAllWeights() {
        return new HashMap<>(ITEM_WEIGHTS);
    }

    public static void setAllWeights(Map<String, Double> weights) {
        ITEM_WEIGHTS.clear();
        ITEM_WEIGHTS.putAll(weights);
    }

    public static void saveCustomWeights() {
        try {
            if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
                WeightMod.LOGGER.error("无法创建配置目录：{}", CONFIG_DIR.getAbsolutePath());
                return;
            }
            try (FileWriter writer = new FileWriter(CUSTOM_CONFIG)) {
                GSON.toJson(ITEM_WEIGHTS, writer);
            }
            WeightMod.LOGGER.info("自定义配置已保存：{}", CUSTOM_CONFIG.getAbsolutePath());
        } catch (Exception e) {
            WeightMod.LOGGER.error("保存配置失败", e);
        }
    }

    public static void saveCustomRatios() {
        try {
            if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
                WeightMod.LOGGER.error("无法创建配置目录：{}", CONFIG_DIR.getAbsolutePath());
                return;
            }
            try (FileWriter writer = new FileWriter(CUSTOM_RATIO_CONFIG)) {
                GSON.toJson(BACKPACK_RATIOS, writer);
            }
            WeightMod.LOGGER.info("背包折算比例配置已保存：{}", CUSTOM_RATIO_CONFIG.getAbsolutePath());
        } catch (Exception e) {
            WeightMod.LOGGER.error("保存背包折算比例配置失败", e);
        }
    }

    private static void loadBuiltinDefaults() {
        String[] defaultFiles = {"minecraft.json", "backpacks.json"};
        for (String fileName : defaultFiles) {
            WeightMod.LOGGER.info("正在尝试加载内置配置文件：{}", fileName);

            String resourcePath = "assets/weight/defaults/" + fileName;
            InputStream inputStream = null;

            try {
                inputStream = WeightMod.class.getClassLoader().getResourceAsStream(resourcePath);
                if (inputStream != null) {
                    WeightMod.LOGGER.info("方式1成功（模组主类加载器）：{}", resourcePath);
                }
            } catch (Exception e) {
                WeightMod.LOGGER.warn("方式1失败：{}", e.getMessage());
            }

            if (inputStream == null) {
                try {
                    inputStream = ItemWeightManager.class.getResourceAsStream("/" + resourcePath);
                    if (inputStream != null) {
                        WeightMod.LOGGER.info("方式2成功（带/前缀）：{}", resourcePath);
                    }
                } catch (Exception e) {
                    WeightMod.LOGGER.warn("方式2失败：{}", e.getMessage());
                }
            }

            if (inputStream == null) {
                try {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                    if (inputStream != null) {
                        WeightMod.LOGGER.info("方式3成功（线程上下文）：{}", resourcePath);
                    }
                } catch (Exception e) {
                    WeightMod.LOGGER.warn("方式3失败：{}", e.getMessage());
                }
            }

            if (inputStream == null) {
                WeightMod.LOGGER.error("所有方式都无法加载内置配置文件：{}", resourcePath);
                continue;
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                Map<String, Object> rawData = GSON.fromJson(reader, MAP_TYPE);
                if (rawData != null && !rawData.isEmpty()) {
                    int count = 0;
                    for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                        String id = entry.getKey();
                        Object value = entry.getValue();

                        // 严格过滤：只保留有效ID + 数值类型的值
                        if (id != null
                                && !id.startsWith("=")
                                && !id.startsWith("==========")
                                && value instanceof Number) {
                            double weight = ((Number) value).doubleValue();
                            if (weight >= 0) {
                                ITEM_WEIGHTS.putIfAbsent(id, weight);
                                DEFAULT_WEIGHTS.putIfAbsent(id, weight);
                                count++;
                            }
                        }
                    }
                    WeightMod.LOGGER.info("成功加载内置配置文件：{}（{} 条有效数据）", fileName, count);
                } else {
                    WeightMod.LOGGER.error("内置配置文件解析结果为空：{}", fileName);
                }
            } catch (JsonSyntaxException e) {
                WeightMod.LOGGER.error("内置配置文件JSON格式错误：{}", fileName, e);
            } catch (Exception e) {
                WeightMod.LOGGER.error("加载内置配置失败：{}", fileName, e);
            }
        }
    }

    private static void loadBuiltinRatios() {
        String resourcePath = "assets/weight/defaults/backpack_ratios.json";
        WeightMod.LOGGER.info("正在尝试加载内置背包比例配置：{}", resourcePath);

        InputStream inputStream = null;
        try {
            inputStream = WeightMod.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                inputStream = ItemWeightManager.class.getResourceAsStream("/" + resourcePath);
            }
        } catch (Exception e) {
            WeightMod.LOGGER.warn("加载背包比例配置时出错：{}", e.getMessage());
        }

        if (inputStream == null) {
            WeightMod.LOGGER.warn("内置背包比例配置文件不存在：{}", resourcePath);
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map<String, Float> defaults = GSON.fromJson(reader, RATIO_MAP_TYPE);
            if (defaults != null) {
                int count = 0;
                for (Map.Entry<String, Float> entry : defaults.entrySet()) {
                    String id = entry.getKey();
                    Float ratio = entry.getValue();
                    if (id != null && !id.startsWith("=") && !id.startsWith("==========") && ratio != null && ratio >= 0 && ratio <= 1) {
                        BACKPACK_RATIOS.putIfAbsent(id, ratio);
                        count++;
                    }
                }
                WeightMod.LOGGER.info("成功加载内置背包比例配置：{}（{} 条）", resourcePath, count);
            }
        } catch (Exception e) {
            WeightMod.LOGGER.warn("加载内置背包折算比例配置失败", e);
        }
    }

    private static void loadCustomConfig() {
        if (!CUSTOM_CONFIG.exists()) {
            WeightMod.LOGGER.info("自定义配置文件不存在，跳过加载：{}", CUSTOM_CONFIG.getAbsolutePath());
            return;
        }
        try (FileReader reader = new FileReader(CUSTOM_CONFIG)) {
            Map<String, Double> custom = GSON.fromJson(reader, new TypeToken<Map<String, Double>>() {}.getType());
            if (custom != null) {
                ITEM_WEIGHTS.putAll(custom);
                WeightMod.LOGGER.info("加载用户自定义配置：{} 条", custom.size());
            }
        } catch (Exception e) {
            WeightMod.LOGGER.error("加载自定义配置失败", e);
        }
    }

    private static void loadCustomRatios() {
        if (!CUSTOM_RATIO_CONFIG.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CUSTOM_RATIO_CONFIG)) {
            Map<String, Float> custom = GSON.fromJson(reader, RATIO_MAP_TYPE);
            if (custom != null) {
                BACKPACK_RATIOS.putAll(custom);
                WeightMod.LOGGER.info("加载用户自定义背包折算比例：{} 条", custom.size());
            }
        } catch (Exception e) {
            WeightMod.LOGGER.error("加载自定义背包折算比例失败", e);
        }
    }
}

