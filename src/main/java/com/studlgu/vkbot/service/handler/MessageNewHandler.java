package com.studlgu.vkbot.service.handler;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageNewHandler implements ICallbackHandler {

    @Value("${vkbot.access-token}")
    private String accessToken;

    @Override
    public String handle(ConfirmationServerRequest request) {
        VkApiClient vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        UserActor userActor = new UserActor(request.getObject().getMessage().getFromId(), accessToken);
        try {
            vkApiClient.messages().sendUserIds(userActor).message("hi test").execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }
}
