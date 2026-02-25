package weight;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class WeightHud {
    public static final WeightHud INSTANCE = new WeightHud();

    public float xRatio = 0.05f;
    public float yRatio = 0.05f;
    public float scale = 1.0f;
    public float alpha = 1.0f;
    public boolean enabled = true;

    private final Minecraft mc = Minecraft.getInstance();

    private WeightHud() {}

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled || mc.player == null || mc.options.hideGui || mc.screen != null) return;

        Player player = mc.player;
        double current = WeightCalculator.getCurrentWeight(player);
        double max = WeightCalculator.getMaxWeight(player);
        float percent = (float) (current / max);

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        String text = String.format("总负重: %.1f / %.1f (%.0f%%)", current, max, percent * 100);
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;
        float scaledWidth = textWidth * scale;
        float scaledHeight = textHeight * scale;

        float pixelX = Math.max(0, Math.min(xRatio * screenWidth, screenWidth - scaledWidth));
        float pixelY = Math.max(0, Math.min(yRatio * screenHeight, screenHeight - scaledHeight));

        g.pose().pushPose();
        g.pose().translate(pixelX, pixelY, 0);
        g.pose().scale(scale, scale, 1);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, alpha);

        int color = percent > 1.0f ? 0xFF0000 :
                percent > 0.8f ? 0xFF5555 :
                        percent > 0.5f ? 0xFFFF55 : 0x55FF55;
        g.drawString(font, text, 0, 0, color);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().popPose();
    }
}
