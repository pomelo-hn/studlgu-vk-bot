package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.PhotoStorage;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
public class GetMenuCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final PhotoStorage photoStorage;

    @Override
    public CommandType getType() {
        return CommandType.GET_MENU;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

        try {
            LocalDateTime dateTime = LocalDateTime.now();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            String date = dateTime.format(dateFormatter);
            List<File> menuPhotos = photoStorage.getAllPhotos();

            String message = "\uD83C\uDF5B Меню столовой на " + date;
            String attachments = "";
            if (menuPhotos.isEmpty()) {
                message = "Меню пока не загружено.";
            } else {
                GetMessagesUploadServerResponse messagesUploadServer = vkApiClient
                        .photos().getMessagesUploadServer(userActor).execute();
                attachments = getMenu(messagesUploadServer, userActor, menuPhotos);
            }

            int randomId = Math.abs(new Random().nextInt(10000));
            var sendRequest = vkApiClient
                    .messages()
                    .sendDeprecated(userActor)
                    .message(message)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(randomId);
            if (!attachments.isBlank()) {
                sendRequest.attachment(attachments);
            }
            sendRequest.execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMenu(GetMessagesUploadServerResponse messagesUploadServer, UserActor userActor) throws ApiException, ClientException, MalformedURLException {
        return getMenu(messagesUploadServer, userActor, photoStorage.getAllPhotos());
    }

    private String getMenu(GetMessagesUploadServerResponse messagesUploadServer, UserActor userActor, List<File> menuPhotos)
            throws ApiException, ClientException, MalformedURLException {
	    try {
            StringJoiner attachments = new StringJoiner(",");

            for (File photo : menuPhotos) {
                PhotoUploadResponse photoUploadResponse = vkApiClient.upload()
                        .photo(messagesUploadServer.getUploadUrl().toURL().toString(), photo).execute();

                SaveMessagesPhotoResponse saveMessagesPhotoResponse = vkApiClient.photos().saveMessagesPhoto(userActor)
                        .photo(photoUploadResponse.getPhoto())
                        .server(photoUploadResponse.getServer())
                        .hash(photoUploadResponse.getHash())
                        .execute().getFirst();

                attachments.add("photo" + saveMessagesPhotoResponse.getOwnerId() + "_" + saveMessagesPhotoResponse.getId());
            }

            return attachments.toString();
	    } catch (IOException e) {
		    throw new RuntimeException(e);
	    }
    }

}
