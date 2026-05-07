package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class EventDayDetailCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("ru"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public CommandType getType() {
        return CommandType.EVENT_DAY_DETAIL;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            String dateStr = request.getObject().getMessage().getMappedPayload().getDate();
            LocalDate date = LocalDate.parse(dateStr);
            List<Event> events = eventService.getEventsForDate(date);

            String message;
            if (events.isEmpty()) {
                message = "На эту дату событий нет.";
            } else {
                StringBuilder sb = new StringBuilder("\uD83D\uDCC5 ")
                        .append(date.format(DATE_FMT))
                        .append("\n\n");
                for (int i = 0; i < events.size(); i++) {
                    Event e = events.get(i);
                    sb.append(i + 1).append(". ").append(e.getTitle()).append("\n");
                    if (e.getTime() != null) {
                        sb.append("   ⏰ ").append(e.getTime().format(TIME_FMT)).append("\n");
                    }
                    if (e.getDescription() != null && !e.getDescription().isBlank()) {
                        sb.append("   \uD83D\uDCDD ").append(e.getDescription()).append("\n");
                    }
                    if (e.getLocation() != null && !e.getLocation().isBlank()) {
                        sb.append("   \uD83D\uDCCD ").append(e.getLocation()).append("\n");
                    }
                    sb.append("\n");
                }
                message = sb.toString().trim();
            }

            vkApiClient.messages().sendDeprecated(userActor)
                    .message(message)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
