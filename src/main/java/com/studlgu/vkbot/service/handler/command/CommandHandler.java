package com.studlgu.vkbot.service.handler.command;

import com.studlgu.vkbot.model.CallbackRequest;

public interface CommandHandler {
    CommandType getType();
    void handle(CallbackRequest request);
}
