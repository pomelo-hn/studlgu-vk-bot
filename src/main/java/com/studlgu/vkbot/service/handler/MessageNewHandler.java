package com.studlgu.vkbot.service.handler;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MessageNewHandler implements ICallbackHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageNewHandler.class);

    @Value("${vkbot.access-token}")
    private String accessToken;

    @Override
    public String handle(ConfirmationServerRequest request) {
        VkApiClient vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        UserActor userActor = new UserActor(request.getObject().getMessage().getFromId(), accessToken);
        try {
            int randomId = Math.abs(new Random().nextInt(10000));
            LOG.info("Random id {}", randomId);
            vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message("" + randomId)
                    .userId(userActor.getId())
                    .randomId(randomId)
                    .execute();

            LOG.info("Message sent with id {}", randomId);
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }
}
