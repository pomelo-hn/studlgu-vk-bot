package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
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
public class ImportDataCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final UserStateCache userStateCache;

    @Override
    public CommandType getType() {
        return CommandType.IMPORT_DATA;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            if (!roleIdentifier.hasEditorRights(vkApiClient, userActor)) {
                sendMessage(userActor, "У вас нет прав администратора.", StandardKeyboard.createkeyboard(false));
                return;
            }

            userStateCache.setState(userActor.getId(), UserState.AWAITING_DATA_IMPORT);
            sendMessage(userActor,
                    "Отправьте Base64-бэкап одним сообщением. Импорт заменит события, обращения, мотивашки и фото меню.",
                    StandardKeyboard.createCancelKeyboard());
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(UserActor userActor, String message, com.vk.api.sdk.objects.messages.Keyboard keyboard)
            throws ApiException, ClientException {
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
