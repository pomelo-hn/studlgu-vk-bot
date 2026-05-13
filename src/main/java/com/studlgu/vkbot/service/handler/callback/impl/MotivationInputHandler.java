package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.MotivationRepository;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Component("motivationInputHandler")
@RequiredArgsConstructor
public class MotivationInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final UserStateCache userStateCache;
    private final MotivationRepository motivationRepository;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String text = Optional.ofNullable(request.getObject().getMessage().getText()).orElse("");

        try {
            if (!roleIdentifier.hasEditorRights(vkApiClient, userActor)) {
                userStateCache.clearState(userActor.getId());
                sendMessage(userActor, "У вас нет прав администратора.", StandardKeyboard.createkeyboard(false));
                return "ok";
            }

            if (text.isBlank()) {
                sendMessage(userActor, "Текст мотивашки обязателен. Отправьте текст.", StandardKeyboard.createCancelKeyboard());
                return "ok";
            }

            motivationRepository.add(text);
            userStateCache.clearState(userActor.getId());
            sendMessage(userActor, "Мотивашка добавлена.", StandardKeyboard.createAdminKeyboard());
            return "ok";
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(UserActor userActor, String message, Keyboard keyboard) throws ApiException, ClientException {
        vkApiClient
                .messages()
                .sendDeprecated(userActor)
                .message(message)
                .keyboard(keyboard)
                .userId(userActor.getId())
                .randomId(Math.abs(new Random().nextInt(10000)))
                .execute();
    }
}
