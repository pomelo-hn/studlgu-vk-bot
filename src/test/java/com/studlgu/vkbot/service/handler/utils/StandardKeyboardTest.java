package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardKeyboardTest {

    @Test
    void createCancelKeyboardContainsCancelCommand() {
        Keyboard keyboard = StandardKeyboard.createCancelKeyboard();

        assertThat(keyboard.getButtons())
                .flatExtracting(row -> row)
                .map(KeyboardButton::getAction)
                .map(KeyboardButtonActionText.class::cast)
                .extracting(KeyboardButtonActionText::getPayload)
                .contains("{\"command\": \"cancel\"}");
    }
}
