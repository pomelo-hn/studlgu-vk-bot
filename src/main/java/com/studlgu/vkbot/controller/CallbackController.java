package com.studlgu.vkbot.controller;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import com.studlgu.vkbot.service.CallbackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public String callback(@RequestBody ConfirmationServerRequest request) {
        return service.handle(request);
    }
}
