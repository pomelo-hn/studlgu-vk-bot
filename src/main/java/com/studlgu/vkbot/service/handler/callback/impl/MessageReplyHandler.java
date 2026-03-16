package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class MessageReplyHandler implements ICallbackHandler {

    @Override
    public String handle(CallbackRequest request) {
        return "ok";
    }
}
