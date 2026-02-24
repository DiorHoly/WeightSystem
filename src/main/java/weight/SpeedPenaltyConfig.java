package weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpeedPenaltyConfig {
    public static final SpeedPenaltyConfig INSTANCE = new SpeedPenaltyConfig();
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "weight/speed_penalty.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<SpeedPenaltyRule> penaltyRules = new ArrayList<>();
    private float defaultMultiplier = 1.0f;

    public void load() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    SpeedPenaltyConfigData data = GSON.fromJson(reader, SpeedPenaltyConfigData.class);
                    if (data != null) {
                        this.penaltyRules = data.speed_penalty_rules != null ? data.speed_penalty_rules : getDefaultRules();
                        this.defaultMultiplier = data.default_multiplier != 0 ? data.default_multiplier : 1.0f;
                        // 按percent降序排序，保证匹配优先级
                        this.penaltyRules.sort(Comparator.comparing(SpeedPenaltyRule::getPercent).reversed());
                    }
                }
                WeightMod.LOGGER.info("移速惩罚配置已加载：{} 条规则", penaltyRules.size());
            } else {
                this.penaltyRules = getDefaultRules();
                save();
                WeightMod.LOGGER.info("移速惩罚配置文件不存在，已创建默认配置");
            }
        } catch (Exception e) {
            WeightMod.LOGGER.error("加载移速惩罚配置失败，使用默认规则", e);
            this.penaltyRules = getDefaultRules();
        }
    }

    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            SpeedPenaltyConfigData data = new SpeedPenaltyConfigData();
            data.speed_penalty_rules = this.penaltyRules;
            data.default_multiplier = this.defaultMultiplier;

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
            WeightMod.LOGGER.info("移速惩罚配置已保存到：{}", CONFIG_FILE.getAbsolutePath());
        } catch (Exception e) {
            WeightMod.LOGGER.error("保存移速惩罚配置失败", e);
        }
    }

    public float getSpeedMultiplier(float weightPercent) {
        for (SpeedPenaltyRule rule : penaltyRules) {
            if (weightPercent > rule.getPercent()) {
                return rule.getMultiplier();
            }
        }
        return defaultMultiplier;
    }

    public void addOrUpdateRule(float percent, float multiplier) {
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        multiplier = Math.max(0.01f, multiplier);

        float finalPercent = percent;
        penaltyRules.removeIf(rule -> Float.compare(rule.getPercent(), finalPercent) == 0);
        penaltyRules.add(new SpeedPenaltyRule(percent, multiplier));
        penaltyRules.sort(Comparator.comparing(SpeedPenaltyRule::getPercent).reversed());
        save();
    }

    public void removeRule(float percent) {
        penaltyRules.removeIf(rule -> Float.compare(rule.getPercent(), percent) == 0);
        save();
    }

    public void resetRules() {
        this.penaltyRules = getDefaultRules();
        this.defaultMultiplier = 1.0f;
        save();
    }

    public List<SpeedPenaltyRule> getPenaltyRules() {
        return new ArrayList<>(penaltyRules);
    }

    private List<SpeedPenaltyRule> getDefaultRules() {
        List<SpeedPenaltyRule> defaultRules = new ArrayList<>();
        defaultRules.add(new SpeedPenaltyRule(1.0f, 0.2f));   // >100% 0.2倍
        defaultRules.add(new SpeedPenaltyRule(0.8f, 0.5f));   // >80% 0.5倍
        defaultRules.add(new SpeedPenaltyRule(0.5f, 0.8f));   // >50% 0.8倍
        defaultRules.add(new SpeedPenaltyRule(0.0f, 1.0f));   // 0-50% 1.0倍
        return defaultRules;
    }

    public static class SpeedPenaltyConfigData {
        public List<SpeedPenaltyRule> speed_penalty_rules;
        public float default_multiplier = 1.0f;
    }

    public static class SpeedPenaltyRule {
        private float percent;
        private float multiplier;

        public SpeedPenaltyRule() {}

        public SpeedPenaltyRule(float percent, float multiplier) {
            this.percent = percent;
            this.multiplier = multiplier;
        }

        public float getPercent() {
            return percent;
        }

        public float getMultiplier() {
            return multiplier;
        }

        public void setPercent(float percent) {
            this.percent = percent;
        }

        public void setMultiplier(float multiplier) {
            this.multiplier = multiplier;
        }
    }
}