package com.studlgu.vkbot.service.handler.button;

import com.studlgu.vkbot.model.CallbackRequest;

public interface ButtonHandler {
    ButtonType getType();
    void handle(CallbackRequest request);
}
