package weight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class WeightSpeedEvent {
    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return; // 仅服务端计算

        double current = WeightCalculator.getCurrentWeight(player);
        double max = WeightCalculator.getMaxWeight(player);
        float percent = (float) (current / max);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }

        // 从配置动态获取移速乘数
        float multiplier = SpeedPenaltyConfig.INSTANCE.getSpeedMultiplier(percent);

        if (speedAttr != null && multiplier != 1.0) {
            speedAttr.addPermanentModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    "Weight speed penalty",
                    multiplier - 1.0,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}
