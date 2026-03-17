package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class WhichWeekCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;

    @Override
    public CommandType getType() {
        return CommandType.WHICH_WEEK;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            LocalDate localDate = LocalDate.now();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            String date = localDate.format(dateFormatter);

            int randomId = Math.abs(new Random().nextInt(10000));
            vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message("Сегодня: " + date + "\n" +
                            "Сейчас верхняя неделя")
                    .keyboard(StandardKeyboard.createkeyboard())
                    .userId(userActor.getId())
                    .randomId(randomId)
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
