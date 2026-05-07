package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void createDeleteEventKeyboardContainsDeleteEventPayloads() {
        Keyboard keyboard = StandardKeyboard.createDeleteEventKeyboard(List.of(
                "12345678-aaaa-bbbb-cccc-123456789abc",
                "87654321-aaaa-bbbb-cccc-123456789abc"
        ));

        assertThat(keyboard.getButtons())
                .flatExtracting(row -> row)
                .map(KeyboardButton::getAction)
                .map(KeyboardButtonActionText.class::cast)
                .extracting(KeyboardButtonActionText::getPayload)
                .contains(
                        "{\"command\": \"delete_event_by_id\", \"event_id\": \"12345678-aaaa-bbbb-cccc-123456789abc\"}",
                        "{\"command\": \"delete_event_by_id\", \"event_id\": \"87654321-aaaa-bbbb-cccc-123456789abc\"}"
                );
    }
}
