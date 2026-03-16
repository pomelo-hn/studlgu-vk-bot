package com.studlgu.vkbot.service.handler.callback;

import com.studlgu.vkbot.model.CallbackRequest;

public interface ICallbackHandler {
    String handle(CallbackRequest request);
}
