package com.studlgu.vkbot.callback;

import com.studlgu.vkbot.model.ConfirmationServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ConfirmationServerCallback.class);

    @PostMapping("/callback")
    public String callback(@RequestBody ConfirmationServerRequest request) {
        log.info("body type: " + request.getType());
        log.info("body group: " + request.getGroupId());
        if (request.getType().contains("confirmation") && request.getGroupId().equals(Long.valueOf(groupId))) {
            log.info("success");
            return confirmationCode;
        }

        log.info("not success");
        return "callback was not recognized";
    }
}
