package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.AppealService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class AppealsCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final AppealService appealService;

    @Override
    public CommandType getType() {
        return CommandType.APPEALS;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            boolean hasEditorRights = roleIdentifier.hasEditorRights(vkApiClient, userActor);
            if (!hasEditorRights) {
                sendMessage(userActor, "У вас нет прав для просмотра обращений.",
                        StandardKeyboard.createkeyboard(false));
                return;
            }

            List<Appeal> appeals = appealService.listOpen();
            if (appeals.isEmpty()) {
                sendMessage(userActor, "Открытых обращений нет.",
                        StandardKeyboard.createkeyboard(true));
                return;
            }

            StringBuilder message = new StringBuilder("Выберите обращение для ответа:\n\n");
            for (Appeal appeal : appeals) {
                message.append("ID: ")
                        .append(shortId(appeal.getId()))
                        .append(" от пользователя ")
                        .append(appeal.getUserId())
                        .append("\n")
                        .append(appeal.getText())
                        .append("\n\n");
            }

            sendMessage(userActor, message.toString().trim(),
                    StandardKeyboard.createAppealsListKeyboard(appeals.stream()
                            .map(Appeal::getId)
                            .toList()));
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(UserActor userActor, String message, com.vk.api.sdk.objects.messages.Keyboard keyboard)
            throws ApiException, ClientException {
        vkApiClient.messages().sendDeprecated(userActor)
                .message(message)
                .keyboard(keyboard)
                .userId(userActor.getId())
                .randomId(Math.abs(new Random().nextInt(10000)))
                .execute();
    }

    private String shortId(String id) {
        if (id == null) {
            return "";
        }
        return id.substring(0, Math.min(8, id.length()));
    }
}
