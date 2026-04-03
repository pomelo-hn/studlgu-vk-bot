package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadMenuCommandHandler implements CommandHandler {

	private final VkApiClient vkApiClient;
	private final VkActorFactory actorFactory;


	@Override
	public CommandType getType() {
		return CommandType.UPLOAD_MENU;
	}

	@Override
	public void handle(CallbackRequest request) {
		UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
	}
}
