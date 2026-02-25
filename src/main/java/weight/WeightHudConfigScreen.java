package weight;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;

@OnlyIn(Dist.CLIENT)
public class WeightHudConfigScreen extends Screen {
    private final Screen parent;
    private final WeightHud hud = WeightHud.INSTANCE;

    public WeightHudConfigScreen(Screen parent) {
        super(Component.literal("负重HUD设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 200;
        int btnHeight = 20;
        int startY = height / 2 - 85;
        int spacing = 25;

        addRenderableWidget(new ForgeSlider(
                width / 2 - btnWidth / 2, startY, btnWidth, btnHeight,
                Component.literal("X位置: "), Component.empty(),
                0.0, 100.0, hud.xRatio * 100, 0.1, 1, true
        ) {
            @Override
            protected void applyValue() {
                hud.xRatio = (float) (getValue() / 100.0);
                WeightHudConfig.save();
            }

            @Override
            protected void updateMessage() {
                int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                float pixelX = (float) (getValue() / 100.0 * screenWidth);
                setMessage(Component.literal(String.format("X位置: %.1f%% (%.0fpx)", getValue(), pixelX)));
            }
        });

        // Y位置滑块
        addRenderableWidget(new ForgeSlider(
                width / 2 - btnWidth / 2, startY + spacing, btnWidth, btnHeight,
                Component.literal("Y位置: "), Component.empty(),
                0.0, 100.0, hud.yRatio * 100, 0.1, 1, true
        ) {
            @Override
            protected void applyValue() {
                hud.yRatio = (float) (getValue() / 100.0);
                WeightHudConfig.save();
            }

            @Override
            protected void updateMessage() {
                int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                float pixelY = (float) (getValue() / 100.0 * screenHeight);
                setMessage(Component.literal(String.format("Y位置: %.1f%% (%.0fpx)", getValue(), pixelY)));
            }
        });

        addRenderableWidget(new ForgeSlider(
                width / 2 - btnWidth / 2, startY + spacing * 2, btnWidth, btnHeight,
                Component.literal("缩放: "), Component.empty(),
                0.5, 3.0, hud.scale, 0.05, 2, true
        ) {
            @Override
            protected void applyValue() {
                hud.scale = (float) getValue();
                WeightHudConfig.save();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(String.format("缩放: %.2f", getValue())));
            }
        });

        addRenderableWidget(new ForgeSlider(
                width / 2 - btnWidth / 2, startY + spacing * 3, btnWidth, btnHeight,
                Component.literal("透明度: "), Component.empty(),
                0.0, 100.0, hud.alpha * 100, 0.5, 1, true
        ) {
            @Override
            protected void applyValue() {
                hud.alpha = (float) (getValue() / 100.0);
                WeightHudConfig.save();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(String.format("透明度: %.1f%%", getValue())));
            }
        });

        addRenderableWidget(Button.builder(
                        Component.literal(hud.enabled ? "§a✔ 显示HUD" : "§c✘ 隐藏HUD"),
                        btn -> {
                            hud.enabled = !hud.enabled;
                            btn.setMessage(Component.literal(hud.enabled ? "§a✔ 显示HUD" : "§c✘ 隐藏HUD"));
                            WeightHudConfig.save();
                        })
                .pos(width / 2 - btnWidth / 2, startY + spacing * 4)
                .size(btnWidth, btnHeight)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("返回"),
                        btn -> minecraft.setScreen(parent))
                .pos(width / 2 - btnWidth / 2, startY + spacing * 5 + 10)
                .size(btnWidth, btnHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}