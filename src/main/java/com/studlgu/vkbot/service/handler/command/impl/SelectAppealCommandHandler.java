package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.AppealAnswerDraftCache;
import com.studlgu.vkbot.service.handler.utils.AppealService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SelectAppealCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final AppealService appealService;
    private final UserStateCache userStateCache;
    private final AppealAnswerDraftCache appealAnswerDraftCache;

    @Override
    public CommandType getType() {
        return CommandType.SELECT_APPEAL;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            boolean hasEditorRights = roleIdentifier.hasEditorRights(vkApiClient, userActor);
            if (!hasEditorRights) {
                sendMessage(userActor, "У вас нет прав для ответа на обращения.",
                        StandardKeyboard.createkeyboard(false));
                return;
            }

            String appealId = request.getObject().getMessage().getMappedPayload().getAppealId();
            Appeal appeal = appealService.findOpenById(appealId).orElse(null);
            if (appeal == null) {
                sendCurrentOpenAppeals(userActor, "Обращение не найдено или уже закрыто.");
                return;
            }

            appealAnswerDraftCache.setAppealId(userActor.getId(), appeal.getId());
            userStateCache.setState(userActor.getId(), UserState.AWAITING_APPEAL_ANSWER);
            sendMessage(userActor, formatAppeal(appeal) + "\n\nОтправьте текст ответа.",
                    StandardKeyboard.createCancelKeyboard(),
                    photoAttachments(appeal));
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatAppeal(Appeal appeal) {
        StringBuilder message = new StringBuilder()
                .append("Обращение ID: ")
                .append(shortId(appeal.getId()))
                .append("\nАвтор VK ID: ")
                .append(appeal.getUserId())
                .append("\nСоздано: ")
                .append(appeal.getCreatedAt())
                .append("\n\n")
                .append(appeal.getText());

        if ((appeal.getPhotoAttachments() == null || appeal.getPhotoAttachments().isEmpty())
                && appeal.getPhotoUrls() != null && !appeal.getPhotoUrls().isEmpty()) {
            message.append("\n\nКартинки:");
            for (String photoUrl : appeal.getPhotoUrls()) {
                message.append("\n").append(photoUrl);
            }
        }
        return message.toString();
    }

    private void sendCurrentOpenAppeals(UserActor userActor, String prefix) throws ApiException, ClientException {
        List<Appeal> appeals = appealService.listOpen();
        if (appeals.isEmpty()) {
            sendMessage(userActor, prefix + "\n\nОткрытых обращений нет.",
                    StandardKeyboard.createkeyboard(true));
            return;
        }
        sendMessage(userActor, prefix + "\n\nВыберите обращение из актуального списка.",
                StandardKeyboard.createAppealsListKeyboard(appeals.stream()
                        .map(Appeal::getId)
                        .toList()));
    }

    private void sendMessage(UserActor userActor, String message, Keyboard keyboard)
            throws ApiException, ClientException {
        sendMessage(userActor, message, keyboard, null);
    }

    private void sendMessage(UserActor userActor, String message, Keyboard keyboard, String attachment)
            throws ApiException, ClientException {
        var sendRequest = vkApiClient.messages().sendDeprecated(userActor)
                .message(message);
        if (attachment != null && !attachment.isBlank()) {
            sendRequest.attachment(attachment);
        }
        sendRequest
                .keyboard(keyboard)
                .userId(userActor.getId())
                .randomId(Math.abs(new Random().nextInt(10000)));
        sendRequest.execute();
    }

    private String shortId(String id) {
        if (id == null) {
            return "";
        }
        return id.substring(0, Math.min(8, id.length()));
    }

    private String photoAttachments(Appeal appeal) {
        if (appeal.getPhotoAttachments() == null || appeal.getPhotoAttachments().isEmpty()) {
            return null;
        }
        return String.join(",", appeal.getPhotoAttachments());
    }
}
