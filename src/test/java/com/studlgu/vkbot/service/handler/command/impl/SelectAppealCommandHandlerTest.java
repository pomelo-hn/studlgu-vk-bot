package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.AppealStatus;
import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.utils.AppealAnswerDraftCache;
import com.studlgu.vkbot.service.handler.utils.AppealService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelectAppealCommandHandlerTest {

    @Test
    void selectedAppealPhotosAreSentAsVkAttachments() throws Exception {
        long userId = 123L;
        UserActor userActor = new UserActor(userId, "token");
        VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        VkActorFactory actorFactory = mock(VkActorFactory.class);
        RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        AppealService appealService = mock(AppealService.class);
        Appeal appeal = appeal("appeal-1");
        when(actorFactory.create(userId)).thenReturn(userActor);
        when(roleIdentifier.hasEditorRights(vkApiClient, userActor)).thenReturn(true);
        when(appealService.findOpenById("appeal-1")).thenReturn(Optional.of(appeal));
        SelectAppealCommandHandler handler = new SelectAppealCommandHandler(
                vkApiClient,
                actorFactory,
                roleIdentifier,
                appealService,
                new UserStateCache(),
                new AppealAnswerDraftCache()
        );

        handler.handle(messageRequest(userId, "appeal-1"));

        verify(vkApiClient.messages().sendDeprecated(userActor).message(contains("Need help")))
                .attachment("photo-10_20_access-key");
    }

    private CallbackRequest messageRequest(long userId, String appealId) {
        Payload payload = new Payload();
        payload.setAppealId(appealId);
        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        message.setMappedPayload(payload);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }

    private Appeal appeal(String id) {
        Appeal appeal = new Appeal();
        appeal.setId(id);
        appeal.setUserId(456L);
        appeal.setText("Need help");
        appeal.setPhotoAttachments(List.of("photo-10_20_access-key"));
        appeal.setPhotoUrls(List.of("https://vk/photo.jpg"));
        appeal.setStatus(AppealStatus.OPEN);
        appeal.setCreatedAt(Instant.parse("2026-05-07T10:00:00Z"));
        return appeal;
    }
}
