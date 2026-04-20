package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.PhotoStorage;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class UploadPhotoHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final UserStateCache userStateCache;
    private final PhotoStorage photoStorage;
	private final RoleIdentifier roleIdentifier;
	private final RestClient restClient = RestClient.create();

    @Override
    public String handle(CallbackRequest request) {
	    try {
		    UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
		    boolean isServerWaitingPhoto = userStateCache.isWaitingPhoto(userActor.getId());

		    if (!isServerWaitingPhoto) {
                sendDeclineMessage(userActor);
                return "ok";
            }

		    List<byte[]> photoList = request.getObject().getMessage().getAttachments()
				    .stream()
				    .map(attachment -> attachment.getPhoto().getOrigPhoto().getUrl())
				    .map(this::getPhoto)
				    .toList();

			photoStorage.savePhotos(photoList);

			userStateCache.clearWaitingPhoto(userActor.getId());
		    sendSuccessMessage(userActor);

		    return "ok";
	    } catch (ApiException e) {
		    throw new RuntimeException(e);
	    } catch (ClientException e) {
		    throw new RuntimeException(e);
	    } catch (IOException e) {
		    throw new RuntimeException(e);
	    }
    }

	private byte[] getPhoto(String photoUrl) {
		return restClient.get()
				.uri(photoUrl)
				.retrieve()
				.body(byte[].class);
	}

	private void sendDeclineMessage(UserActor userActor) throws ApiException, ClientException {
        vkApiClient
                .messages()
                .sendDeprecated(userActor)
                .message("Не удалось загрузить фото!")
                .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                .userId(userActor.getId())
                .randomId(Math.abs(new Random().nextInt(10000)))
                .execute();
    }

	private void sendSuccessMessage(UserActor userActor) throws ApiException, ClientException {
		vkApiClient
				.messages()
				.sendDeprecated(userActor)
				.message("Фото успешно прикреплено!")
				.keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
				.userId(userActor.getId())
				.randomId(Math.abs(new Random().nextInt(10000)))
				.execute();
	}
}
