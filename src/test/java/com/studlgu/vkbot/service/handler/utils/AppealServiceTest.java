package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.AppealStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppealServiceTest {

    @Test
    void createAppealTrimsTextAndSavesOpenAppealWithPhotoUrls() {
        AppealRepository repository = mock(AppealRepository.class);
        AppealService service = new AppealService(repository);

        Appeal appeal = service.createAppeal(123L, "  Help  ", List.of("https://vk/photo.jpg"));

        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.OPEN);
        assertThat(appeal.getUserId()).isEqualTo(123L);
        assertThat(appeal.getText()).isEqualTo("Help");
        assertThat(appeal.getPhotoUrls()).containsExactly("https://vk/photo.jpg");
        assertThat(appeal.getCreatedAt()).isNotNull();
        verify(repository).save(appeal);
    }

    @Test
    void createAppealRejectsBlankText() {
        AppealService service = new AppealService(mock(AppealRepository.class));

        assertThatThrownBy(() -> service.createAppeal(123L, " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Текст обращения");
    }

    @Test
    void listOpenAppealsReturnsOnlyOpenAppeals() {
        AppealRepository repository = mock(AppealRepository.class);
        AppealService service = new AppealService(repository);
        Appeal open = appeal("a1", AppealStatus.OPEN);
        Appeal answered = appeal("a2", AppealStatus.ANSWERED);
        when(repository.findAll()).thenReturn(List.of(answered, open));

        assertThat(service.listOpen()).containsExactly(open);
    }

    @Test
    void findOpenByIdReturnsEmptyForAnsweredAppeal() {
        AppealRepository repository = mock(AppealRepository.class);
        AppealService service = new AppealService(repository);
        Appeal answered = appeal("a1", AppealStatus.ANSWERED);
        when(repository.findAll()).thenReturn(List.of(answered));

        assertThat(service.findOpenById("a1")).isEmpty();
    }

    @Test
    void answerAppealMarksOpenAppealAnsweredAndSavesAllAppeals() {
        AppealRepository repository = mock(AppealRepository.class);
        AppealService service = new AppealService(repository);
        Appeal appeal = appeal("a1", AppealStatus.OPEN);
        when(repository.findAll()).thenReturn(List.of(appeal));

        Appeal answered = service.answerAppeal("a1", 999L, "  Done  ");

        assertThat(answered.getStatus()).isEqualTo(AppealStatus.ANSWERED);
        assertThat(answered.getAnswerText()).isEqualTo("Done");
        assertThat(answered.getAnsweredByUserId()).isEqualTo(999L);
        assertThat(answered.getAnsweredAt()).isNotNull();
        verify(repository).saveAll(List.of(answered));
    }

    @Test
    void answerAppealRejectsBlankAnswer() {
        AppealService service = new AppealService(mock(AppealRepository.class));

        assertThatThrownBy(() -> service.answerAppeal("a1", 999L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Текст ответа");
    }

    private Appeal appeal(String id, AppealStatus status) {
        Appeal appeal = new Appeal();
        appeal.setId(id);
        appeal.setUserId(123L);
        appeal.setText("Text");
        appeal.setPhotoUrls(List.of());
        appeal.setStatus(status);
        appeal.setCreatedAt(Instant.parse("2026-05-07T10:00:00Z"));
        return appeal;
    }
}
