package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.DataBackupService;
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

@Component("dataImportInputHandler")
@RequiredArgsConstructor
public class DataImportInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final UserStateCache userStateCache;
    private final DataBackupService dataBackupService;

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
                sendMessage(userActor, "Base64-бэкап пустой. Отправьте текст бэкапа или нажмите отмену.",
                        StandardKeyboard.createCancelKeyboard());
                return "ok";
            }

            dataBackupService.importFromBase64(text);
            userStateCache.clearState(userActor.getId());
            sendMessage(userActor, "Данные импортированы.", StandardKeyboard.createAdminKeyboard());
            return "ok";
        } catch (IllegalArgumentException e) {
            try {
                sendMessage(userActor, "Не удалось импортировать бэкап: " + e.getMessage(),
                        StandardKeyboard.createCancelKeyboard());
                return "ok";
            } catch (ApiException | ClientException sendException) {
                throw new RuntimeException(sendException);
            }
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
