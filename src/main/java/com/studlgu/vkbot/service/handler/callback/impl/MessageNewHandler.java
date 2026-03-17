package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class MessageNewHandler implements ICallbackHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageNewHandler.class);

    @Value("${vkbot.access-token}")
    private String accessToken;

    @Override
    public String handle(CallbackRequest request) {
        VkApiClient vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        UserActor userActor = new UserActor(request.getObject().getMessage().getFromId(), accessToken);

        boolean isCommandStart = Optional.ofNullable(request.getObject())
                .map(CallbackObject::getMessage)
                .map(CallbackMessage::getPayload)
                .filter(payload -> payload.contains("start"))
                .isPresent();

        try {
            if (isCommandStart) {
                int randomId = Math.abs(new Random().nextInt(10000));
                LOG.info("Random id {}", randomId);
                vkApiClient
                        .messages()
                        .sendDeprecated(userActor)
                        .message("Выберите действие: " + randomId)
                        .keyboard(createkeyboard())
                        .userId(userActor.getId())
                        .randomId(randomId)
                        .execute();

                LOG.info("Message sent with id {}", randomId);
            }
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }

    private Keyboard createkeyboard() {
        List<List<KeyboardButton>> keyboardButtonList = new ArrayList<>();

        List<KeyboardButton> keyboardButton = new ArrayList<>();
        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionText()
                                        .setLabel("✨Мотивашки")
                                        .setPayload("{\"button_type\": \"motivation_btn\"}")
                                        .setType(KeyboardButtonActionTextType.TEXT)));
        keyboardButtonList.add(keyboardButton);

        return new Keyboard()
                .setButtons(keyboardButtonList)
                .setInline(false);
    }
}
