package com.studlgu.vkbot.controller;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.callback.CallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@RestController
@RequestMapping("/callback")
@RequiredArgsConstructor
public class CallbackController {

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
