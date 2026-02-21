package com.studlgu.vkbot.controller;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import com.studlgu.vkbot.service.CallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/callback")
public class CallbackController {

    private final CallbackService service;

    @PostMapping
    public String callback(@RequestBody ConfirmationServerRequest request) {
        return service.handle(request);
    }
}
