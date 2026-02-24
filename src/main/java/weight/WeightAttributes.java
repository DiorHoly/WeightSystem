package weight;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = WeightMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WeightAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(
            ForgeRegistries.ATTRIBUTES, WeightMod.MOD_ID
    );

    public static final RegistryObject<Attribute> MAX_CARRY_WEIGHT = ATTRIBUTES.register(
            "max_carry_weight",
            () -> new RangedAttribute(
                    "attribute.name.weight.max_carry_weight",
                    100.0, // 默认值
                    0.0,   // 最小值
                    1000.0 // 最大值
            ).setSyncable(true)
    );

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_CARRY_WEIGHT.get());
        WeightMod.LOGGER.info("已将最大负重属性附加到玩家实体");
    }

    public static void registerAttributes() {
        ATTRIBUTES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        WeightMod.LOGGER.info("自定义属性已注册：max_carry_weight");
    }
}