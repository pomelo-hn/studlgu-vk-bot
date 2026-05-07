package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.EventDraftCache;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class AddEventCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final UserStateCache userStateCache;
    private final EventDraftCache eventDraftCache;

    @Override
    public CommandType getType() {
        return CommandType.ADD_EVENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            eventDraftCache.createDraft(userActor.getId());
            userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_TITLE);
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("""
                            Отправь название события.
                            Например: Экзамен по математике""")
                    .keyboard(StandardKeyboard.createCancelKeyboard())
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
