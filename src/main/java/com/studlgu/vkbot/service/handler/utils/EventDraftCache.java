package com.studlgu.vkbot.service.handler.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventDraftCache {

    private final Map<Long, EventDraft> drafts = new ConcurrentHashMap<>();

    public EventDraft createDraft(Long userId) {
        EventDraft draft = new EventDraft();
        drafts.put(userId, draft);
        return draft;
    }

    public Optional<EventDraft> getDraft(Long userId) {
        return Optional.ofNullable(drafts.get(userId));
    }

    public void clearDraft(Long userId) {
        drafts.remove(userId);
    }
}
