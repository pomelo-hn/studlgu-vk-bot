package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionTextType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StandardKeyboard {

    private StandardKeyboard() {}

    public static Keyboard createkeyboard() {
        return createkeyboard(false);
    }

    public static Keyboard createkeyboard(Boolean isUserHasEditorRights) {
        List<List<KeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                btn("✨ Мотивашки", "motivation"),
                btn("🔔 Расписание звонков", "bell_schedule")
        ));

        rows.add(List.of(
                btn("📅 Какая неделя?", "which_week")
        ));

        rows.add(List.of(
                btn("📅 Календарь событий", "calendar")
        ));

        rows.add(List.of(
                btn("Обращение", "create_appeal")
        ));

        if (!Boolean.FALSE.equals(isUserHasEditorRights)) {
            rows.add(List.of(
                    btn("Админ панель", "admin_panel")
            ));
        }

        return new Keyboard().setButtons(rows).setInline(false);
    }

    public static Keyboard createAdminKeyboard() {
        return new Keyboard()
                .setButtons(List.of(
                        List.of(
                                btn("➕ Добавить событие", "add_event"),
                                btn("🗑 Удалить событие", "delete_event")
                        ),
                        List.of(btn("Обращения", "appeals")),
                        List.of(btn("✉️ Добавить мотивашку", "add_motivation")),
                        List.of(btn("⬅️ Обычное меню", "start"))
                ))
                .setInline(false);
    }

    public static Keyboard createCancelKeyboard() {
        return new Keyboard()
                .setButtons(List.of(List.of(btn("❌ Отмена", "cancel"))))
                .setInline(false);
    }

    public static Keyboard createOptionalEventStepKeyboard() {
        return new Keyboard()
                .setButtons(List.of(List.of(
                        btn("Пропустить", "skip"),
                        btn("❌ Отмена", "cancel")
                )))
                .setInline(false);
    }

    public static Keyboard createDeleteEventKeyboard(List<String> eventIds) {
        List<List<KeyboardButton>> rows = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();
        for (String eventId : eventIds) {
            currentRow.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                    .setLabel("ID: " + eventId.substring(0, 8))
                    .setPayload("{\"command\": \"delete_event_by_id\", \"event_id\": \"" + eventId + "\"}")
                    .setType(KeyboardButtonActionTextType.TEXT)));
            if (currentRow.size() == 2) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        rows.add(List.of(btn("❌ Отмена", "cancel")));
        return new Keyboard().setButtons(rows).setInline(false);
    }

    public static Keyboard createAppealsListKeyboard(List<String> appealIds) {
        List<List<KeyboardButton>> rows = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();
        for (String appealId : appealIds) {
            currentRow.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                    .setLabel("ID: " + appealId.substring(0, Math.min(8, appealId.length())))
                    .setPayload("{\"command\": \"select_appeal\", \"appeal_id\": \"" + appealId + "\"}")
                    .setType(KeyboardButtonActionTextType.TEXT)));
            if (currentRow.size() == 2) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        rows.add(List.of(btn("❌ Отмена", "cancel")));
        return new Keyboard().setButtons(rows).setInline(false);
    }

    public static Keyboard createEventDayKeyboard(List<LocalDate> dates) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM", Locale.of("ru"));
        List<List<KeyboardButton>> rows = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();
        for (LocalDate date : dates) {
            String label = date.format(fmt);
            String dateStr = date.toString();
            currentRow.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                    .setLabel(label)
                    .setPayload("{\"command\": \"event_day_detail\", \"date\": \"" + dateStr + "\"}")
                    .setType(KeyboardButtonActionTextType.TEXT)));
            if (currentRow.size() == 3) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        return new Keyboard().setButtons(rows).setInline(false);
    }

    private static KeyboardButton btn(String label, String command) {
        return new KeyboardButton().setAction(
                new KeyboardButtonActionText()
                        .setLabel(label)
                        .setPayload("{\"command\": \"" + command + "\"}")
                        .setType(KeyboardButtonActionTextType.TEXT));
    }

    public static Keyboard createCalendarSubmenu() {
        List<List<KeyboardButton>> rows = new ArrayList<>();

        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("📅 Ближайшая неделя")
                .setPayload("{\"command\": \"calendar_week\"}")
                .setType(KeyboardButtonActionTextType.TEXT)));
        row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("📆 Этот месяц")
                .setPayload("{\"command\": \"calendar_month_current\"}")
                .setType(KeyboardButtonActionTextType.TEXT)));
        rows.add(row1);

        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                .setLabel("🗓 Выбрать месяц")
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
