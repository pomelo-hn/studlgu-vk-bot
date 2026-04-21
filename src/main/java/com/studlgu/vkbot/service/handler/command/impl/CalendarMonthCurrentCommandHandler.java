package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.CalendarImageService;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.studlgu.vkbot.service.handler.utils.VkPhotoUploader;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarMonthCurrentCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final CalendarImageService imageService;
    private final VkPhotoUploader photoUploader;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR_MONTH_CURRENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            YearMonth currentMonth = YearMonth.now();
            List<Event> events = eventService.getEventsForMonth(currentMonth);
            byte[] png = imageService.generateMonthImage(currentMonth, events);
            String attachment = photoUploader.uploadBytes(png, userActor);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message("\uD83D\uDCC6 События текущего месяца:")
                    .attachment(attachment)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();

            if (!events.isEmpty()) {
                List<LocalDate> eventDates = events.stream()
                        .map(Event::getDate)
                        .distinct()
                        .sorted()
                        .toList();
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("Выбери день для подробностей:")
                        .keyboard(StandardKeyboard.createEventDayKeyboard(eventDates))
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
            }
        } catch (ApiException | ClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
