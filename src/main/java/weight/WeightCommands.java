package weight;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.DecimalFormat;

@Mod.EventBusSubscriber(modid = WeightMod.MOD_ID)
public class WeightCommands {
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("weight")
                // ========== 修改玩家负重上限指令（权限等级4） ==========
                .then(Commands.literal("max")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("max_weight", DoubleArgumentType.doubleArg(0.0, 10000.0))
                                        .executes(ctx -> {
                                            Player targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                            double newMaxWeight = DoubleArgumentType.getDouble(ctx, "max_weight");
                                            AttributeInstance weightAttribute = targetPlayer.getAttribute(WeightAttributes.MAX_CARRY_WEIGHT.get());

                                            if (weightAttribute == null) {
                                                ctx.getSource().sendFailure(Component.literal("无法获取玩家 " + targetPlayer.getName().getString() + " 的负重属性"));
                                                return 0;
                                            }

                                            weightAttribute.setBaseValue(newMaxWeight);
                                            String successMsg = "已将玩家 " + targetPlayer.getName().getString() + " 的负重上限设置为 " + String.format("%.1f", newMaxWeight) + " kg";
                                            ctx.getSource().sendSuccess(() -> Component.literal(successMsg), true);
                                            targetPlayer.sendSystemMessage(Component.literal("你的负重上限已被管理员设置为 " + String.format("%.1f", newMaxWeight) + " kg"));
                                            WeightMod.LOGGER.info(successMsg);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            Player targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                            AttributeInstance weightAttribute = targetPlayer.getAttribute(WeightAttributes.MAX_CARRY_WEIGHT.get());

                                            if (weightAttribute == null) {
                                                ctx.getSource().sendFailure(Component.literal("无法获取玩家 " + targetPlayer.getName().getString() + " 的负重属性"));
                                                return 0;
                                            }

                                            double defaultWeight = WeightAttributes.MAX_CARRY_WEIGHT.get().getDefaultValue();
                                            weightAttribute.setBaseValue(defaultWeight);
                                            String successMsg = "已将玩家 " + targetPlayer.getName().getString() + " 的负重上限重置为默认值 " + String.format("%.1f", defaultWeight) + " kg";
                                            ctx.getSource().sendSuccess(() -> Component.literal(successMsg), true);
                                            targetPlayer.sendSystemMessage(Component.literal("你的负重上限已被管理员重置为默认值 " + String.format("%.1f", defaultWeight) + " kg"));
                                            WeightMod.LOGGER.info(successMsg);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                                        builder.suggest(id.toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("weight", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            ResourceLocation itemId = ResourceLocationArgument.getId(ctx, "item");
                                            double weight = DoubleArgumentType.getDouble(ctx, "weight");

                                            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                                                ctx.getSource().sendFailure(Component.literal("物品ID无效：" + itemId));
                                                return 0;
                                            }

                                            ItemWeightManager.setItemWeight(itemId.toString(), weight);
                                            ItemWeightManager.saveCustomWeights();
                                            WeightNetwork.syncToAllPlayers();

                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("已设置 " + itemId + " 重量为 " + weight), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("default")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                                        builder.suggest(id.toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ResourceLocation itemId = ResourceLocationArgument.getId(ctx, "item");
                                    ItemWeightManager.resetItemWeight(itemId.toString());
                                    ItemWeightManager.saveCustomWeights();
                                    WeightNetwork.syncToAllPlayers();

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("已重置 " + itemId + " 重量为默认值"), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("resetall")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ItemWeightManager.resetAllWeights();
                            ItemWeightManager.saveCustomWeights();
                            WeightNetwork.syncToAllPlayers();

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("已重置所有物品重量为模组内置默认值"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("ratio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("backpack", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                                                if (id.toString().contains("backpack") || id.toString().contains("bag")) {
                                                    builder.suggest(id.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("ratio", DoubleArgumentType.doubleArg(0, 1))
                                                .executes(ctx -> {
                                                    ResourceLocation backpackId = ResourceLocationArgument.getId(ctx, "backpack");
                                                    float ratio = (float) DoubleArgumentType.getDouble(ctx, "ratio");

                                                    if (!ForgeRegistries.ITEMS.containsKey(backpackId)) {
                                                        ctx.getSource().sendFailure(Component.literal("背包ID无效：" + backpackId));
                                                        return 0;
                                                    }

                                                    ItemWeightManager.setBackpackRatio(backpackId.toString(), ratio);
                                                    ItemWeightManager.saveCustomRatios();

                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("已设置 " + backpackId + " 折算比例为 " + ratio), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("backpack", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                                                if (id.toString().contains("backpack") || id.toString().contains("bag")) {
                                                    builder.suggest(id.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ResourceLocation backpackId = ResourceLocationArgument.getId(ctx, "backpack");

                                            if (!ForgeRegistries.ITEMS.containsKey(backpackId)) {
                                                ctx.getSource().sendFailure(Component.literal("背包ID无效：" + backpackId));
                                                return 0;
                                            }

                                            float ratio = ItemWeightManager.getBackpackRatio(ForgeRegistries.ITEMS.getValue(backpackId));
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal(backpackId + " 的折算比例为 " + ratio), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("speed")
                        .requires(source -> source.hasPermission(2))
                        // 添加/修改移速惩罚规则
                        .then(Commands.literal("add")
                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 1))
                                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.01, 10))
                                                .executes(ctx -> {
                                                    float percent = (float) DoubleArgumentType.getDouble(ctx, "percent");
                                                    float multiplier = (float) DoubleArgumentType.getDouble(ctx, "multiplier");

                                                    SpeedPenaltyConfig.INSTANCE.addOrUpdateRule(percent, multiplier);
                                                    String msg = String.format("已添加移速惩罚规则：重量百分比>%.0f%% 移速乘数×%.2f",
                                                            percent * 100, multiplier);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
                                                    WeightMod.LOGGER.info(msg);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // 删除移速惩罚规则
                        .then(Commands.literal("remove")
                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 1))
                                        .executes(ctx -> {
                                            float percent = (float) DoubleArgumentType.getDouble(ctx, "percent");
                                            SpeedPenaltyConfig.INSTANCE.removeRule(percent);
                                            String msg = String.format("已删除重量百分比>%.0f%% 的移速惩罚规则", percent * 100);
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
                                            WeightMod.LOGGER.info(msg);
                                            return 1;
                                        })
                                )
                        )
                        // 重置移速惩罚规则为默认
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    SpeedPenaltyConfig.INSTANCE.resetRules();
                                    String msg = "已重置所有移速惩罚规则为默认值";
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
                                    WeightMod.LOGGER.info(msg);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("=== 当前移速惩罚规则 ==="), false);
                                    for (SpeedPenaltyConfig.SpeedPenaltyRule rule : SpeedPenaltyConfig.INSTANCE.getPenaltyRules()) {
                                        String ruleMsg = String.format("重量>%.0f%% → 移速×%.2f",
                                                rule.getPercent() * 100, rule.getMultiplier());
                                        ctx.getSource().sendSuccess(() -> Component.literal(ruleMsg), false);
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}