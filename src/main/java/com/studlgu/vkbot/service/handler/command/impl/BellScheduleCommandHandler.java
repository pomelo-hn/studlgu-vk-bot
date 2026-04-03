package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class BellScheduleCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;

    @Override
    public CommandType getType() {
        return CommandType.BELL_SCHEDULE;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            LocalDateTime dateTime = LocalDateTime.now();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            String date = dateTime.format(dateFormatter);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String time = dateTime.format(timeFormatter);

            int randomId = Math.abs(new Random().nextInt(10000));
            vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message("\uD83D\uDD14 Расписание звонков: \n" +
                            " \n" +
                            "• 1 пара | 9.00 — 10.20 \n" +
                            "• 2 пара | 10.30 — 11.50 \n" +
                            "• Обед⠀ | 11.50 — 12.50 \n" +
                            "• 3 пара | 12.50 — 14.10 \n" +
                            "• 4 пара | 14.20 — 15.40 \n" +
                            "• 5 пара | 15.50 — 17.10 \n" +
                            "• 6 пара | 17.20 — 18.40 \n" +
                            "• 7 пара | 18.50 — 20.10 \n" +
                            "\n" +
                            "\n" +
                            "Текущая дата: " + date + "\n" +
                            "Время:" + time)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(randomId)
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
