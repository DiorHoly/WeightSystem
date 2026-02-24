package weight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class WeightCalculator {

    public static double getCurrentWeight(Player player) {
        double totalWeight = 0.0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                totalWeight += calculateStackWeight(stack, 1.0f);

                if (isBackpack(stack)) {
                    float ratio = ItemWeightManager.getBackpackRatio(stack.getItem());
                    totalWeight += calculateBackpackContentWeight(stack, ratio);
                }
            }
        }

        double weightModifier = WeightEnchantments.calculateWeightModifier(player);
        totalWeight *= weightModifier;

        return totalWeight;
    }

    public static double getMaxWeight(Player player) {
        try {
            if (player.getAttribute(WeightAttributes.MAX_CARRY_WEIGHT.get()) != null) {
                return player.getAttributeValue(WeightAttributes.MAX_CARRY_WEIGHT.get());
            }
        } catch (Exception e) {
            WeightMod.LOGGER.warn("无法读取最大负重属性，使用默认值", e);
        }
        return 100.0;
    }

    private static double calculateStackWeight(ItemStack stack, float ratio) {
        if (stack.isEmpty()) return 0.0;
        double itemWeight = ItemWeightManager.getWeight(stack.getItem());
        return itemWeight * stack.getCount() * ratio;
    }

    private static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = registryName != null ? registryName.toString().toLowerCase() : "";
        String translationKey = stack.getItem().getDescriptionId().toLowerCase();
        boolean isBackpackByKey = itemId.contains("backpack") || itemId.contains("bag") ||
                translationKey.contains("backpack") || translationKey.contains("bag");

        boolean hasInventory = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();

        return isBackpackByKey || hasInventory;
    }

    public static double getBackpackContentWeight(ItemStack backpackStack) {
        if (!isBackpack(backpackStack)) return 0.0;
        float ratio = ItemWeightManager.getBackpackRatio(backpackStack.getItem());
        return calculateBackpackContentWeight(backpackStack, ratio);
    }

    public static double calculateBackpackContentWeight(ItemStack backpackStack, float ratio) {
        final double[] contentWeight = {0.0};

        backpackStack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack innerStack = handler.getStackInSlot(i);
                if (!innerStack.isEmpty()) {
                    contentWeight[0] += calculateStackWeight(innerStack, ratio);
                    if (isBackpack(innerStack)) {
                        float innerRatio = ItemWeightManager.getBackpackRatio(innerStack.getItem());
                        contentWeight[0] += calculateBackpackContentWeight(innerStack, innerRatio);
                    }
                }
            }
        });

        return contentWeight[0];
    }

    @SuppressWarnings("unused")
    public static boolean isOverweight(Player player) {
        return getCurrentWeight(player) > getMaxWeight(player);
    }

    @SuppressWarnings("unused")
    public static double getWeightPercentage(Player player) {
        double current = getCurrentWeight(player);
        double max = getMaxWeight(player);
        return max > 0 ? Math.min(current / max, 1.0) : 1.0;
    }
}
