package com.studlgu.vkbot.callback;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfirmationServerCallback {

    @Value("${vkbot.group.id}")
    private String groupId;

    @Value("${vkbot.confirmation-code}")
    private String confirmationCode;

    @PostMapping("/callback")
    public String callback(@RequestBody ConfirmationServerRequest request) {
        if (request.getType().contains("confirmation") && request.getGroupId().equals(Long.valueOf(groupId))) {
            return confirmationCode;
        }

        return "callback was not recognized";
    }
}
