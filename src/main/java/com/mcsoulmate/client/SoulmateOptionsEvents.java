package com.mcsoulmate.client;

import com.mcsoulmate.McSoulmate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = McSoulmate.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class SoulmateOptionsEvents {
    private static final int API_BUTTON_WIDTH = 200;
    private static final int API_BUTTON_HEIGHT = 20;
    private static final int API_BUTTON_BOTTOM_OFFSET = 52;

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
                pressed -> Minecraft.getInstance().setScreen(new ApiConfigurationScreen(optionsScreen))
        ).bounds(
                optionsScreen.width / 2 - API_BUTTON_WIDTH / 2,
                optionsScreen.height - API_BUTTON_BOTTOM_OFFSET,
                API_BUTTON_WIDTH,
                API_BUTTON_HEIGHT
        ).build();
        event.addListener(button);
    }
}
