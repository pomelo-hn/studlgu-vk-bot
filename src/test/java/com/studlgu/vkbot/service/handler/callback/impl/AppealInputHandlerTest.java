package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.AppealStatus;
import com.studlgu.vkbot.model.CallbackAttachment;
import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackOrigPhoto;
import com.studlgu.vkbot.model.CallbackPhoto;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.utils.AppealAnswerDraftCache;
import com.studlgu.vkbot.service.handler.utils.AppealService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppealInputHandlerTest {

    @Test
    void userAppealTextIsRequired() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_APPEAL_TEXT);

        ctx.handler.handle(messageRequest(ctx.userId, " ", List.of()));

        verify(ctx.appealService, never()).createAppeal(anyLong(), anyString(), any());
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_APPEAL_TEXT);
    }

    @Test
    void createsAppealWithPhotoUrlsAndClearsState() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_APPEAL_TEXT);

        ctx.handler.handle(messageRequest(ctx.userId, "Need help", List.of(photoAttachment("https://vk/photo.jpg"))));

        verify(ctx.appealService).createAppeal(ctx.userId, "Need help", List.of("https://vk/photo.jpg"));
        assertThat(ctx.userStateCache.getState(ctx.userId)).isEmpty();
    }

    @Test
    void editorAnswerTextIsRequired() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_APPEAL_ANSWER);
        ctx.appealAnswerDraftCache.setAppealId(ctx.userId, "appeal-1");

        ctx.handler.handle(messageRequest(ctx.userId, " ", List.of()));

        verify(ctx.appealService, never()).answerAppeal(anyString(), anyLong(), anyString());
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_APPEAL_ANSWER);
        assertThat(ctx.appealAnswerDraftCache.getAppealId(ctx.userId)).contains("appeal-1");
    }

    @Test
    void missingAnswerDraftClearsAnswerState() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_APPEAL_ANSWER);

        ctx.handler.handle(messageRequest(ctx.userId, "Done", List.of()));

        verify(ctx.appealService, never()).answerAppeal(anyString(), anyLong(), anyString());
        assertThat(ctx.userStateCache.getState(ctx.userId)).isEmpty();
    }

    @Test
    void sendsAnswerToAppealAuthorThenMarksAppealAnsweredAndClearsDraft() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_APPEAL_ANSWER);
        ctx.appealAnswerDraftCache.setAppealId(ctx.userId, "appeal-1");
        when(ctx.appealService.findOpenById("appeal-1")).thenReturn(Optional.of(appeal("appeal-1", 456L)));

        ctx.handler.handle(messageRequest(ctx.userId, " Done ", List.of()));

        verify(ctx.appealService).findOpenById("appeal-1");
        verify(ctx.appealService).answerAppeal("appeal-1", ctx.userId, "Done");
        assertThat(ctx.userStateCache.getState(ctx.userId)).isEmpty();
        assertThat(ctx.appealAnswerDraftCache.getAppealId(ctx.userId)).isEmpty();
    }

    private static CallbackRequest messageRequest(long userId, String text, List<CallbackAttachment> attachments) {
        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        message.setText(text);
        message.setAttachments(attachments);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }

    private static CallbackAttachment photoAttachment(String url) {
        CallbackOrigPhoto origPhoto = new CallbackOrigPhoto();
        origPhoto.setUrl(url);
        CallbackPhoto photo = new CallbackPhoto();
        photo.setOrigPhoto(origPhoto);
        CallbackAttachment attachment = new CallbackAttachment();
        attachment.setType("photo");
        attachment.setPhoto(photo);
        return attachment;
    }

    private static Appeal appeal(String id, long authorUserId) {
        Appeal appeal = new Appeal();
        appeal.setId(id);
        appeal.setUserId(authorUserId);
        appeal.setText("Need help");
        appeal.setPhotoUrls(List.of());
        appeal.setStatus(AppealStatus.OPEN);
        appeal.setCreatedAt(Instant.parse("2026-05-07T10:00:00Z"));
        return appeal;
    }

    private static class TestContext {
        private final long userId = 123L;
        private final VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        private final VkActorFactory actorFactory = mock(VkActorFactory.class);
        private final RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        private final AppealService appealService = mock(AppealService.class);
        private final UserStateCache userStateCache = new UserStateCache();
        private final AppealAnswerDraftCache appealAnswerDraftCache = new AppealAnswerDraftCache();
        private final AppealInputHandler handler;

        private TestContext() {
            UserActor userActor = new UserActor(userId, "token");
            when(actorFactory.create(userId)).thenReturn(userActor);
            try {
                when(roleIdentifier.hasEditorRights(vkApiClient, userActor)).thenReturn(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            handler = new AppealInputHandler(
                    vkApiClient,
                    actorFactory,
                    roleIdentifier,
                    appealService,
                    userStateCache,
                    appealAnswerDraftCache
            );
        }
    }
}
