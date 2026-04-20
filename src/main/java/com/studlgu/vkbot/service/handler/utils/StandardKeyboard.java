package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionTextType;

import java.util.ArrayList;
import java.util.List;

public class StandardKeyboard {

    private StandardKeyboard() {}

    public static Keyboard createkeyboard() {
        return createkeyboard(false);
    }

    public static Keyboard createkeyboard(Boolean isUserHasEditorRights) { //TODO: докинуть разные уровни, чтобы было не в одну строчку
        List<List<KeyboardButton>> keyboardButtonList = new ArrayList<>();

        List<KeyboardButton> keyboardButton = new ArrayList<>();
        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("✨ Мотивашки")
                                        .setPayload("{\"command\": \"motivation\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83D\uDD14 Расписание звонков")
                                        .setPayload("{\"command\": \"bell_schedule\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83D\uDCC5 Какая неделя?")
                                        .setPayload("{\"command\": \"which_week\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83C\uDF7D Получить меню")
                                        .setPayload("{\"command\": \"get_menu\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));

        addEditorButtons(keyboardButton, isUserHasEditorRights);

        keyboardButtonList.add(keyboardButton);

        return new Keyboard()
                .setButtons(keyboardButtonList)
                .setInline(false);
    }

	private static void addEditorButtons(List<KeyboardButton> keyboardButton, Boolean isUserHasEditorRights) {
        if (Boolean.FALSE.equals(isUserHasEditorRights)) {
            return;
        }

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83D\uDCE5 Загрузить меню")
                                        .setPayload("{\"command\": \"upload_menu\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));
	}
}
