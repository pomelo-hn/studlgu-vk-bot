package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
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

import java.util.Random;

@Component
@RequiredArgsConstructor
public class UploadMenuCommandHandler implements CommandHandler {

	private final VkApiClient vkApiClient;
	private final VkActorFactory actorFactory;
	private final UserStateCache userStateCache;

	@Override
	public CommandType getType() {
		return CommandType.UPLOAD_MENU;
	}

	@Override
	public void handle(CallbackRequest request) {
		UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());

		try {
			int randomId = Math.abs(new Random().nextInt(10000));
			vkApiClient
					.messages()
					.sendDeprecated(userActor)
					.message("Отправьте фото меню и описание.\n" +
							"Для отмены напишите cancel")
					.userId(userActor.getId())
					.randomId(randomId)
					.execute();

			userStateCache.setWaitingPhoto(userActor.getId());
		} catch (ApiException | ClientException e) {
			throw new RuntimeException(e);
		}
	}
}
