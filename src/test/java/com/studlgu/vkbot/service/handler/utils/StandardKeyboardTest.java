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
    void createCancelKeyboardContainsReadableCancelLabel() {
        Keyboard keyboard = StandardKeyboard.createCancelKeyboard();

        assertThat(labels(keyboard))
                .contains("❌ Отмена");
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

    @Test
    void createKeyboardContainsCreateAppealForAllUsers() {
        Keyboard keyboard = StandardKeyboard.createkeyboard(false);

        assertThat(payloads(keyboard))
                .contains("{\"command\": \"create_appeal\"}");
    }

    @Test
    void createKeyboardContainsAppealsOnlyForEditors() {
        Keyboard userKeyboard = StandardKeyboard.createkeyboard(false);
        Keyboard editorKeyboard = StandardKeyboard.createkeyboard(true);

        assertThat(payloads(userKeyboard))
                .doesNotContain("{\"command\": \"appeals\"}");
        assertThat(payloads(editorKeyboard))
                .contains("{\"command\": \"admin_panel\"}")
                .doesNotContain(
                        "{\"command\": \"appeals\"}",
                        "{\"command\": \"upload_menu\"}",
                        "{\"command\": \"add_event\"}",
                        "{\"command\": \"delete_event\"}",
                        "{\"command\": \"add_motivation\"}"
                );
    }

    @Test
    void createAdminKeyboardContainsAdminCommandsAndMainMenuButton() {
        Keyboard keyboard = StandardKeyboard.createAdminKeyboard();

        assertThat(payloads(keyboard))
                .contains(
                        "{\"command\": \"upload_menu\"}",
                        "{\"command\": \"add_event\"}",
                        "{\"command\": \"delete_event\"}",
                        "{\"command\": \"appeals\"}",
                        "{\"command\": \"add_motivation\"}",
                        "{\"command\": \"export_data\"}",
                        "{\"command\": \"import_data\"}",
                        "{\"command\": \"start\"}"
                );

        assertThat(labels(keyboard))
                .contains("⬅️ Обычное меню");
    }

    @Test
    void createAppealsListKeyboardContainsSelectAppealPayloads() {
        Keyboard keyboard = StandardKeyboard.createAppealsListKeyboard(List.of(
                "12345678-aaaa-bbbb-cccc-123456789abc",
                "87654321-aaaa-bbbb-cccc-123456789abc"
        ));

        assertThat(payloads(keyboard))
                .contains(
                        "{\"command\": \"select_appeal\", \"appeal_id\": \"12345678-aaaa-bbbb-cccc-123456789abc\"}",
                        "{\"command\": \"select_appeal\", \"appeal_id\": \"87654321-aaaa-bbbb-cccc-123456789abc\"}",
                        "{\"command\": \"cancel\"}"
                );
    }

    private List<String> payloads(Keyboard keyboard) {
        return keyboard.getButtons()
                .stream()
                .flatMap(row -> row.stream())
                .map(KeyboardButton::getAction)
                .map(KeyboardButtonActionText.class::cast)
                .map(KeyboardButtonActionText::getPayload)
                .toList();
    }

    private List<String> labels(Keyboard keyboard) {
        return keyboard.getButtons()
                .stream()
                .flatMap(row -> row.stream())
                .map(KeyboardButton::getAction)
                .map(KeyboardButtonActionText.class::cast)
                .map(KeyboardButtonActionText::getLabel)
                .toList();
    }
}
