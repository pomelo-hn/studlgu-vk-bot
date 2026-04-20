package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("addEventInputHandler")
@RequiredArgsConstructor
public class AddEventInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final UserStateCache userStateCache;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String text = request.getObject().getMessage().getText();
        try {
            eventService.addEvent(text);
            userStateCache.clearState(userActor.getId());
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("✅ Событие добавлено!")
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (IllegalArgumentException e) {
            try {
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("❌ Ошибка: " + e.getMessage())
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
            } catch (ApiException | ClientException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }
}
