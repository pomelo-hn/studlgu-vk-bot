package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.service.handler.utils.PhotoStorage;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetMenuCommandHandlerTest {

    @Test
    void getMenuReturnsEmptyAttachmentStringWhenNoMenuPhotosExist() {
        PhotoStorage photoStorage = mock(PhotoStorage.class);
        when(photoStorage.getAllPhotos()).thenReturn(List.of());
        GetMenuCommandHandler handler = new GetMenuCommandHandler(
                mock(VkApiClient.class, RETURNS_DEEP_STUBS),
                mock(VkActorFactory.class),
                mock(RoleIdentifier.class),
                photoStorage
        );

        String attachments = ReflectionTestUtils.invokeMethod(
                handler,
                "getMenu",
                mock(GetMessagesUploadServerResponse.class),
                new UserActor(123L, "token")
        );

        assertThat(attachments).isEmpty();
    }
}
