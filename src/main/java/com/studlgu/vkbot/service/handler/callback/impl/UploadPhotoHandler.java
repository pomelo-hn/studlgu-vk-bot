package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.PhotoStorage;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserState;
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
import java.util.Optional;
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
		    boolean isServerWaitingPhoto = userStateCache.getState(userActor.getId())
				    .map(s -> s == UserState.AWAITING_PHOTO)
				    .orElse(false);

		    if (!isServerWaitingPhoto) {
                sendDeclineMessage(userActor);
                return "ok";
            }

            var attachments = Optional.ofNullable(request.getObject().getMessage().getAttachments())
                    .orElse(List.of());
            if (attachments.isEmpty() || attachments.stream().anyMatch(attachment -> !"photo".equals(attachment.getType()))) {
                sendMessage(userActor, "Отправьте хотя бы одно фото меню.", StandardKeyboard.createCancelKeyboard());
                return "ok";
            }

		    List<byte[]> photoList = attachments
				    .stream()
				    .map(attachment -> attachment.getPhoto().getOrigPhoto().getUrl())
				    .map(this::getPhoto)
				    .toList();

			photoStorage.savePhotos(photoList);

			userStateCache.clearState(userActor.getId());
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
        sendMessage(userActor, "Не удалось загрузить фото!",
                StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
    }

	private void sendSuccessMessage(UserActor userActor) throws ApiException, ClientException {
        sendMessage(userActor, "Фото успешно прикреплено!",
                StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
	}

    private void sendMessage(UserActor userActor, String message, com.vk.api.sdk.objects.messages.Keyboard keyboard)
            throws ApiException, ClientException {
        vkApiClient
				.messages()
				.sendDeprecated(userActor)
				.message(message)
				.keyboard(keyboard)
				.userId(userActor.getId())
				.randomId(Math.abs(new Random().nextInt(10000)))
				.execute();
    }
}
