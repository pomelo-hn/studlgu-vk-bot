package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.PhotoUploadResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class VkPhotoUploader {

    private final VkApiClient vkApiClient;

    public String uploadBytes(byte[] imageBytes, UserActor userActor)
            throws ApiException, ClientException, IOException {
        File tempFile = Files.createTempFile("vkbot_calendar_", ".png").toFile();
        try {
            Files.write(tempFile.toPath(), imageBytes);

            GetMessagesUploadServerResponse server = vkApiClient
                    .photos().getMessagesUploadServer(userActor).execute();

            PhotoUploadResponse uploaded = vkApiClient
                    .upload().photo(server.getUploadUrl().toURL().toString(), tempFile).execute();

            SaveMessagesPhotoResponse saved = vkApiClient
                    .photos().saveMessagesPhoto(userActor)
                    .photo(uploaded.getPhoto())
                    .server(uploaded.getServer())
                    .hash(uploaded.getHash())
                    .execute().getFirst();

            return "photo" + saved.getOwnerId() + "_" + saved.getId();
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }
}
