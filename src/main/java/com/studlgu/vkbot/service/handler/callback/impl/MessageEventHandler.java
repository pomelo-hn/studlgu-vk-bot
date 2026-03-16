package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.button.ButtonHandlerService;
import com.studlgu.vkbot.service.handler.button.ButtonType;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageEventHandler implements ICallbackHandler {

    private final ButtonHandlerService buttonHandlerService;

    @Override
    public String handle(CallbackRequest request) {
        String buttonType = request.getObject().getPayload().getButtonType().toUpperCase();
        buttonHandlerService.handle(ButtonType.valueOf(buttonType), request);

        return "ok";
    }
}
