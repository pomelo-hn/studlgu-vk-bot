package com.studlgu.vkbot.service.handler;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfirmationHandler implements ICallbackHandler {

    @Value("${vkbot.group-id}")
    private String groupId;

    @Value("${vkbot.confirmation-code}")
    private String confirmationCode;

    @Override
    public String handle(ConfirmationServerRequest request) {
        if (request.getGroupId().equals(Long.valueOf(groupId))) {
            return confirmationCode;
        }

        return "err";
    }
}
