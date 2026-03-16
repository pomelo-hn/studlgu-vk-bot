package com.studlgu.vkbot.service.handler.button.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.button.ButtonHandler;
import com.studlgu.vkbot.service.handler.button.ButtonType;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallback;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionCallbackType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class MotivationButtonHandler implements ButtonHandler {

    @Value("${vkbot.access-token}")
    private String accessToken;

    @Override
    public ButtonType getType() {
        return ButtonType.MOTIVATION_BTN;
    }

    @Override
    public void handle(CallbackRequest request) { //TODO: запросить список мотивашек
        VkApiClient vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        UserActor userActor = new UserActor(request.getObject().getUserId(), accessToken);

        try {
            int randomId = Math.abs(new Random().nextInt(10000));
            vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message("Ваша мотивашка" + randomId)
                    .keyboard(createkeyboard())
                    .userId(userActor.getId())
                    .randomId(randomId)
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private Keyboard createkeyboard() {
        List<List<KeyboardButton>> keyboardButtonList = new ArrayList<>();

        List<KeyboardButton> keyboardButton = new ArrayList<>();
        keyboardButton.add(
                new KeyboardButton()
                        .setAction(
                                new KeyboardButtonActionCallback()
                                        .setLabel("✨Мотивашки")
                                        .setPayload("{\"button_type\": \"motivation_btn\"}")
                                        .setType(KeyboardButtonActionCallbackType.CALLBACK))); //TODO: перейти на тип TEXT
        keyboardButtonList.add(keyboardButton);

        return new Keyboard()
                .setButtons(keyboardButtonList)
                .setInline(false);
    }
}
