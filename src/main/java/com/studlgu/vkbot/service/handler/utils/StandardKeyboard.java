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

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83D\uDCC5 Календарь событий")
                                        .setPayload("{\"command\": \"calendar\"}")
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

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("➕ Добавить событие")
                                        .setPayload("{\"command\": \"add_event\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));

        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("\uD83D\uDDD1 Удалить событие")
                                        .setPayload("{\"command\": \"delete_event\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));
	}

    public static Keyboard createCalendarSubmenu() {
        List<List<KeyboardButton>> rows = new ArrayList<>();

        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("\uD83D\uDCC5 Ближайшая неделя")
                .setPayload("{\"command\": \"calendar_week\"}")
                .setType(KeyboardButtonActionTextType.TEXT)));
        row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("\uD83D\uDCC6 Этот месяц")
                .setPayload("{\"command\": \"calendar_month_current\"}")
                .setType(KeyboardButtonActionTextType.TEXT)));
        rows.add(row1);

        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("\uD83D\uDDD3 Выбрать месяц")
                .setPayload("{\"command\": \"calendar_month_select\"}")
                .setType(KeyboardButtonActionTextType.TEXT)));
        rows.add(row2);

        return new Keyboard().setButtons(rows).setInline(false);
    }

    public static Keyboard createMonthSelectKeyboard() {
        String[] months = {
            "Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };

        List<List<KeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < 12; i += 3) {
            List<KeyboardButton> row = new ArrayList<>();
            for (int j = i; j < Math.min(i + 3, 12); j++) {
                int monthNum = j + 1;
                row.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                        .setLabel(months[j])
                        .setPayload("{\"command\": \"calendar_month\", \"month\": " + monthNum + "}")
                        .setType(KeyboardButtonActionTextType.TEXT)));
            }
            rows.add(row);
        }

        return new Keyboard().setButtons(rows).setInline(false);
    }
}
