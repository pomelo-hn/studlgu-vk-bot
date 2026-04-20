package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DeleteEventCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final EventService eventService;
    private final UserStateCache userStateCache;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public CommandType getType() {
        return CommandType.DELETE_EVENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            List<Event> events = eventService.listAll();

            if (events.isEmpty()) {
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("Нет событий для удаления.")
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
                return;
            }

            StringBuilder sb = new StringBuilder("Отправь ID события для удаления:\n\n");
            for (Event event : events) {
                sb.append("ID: ").append(event.getId(), 0, 8)
                  .append("  ").append(event.getTitle())
                  .append(" — ").append(event.getDate().format(DATE_FMT))
                  .append("\n\n");
            }

            userStateCache.setState(userActor.getId(), UserState.AWAITING_DELETE_ID);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message(sb.toString())
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
