package weight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent; // 修复：使用正确的游戏模式变更事件类
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component; // 可选：导入组件类（提示消息用）
import java.util.UUID;

// 修复：确保类被Forge注册为事件监听器（需配合@Mod.EventBusSubscriber，或手动注册）
public class WeightSpeedEvent {
    // 固定UUID，用于唯一标识速度惩罚修饰符
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    
    // 主Tick事件处理：每刻检测玩家负重并更新速度惩罚
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingTickEvent event) {
        // 双层校验：确保是服务端玩家，避免客户端执行逻辑
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        // 创造/旁观模式直接移除惩罚
        if (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR) {
            removeSpeedPenalty(player);
            return;
        }
        // 生存/冒险模式计算并应用速度惩罚
        updateWeightSpeedPenalty(player);
    }
    
    // 监听游戏模式变更事件：模式切换时立即更新惩罚
    @SubscribeEvent
    public void onGameModeChange(PlayerChangeGameModeEvent event) {
        // 修复：原代码未校验服务端，补充客户端过滤
        ServerPlayer player = event.getEntity() instanceof ServerPlayer ? (ServerPlayer) event.getEntity() : null;
        if (player == null || player.level().isClientSide()) return;
        
        GameType newMode = event.getNewGameMode();
        if (newMode == GameType.CREATIVE || newMode == GameType.SPECTATOR) {
            removeSpeedPenalty(player);
        } else {
            updateWeightSpeedPenalty(player);
        }
        // 可选：发送模式变更提示（需导入Component）
        player.sendSystemMessage(Component.literal("游戏模式已切换，速度惩罚已更新"));
    }
    
    // 核心方法：根据负重计算并应用速度惩罚
    private void updateWeightSpeedPenalty(ServerPlayer player) {
        // 空值校验：防止WeightCalculator静态方法返回异常
        double current = WeightCalculator.getCurrentWeight(player);
        double max = WeightCalculator.getMaxWeight(player);
        
        // 防止除零错误+负重为0时移除惩罚
        if (max <= 0 || current <= 0) {
            removeSpeedPenalty(player);
            return;
        }
        
        float weightPercent = (float) (current / max);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        
        // 空值校验：防止属性实例获取失败
        if (speedAttr == null) return;
        
        // 先移除旧修饰符，避免叠加
        speedAttr.removeModifier(SPEED_MODIFIER_ID);
        
        // 从配置获取对应负重比例的速度乘数
        float speedMultiplier = SpeedPenaltyConfig.INSTANCE.getSpeedMultiplier(weightPercent);
        
        // 仅当乘数≠1时添加惩罚（无惩罚时不修改属性）
        if (speedMultiplier != 1.0f) {
            speedAttr.addPermanentModifier(new AttributeModifier(
                SPEED_MODIFIER_ID,
                "Weight speed penalty",
                speedMultiplier - 1.0, // MULTIPLY_TOTAL：最终值=基础值*(1+修饰值)
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
    
    // 移除速度惩罚：清理对应UUID的修饰符
    private void removeSpeedPenalty(ServerPlayer player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }
    
    // 玩家登录时初始化速度惩罚
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        if (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR) {
            removeSpeedPenalty(player);
        } else {
            updateWeightSpeedPenalty(player);
        }
    }
    
    // 玩家重生时重新计算速度惩罚
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        if (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR) {
            removeSpeedPenalty(player);
        } else {
            updateWeightSpeedPenalty(player);
        }
    }
}
