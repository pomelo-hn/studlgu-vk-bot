package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.GetMembersFilter;
import com.vk.api.sdk.objects.groups.responses.GetMembersFieldsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoleIdentifier {

	@Value("${vkbot.group-id}")
	private String groupId;

	@Value("${vkbot.editor-roles}")
	private String editorRoles;

	public Boolean hasEditorRights(VkApiClient vkApiClient, UserActor userActor) throws ClientException, ApiException {
		GetMembersFieldsResponse members = vkApiClient.groups().getMembersWithFields(userActor)
				.groupId(groupId)
				.filter(GetMembersFilter.MANAGERS)
				.execute();

		return members.getItems()
				.stream()
				.filter(userIds -> userActor.getId().equals(userIds.getId()))
				.anyMatch(userIds -> editorRoles.contains(userIds.getRole().getValue()));
	}
}
