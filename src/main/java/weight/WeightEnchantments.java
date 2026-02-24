package weight;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WeightEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(
            ForgeRegistries.ENCHANTMENTS, WeightMod.MOD_ID
    );

    public static final RegistryObject<Enchantment> WEIGHT_LIFTING = ENCHANTMENTS.register(
            "weight_lifting",
            () -> new Enchantment(
                    Enchantment.Rarity.UNCOMMON,
                    EnchantmentCategory.ARMOR_CHEST,
                    new EquipmentSlot[]{EquipmentSlot.CHEST}
            ) {
                @Override
                public int getMaxLevel() {
                    return 5;
                }

                @Override
                public int getMinCost(int level) {
                    return 10 + level * 5;
                }

                @Override
                public int getMaxCost(int level) {
                    return getMinCost(level) + 30;
                }
            }
    );

    public static final RegistryObject<Enchantment> POWERLESS = ENCHANTMENTS.register(
            "powerless", // 关键修改：ID从weakness改为powerless
            () -> new Enchantment(
                    Enchantment.Rarity.RARE,
                    EnchantmentCategory.ARMOR_CHEST,
                    new EquipmentSlot[]{EquipmentSlot.CHEST}
            ) {
                @Override
                public int getMaxLevel() {
                    return 3;
                }

                @Override
                public int getMinCost(int level) {
                    return 15 + level * 10;
                }

                @Override
                public int getMaxCost(int level) {
                    return getMinCost(level) + 40;
                }

                @Override
                public boolean isCurse() {
                    return true;
                }
            }
    );

    public static final RegistryObject<Enchantment> OVERWHELMING_STRENGTH = ENCHANTMENTS.register(
            "overwhelming_strength",
            () -> new Enchantment(
                    Enchantment.Rarity.VERY_RARE,
                    EnchantmentCategory.ARMOR_CHEST,
                    new EquipmentSlot[]{EquipmentSlot.CHEST}
            ) {
                @Override
                public int getMaxLevel() {
                    return 1;
                }

                @Override
                public int getMinCost(int level) {
                    return 50;
                }

                @Override
                public int getMaxCost(int level) {
                    return getMinCost(level) + 50;
                }

                @Override
                public boolean isTreasureOnly() {
                    return true;
                }
            }
    );

    public static double calculateWeightModifier(LivingEntity entity) {
        double modifier = 1.0;

        if (entity.getItemBySlot(EquipmentSlot.CHEST).getEnchantmentLevel(OVERWHELMING_STRENGTH.get()) > 0) {
            return 0.0;
        }

        int weightLiftingLevel = entity.getItemBySlot(EquipmentSlot.CHEST).getEnchantmentLevel(WEIGHT_LIFTING.get());
        if (weightLiftingLevel > 0) {
            modifier -= weightLiftingLevel * 0.1;
        }

        int powerlessLevel = entity.getItemBySlot(EquipmentSlot.CHEST).getEnchantmentLevel(POWERLESS.get());
        if (powerlessLevel > 0) {
            modifier += powerlessLevel * 0.2;
        }

        return Math.max(0.1, Math.min(3.0, modifier));
    }

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
        WeightMod.LOGGER.info("负重附魔已注册：举重、无力、力能抗鼎");
    }
}
