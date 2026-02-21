package com.studlgu.vkbot.service.handler;

import com.studlgu.vkbot.model.ConfirmationServerRequest;

public interface ICallbackHandler {
    String handle(ConfirmationServerRequest request);
}
