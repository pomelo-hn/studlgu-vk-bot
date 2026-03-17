package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.client.actors.UserActor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VkActorFactory {

    @Value("${vkbot.access-token}")
    private String accessToken;

    public UserActor create(Long userId) {
        return new UserActor(userId, accessToken);
    }
}