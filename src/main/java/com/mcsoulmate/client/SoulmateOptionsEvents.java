package com.mcsoulmate.client;

import com.mcsoulmate.McSoulmate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = McSoulmate.MOD_ID, value = Dist.CLIENT)
public final class SoulmateOptionsEvents {
    private static final int API_BUTTON_WIDTH = 200;
    private static final int API_BUTTON_HEIGHT = 20;
    private static final int API_BUTTON_SPACING = 4;
    private static final String FOV_OPTION_KEY = "options.fov";
    private static final String WORLD_OPTIONS_KEY = "options.worldOptions.button";
    private static final String ONLINE_OPTIONS_KEY = "options.online";

    private SoulmateOptionsEvents() {
    }

    @SubscribeEvent
    public static void addApiConfigurationEntry(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen optionsScreen)) {
            return;
        }

        Component status = ApiConfigurationStore.load().isComplete()
                ? Component.translatable("options.mcsoulmate.api_configuration.saved")
                : Component.translatable("options.mcsoulmate.api_configuration.unconfigured");
        Button button = Button.builder(
                Component.translatable("options.mcsoulmate.api_configuration", status),
                pressed -> Minecraft.getInstance().gui.setScreen(new ApiConfigurationScreen(optionsScreen))
        ).bounds(
                optionsScreen.width / 2 - API_BUTTON_WIDTH / 2,
                optionsHeaderBottom(optionsScreen) + API_BUTTON_SPACING,
                API_BUTTON_WIDTH,
                API_BUTTON_HEIGHT
        ).build();
        event.addListener(button);
    }

    private static int optionsHeaderBottom(OptionsScreen screen) {
        return screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(SoulmateOptionsEvents::isHeaderOption)
                .mapToInt(AbstractWidget::getBottom)
                .max()
                .orElseThrow(() -> new IllegalStateException("Options header controls were not found"));
    }

    private static boolean isHeaderOption(AbstractWidget widget) {
        String message = widget.getMessage().getString();
        String fovLabel = Component.translatable(FOV_OPTION_KEY).getString();
        return message.startsWith(fovLabel)
                || message.equals(Component.translatable(WORLD_OPTIONS_KEY).getString())
                || message.equals(Component.translatable(ONLINE_OPTIONS_KEY).getString());
    }
}
