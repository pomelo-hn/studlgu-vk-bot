package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.AppealStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppealService {

    private final AppealRepository repository;

    public Appeal createAppeal(Long userId, String text, List<String> photoUrls) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст обращения обязателен");
        }

        Appeal appeal = new Appeal(
                null,
                userId,
                text.trim(),
                List.of(),
                photoUrls == null ? List.of() : List.copyOf(photoUrls),
                AppealStatus.OPEN,
                Instant.now(),
                null,
                null,
                null
        );
        repository.save(appeal);
        return appeal;
    }

    public List<Appeal> listOpen() {
        return repository.findAll().stream()
                .filter(appeal -> appeal.getStatus() == AppealStatus.OPEN)
                .toList();
    }

    public Optional<Appeal> findOpenById(String id) {
        return repository.findAll().stream()
                .filter(appeal -> appeal.getStatus() == AppealStatus.OPEN)
                .filter(appeal -> appeal.getId() != null && appeal.getId().equals(id))
                .findFirst();
    }

    public Appeal answerAppeal(String appealId, Long editorUserId, String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Текст ответа обязателен");
        }

        List<Appeal> appeals = new ArrayList<>(repository.findAll());
        Appeal appeal = appeals.stream()
                .filter(candidate -> candidate.getStatus() == AppealStatus.OPEN)
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(appealId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Обращение не найдено"));

        appeal.setStatus(AppealStatus.ANSWERED);
        appeal.setAnsweredAt(Instant.now());
        appeal.setAnsweredByUserId(editorUserId);
        appeal.setAnswerText(answerText.trim());
        repository.saveAll(appeals);
        return appeal;
    }
}
