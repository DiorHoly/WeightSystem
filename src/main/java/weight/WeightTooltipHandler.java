package weight;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = WeightMod.MOD_ID, value = Dist.CLIENT)
public class WeightTooltipHandler {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        double singleWeight = ItemWeightManager.getWeight(stack.getItem());
        if (singleWeight <= 0) return;

        double totalWeight = singleWeight * stack.getCount();

        event.getToolTip().add(Component.literal("")); // 空行分隔
        event.getToolTip().add(Component.literal("§7重量: §e" + String.format("%.2f", singleWeight) + " kg"));
        if (stack.getCount() > 1) {
            event.getToolTip().add(Component.literal("§7组重: §a" + String.format("%.2f", totalWeight) + " kg"));
        }

        if (isBackpack(stack)) {
            float ratio = ItemWeightManager.getBackpackRatio(stack.getItem());
            double contentWeight = WeightCalculator.getBackpackContentWeight(stack);

            String ratioColor = ratio >= 0.8 ? "§a" : ratio >= 0.6 ? "§e" : "§c";
            event.getToolTip().add(Component.literal("§7负重计算比例: " + ratioColor + String.format("%.0f%%", ratio * 100)));

            if (contentWeight > 0) {
                event.getToolTip().add(Component.literal("§7内容物总重: §b" + String.format("%.2f", contentWeight) + " kg"));
            }
        }
    }

    private static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;

        boolean hasInventory = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();

        boolean isBackpackById = false;
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName != null) {
            isBackpackById = registryName.toString().toLowerCase().contains("backpack");
        }

        return hasInventory || isBackpackById;
    }
}