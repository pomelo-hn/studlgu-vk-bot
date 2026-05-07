package com.studlgu.vkbot.service.handler.callback;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum CallbackType {

    CONFIRMATION("confirmation", "confirmationHandler"),
    MESSAGE_NEW("message_new", "messageNewHandler"),
    MESSAGE_EVENT("message_event", "messageEventHandler"),
    MESSAGE_REPLY("message_reply", "messageReplyHandler"),
    UPLOAD_PHOTO("upload_photo", "uploadPhotoHandler"),
    ADD_EVENT_INPUT("add_event_input", "addEventInputHandler"),
    APPEAL_INPUT("appeal_input", "appealInputHandler"),
    DELETE_EVENT_INPUT("delete_event_input", "deleteEventInputHandler");

    private final String callbackName;
    private final String handlerName;

    CallbackType(String callbackName, String handlerName) {
        this.callbackName = callbackName;
        this.handlerName = handlerName;
    }

    public static Optional<CallbackType> findByType(String callbackName) {
        return Arrays.stream(values())
                .filter(type -> type.callbackName.equals(callbackName))
                .findFirst();
    }
}
