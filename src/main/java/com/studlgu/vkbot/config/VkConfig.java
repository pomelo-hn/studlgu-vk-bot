package com.studlgu.vkbot.config;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VkConfig {

    @Bean
    public VkApiClient vkApiClient() {
        System.setProperty("api.host", "api.vk.ru");
        System.setProperty("oauth.host", "oauth.vk.ru");
        return new VkApiClient(HttpTransportClient.getInstance());
    }
}