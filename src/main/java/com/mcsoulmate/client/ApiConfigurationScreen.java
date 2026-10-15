package com.mcsoulmate.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class ApiConfigurationScreen extends Screen {
    private static final int FORM_WIDTH = 320;
    private static final int MIN_FORM_WIDTH = 120;
    private static final int SCREEN_MARGIN = 16;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_ROW_SPACING = 46;
    private static final int MIN_FORM_TOP = 64;
    private static final int FORM_TOP_OFFSET = 125;
    private static final int LABEL_VERTICAL_OFFSET = 13;
    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_BOTTOM_MARGIN = 8;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final Component TITLE = Component.translatable("screen.mcsoulmate.api_configuration.title");

    private final Screen parentScreen;
    private ApiConfiguration draft;
    private EditBox providerField;
    private EditBox baseUrlField;
    private EditBox apiKeyField;
    private EditBox modelField;

    public ApiConfigurationScreen(Screen parentScreen) {
        super(TITLE);
        this.parentScreen = parentScreen;
        this.draft = ApiConfigurationStore.load();
    }

    @Override
    protected void init() {
        this.saveDraft();
        int firstFieldY = this.firstFieldY();
        this.providerField = this.addField("provider", this.draft.provider(), firstFieldY);
        this.baseUrlField = this.addField("base_url", this.draft.baseUrl(), firstFieldY + FIELD_ROW_SPACING);
        this.apiKeyField = this.addApiKeyField(firstFieldY + FIELD_ROW_SPACING * 2);
        this.apiKeyField.setFormatter((text, cursor) ->
                FormattedCharSequence.forward("•".repeat(text.length()), Style.EMPTY)
        );
        this.modelField = this.addField("model", this.draft.model(), firstFieldY + FIELD_ROW_SPACING * 3);
        this.addActionButtons();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.drawFieldLabels(graphics, this.firstFieldY());
        graphics.centeredText(
                this.font,
                Component.translatable("screen.mcsoulmate.api_configuration.local_notice"),
                this.width / 2,
                this.height - BUTTON_HEIGHT - BUTTON_BOTTOM_MARGIN - 22,
                LABEL_COLOR
        );
    }

    @Override
    public void onClose() {
        net.minecraft.client.Minecraft.getInstance().setScreen(this.parentScreen);
    }

    private EditBox addField(String key, String value, int y) {
        int width = this.fieldWidth();
        int x = (this.width - width) / 2;
        Component label = Component.translatable("screen.mcsoulmate.api_configuration." + key);
        EditBox field = new EditBox(this.font, x, y, width, FIELD_HEIGHT, label);
        field.setMaxLength(Integer.MAX_VALUE);
        field.setValue(value);
        return this.addRenderableWidget(field);
    }

    private EditBox addApiKeyField(int y) {
        int width = this.fieldWidth();
        int x = (this.width - width) / 2;
        Component label = Component.translatable("screen.mcsoulmate.api_configuration.api_key");
        EditBox field = new EditBox(this.font, x, y, width, FIELD_HEIGHT, label) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return label.copy();
            }
        };
        field.setMaxLength(Integer.MAX_VALUE);
        field.setValue(this.draft.apiKey());
        return this.addRenderableWidget(field);
    }

    private void addActionButtons() {
        int groupWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int firstButtonX = (this.width - groupWidth) / 2;
        int buttonY = this.height - BUTTON_HEIGHT - BUTTON_BOTTOM_MARGIN;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mcsoulmate.api_configuration.save"),
                button -> this.saveConfiguration()
        ).bounds(firstButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mcsoulmate.api_configuration.cancel"),
                button -> this.onClose()
        ).bounds(firstButtonX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void drawFieldLabels(GuiGraphicsExtractor graphics, int firstFieldY) {
        this.drawFieldLabel(graphics, "provider", firstFieldY);
        this.drawFieldLabel(graphics, "base_url", firstFieldY + FIELD_ROW_SPACING);
        this.drawFieldLabel(graphics, "api_key", firstFieldY + FIELD_ROW_SPACING * 2);
        this.drawFieldLabel(graphics, "model", firstFieldY + FIELD_ROW_SPACING * 3);
    }

    private void drawFieldLabel(GuiGraphicsExtractor graphics, String field, int y) {
        int x = (this.width - this.fieldWidth()) / 2;
        Component label = Component.translatable("screen.mcsoulmate.api_configuration." + field);
        graphics.text(this.font, label, x, y - LABEL_VERTICAL_OFFSET, LABEL_COLOR);
    }

    private void saveConfiguration() {
        this.draft = new ApiConfiguration(
                this.providerField.getValue(),
                this.baseUrlField.getValue(),
                this.apiKeyField.getValue(),
                this.modelField.getValue()
        );
        ApiConfigurationStore.save(this.draft);
        net.minecraft.client.Minecraft.getInstance().setScreen(this.parentScreen);
    }

    private void saveDraft() {
        if (this.providerField == null) {
            return;
        }
        this.draft = new ApiConfiguration(
                this.providerField.getValue(),
                this.baseUrlField.getValue(),
                this.apiKeyField.getValue(),
                this.modelField.getValue()
        );
    }

    private int fieldWidth() {
        return Math.max(MIN_FORM_WIDTH, Math.min(FORM_WIDTH, this.width - SCREEN_MARGIN * 2));
    }

    private int firstFieldY() {
        return Math.max(MIN_FORM_TOP, this.height / 2 - FORM_TOP_OFFSET);
    }
}
