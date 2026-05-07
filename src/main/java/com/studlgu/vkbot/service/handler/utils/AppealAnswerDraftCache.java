package com.studlgu.vkbot.service.handler.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppealAnswerDraftCache {

    private final Map<Long, String> selectedAppeals = new ConcurrentHashMap<>();

    public void setAppealId(Long userId, String appealId) {
        selectedAppeals.put(userId, appealId);
    }

    public Optional<String> getAppealId(Long userId) {
        return Optional.ofNullable(selectedAppeals.get(userId));
    }

    public void clearAppealId(Long userId) {
        selectedAppeals.remove(userId);
    }
}
