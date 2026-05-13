package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.DataBackupService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class ExportDataCommandHandler implements CommandHandler {

    private static final int MESSAGE_CHUNK_SIZE = 3000;

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final DataBackupService dataBackupService;

    @Override
    public CommandType getType() {
        return CommandType.EXPORT_DATA;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            if (!roleIdentifier.hasEditorRights(vkApiClient, userActor)) {
                sendMessage(userActor, "У вас нет прав администратора.", StandardKeyboard.createkeyboard(false));
                return;
            }

            String backup = dataBackupService.exportAsBase64();
            int parts = Math.max(1, (int) Math.ceil((double) backup.length() / MESSAGE_CHUNK_SIZE));
            sendMessage(userActor,
                    "Экспорт данных в Base64. Если частей несколько, склейте их подряд перед импортом. Частей: " + parts,
                    StandardKeyboard.createAdminKeyboard());
            for (int i = 0; i < parts; i++) {
                int start = i * MESSAGE_CHUNK_SIZE;
                int end = Math.min(start + MESSAGE_CHUNK_SIZE, backup.length());
                sendMessage(userActor, "backup:" + (i + 1) + "/" + parts + "\n" + backup.substring(start, end),
                        StandardKeyboard.createAdminKeyboard());
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
