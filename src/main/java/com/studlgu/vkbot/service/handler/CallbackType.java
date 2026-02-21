package com.studlgu.vkbot.service.handler;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum CallbackType {
    CONFIRMATION("confirmation", "confirmationHandler"),
    MESSAGE_NEW("message_new", "messageNewHandler");

    private final String callbackType;
    private final String handlerName;

    CallbackType(String callbackType, String handlerName) {
        this.callbackType = callbackType;
        this.handlerName = handlerName;
    }

    public static Optional<CallbackType> findByType(String callbackType) {
        return Arrays.stream(values())
                .filter(type -> type.callbackType.equals(callbackType))
                .findFirst();
    }
}
