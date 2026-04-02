package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
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
import java.net.MalformedURLException;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class GetMenuCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;

    @Override
    public CommandType getType() {
        return CommandType.GET_MENU;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            GetMessagesUploadServerResponse messagesUploadServer = vkApiClient.photos().getMessagesUploadServer(userActor).execute();

            File file = new File("D:\\java\\vk-bot\\src\\main\\resources\\photo_2026-03-13_09-17-06.jpg");

            PhotoUploadResponse photoUploadResponse = vkApiClient.upload()
                    .photo(messagesUploadServer.getUploadUrl().toURL().toString(), file).execute();

            SaveMessagesPhotoResponse saveMessagesPhotoResponse = vkApiClient.photos().saveMessagesPhoto(userActor)
                    .photo(photoUploadResponse.getPhoto())
                    .server(photoUploadResponse.getServer())
                    .hash(photoUploadResponse.getHash())
                    .execute().getFirst();

            String photo = "{photo}" + "{-" + saveMessagesPhotoResponse.getOwnerId() + "}_{" + saveMessagesPhotoResponse.getId() + "}";

            int randomId = Math.abs(new Random().nextInt(10000));
            vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message("\uD83C\uDF5B Меню столовой на 2026-03-13")
                    .attachment(photo)
                    .keyboard(StandardKeyboard.createkeyboard())
                    .userId(userActor.getId())
                    .randomId(randomId)
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
