package com.studlgu.vkbot.controller;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.callback.CallbackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@RestController
@RequestMapping("/callback")
public class CallbackController {

    public CallbackController(CallbackService service) {
        this.service = service;
        System.setProperty("api.host", "api.vk.ru"); //TODO: переезд в конифгурацию
        System.setProperty("oauth.host", "oauth.vk.ru");
    }

    private final CallbackService service;

    @PostMapping
    public String callback(@RequestBody CallbackRequest request) {
        Optional.ofNullable(request.getObject())
                .map(CallbackObject::getMessage)
                .map(CallbackMessage::getPayload)
                .ifPresent(payloadString -> {
                    Payload payload = new ObjectMapper().readValue(payloadString, Payload.class);

                    request.getObject()
                            .getMessage()
                            .setMappedPayload(payload);
                });

        return service.handle(request);
    }
}
