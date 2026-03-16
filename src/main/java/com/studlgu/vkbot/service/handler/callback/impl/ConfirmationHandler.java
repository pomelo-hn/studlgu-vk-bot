package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfirmationHandler implements ICallbackHandler {

    @Value("${vkbot.group-id}")
    private String groupId;

    @Value("${vkbot.confirmation-code}")
    private String confirmationCode;

    @Override
    public String handle(CallbackRequest request) {
        if (request.getGroupId().equals(Long.valueOf(groupId))) {
            return confirmationCode;
        }

        return "err";
    }
}
