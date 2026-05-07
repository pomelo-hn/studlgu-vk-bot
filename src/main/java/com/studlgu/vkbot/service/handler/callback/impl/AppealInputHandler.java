package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.CallbackAttachment;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
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

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component("appealInputHandler")
@RequiredArgsConstructor
public class AppealInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final AppealService appealService;
    private final UserStateCache userStateCache;
    private final AppealAnswerDraftCache appealAnswerDraftCache;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String text = Optional.ofNullable(request.getObject().getMessage().getText()).orElse("");
        UserState state = userStateCache.getState(userActor.getId()).orElse(null);

        try {
            if (state == UserState.AWAITING_APPEAL_TEXT) {
                handleAppealText(userActor, request, text);
            } else if (state == UserState.AWAITING_APPEAL_ANSWER) {
                handleAppealAnswer(userActor, text);
            } else {
                userStateCache.clearState(userActor.getId());
                sendMessage(userActor, userActor.getId(), "Сценарий обращения не найден.",
                        StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
            }
            return "ok";
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleAppealText(UserActor userActor, CallbackRequest request, String text)
            throws ApiException, ClientException {
        if (text.isBlank()) {
            sendMessage(userActor, userActor.getId(), "Текст обращения обязателен. Отправьте текст обращения.",
                    StandardKeyboard.createCancelKeyboard());
            return;
        }

        appealService.createAppeal(userActor.getId(), text.trim(), extractPhotoUrls(request));
        userStateCache.clearState(userActor.getId());
        sendMessage(userActor, userActor.getId(), "Обращение принято.",
                StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
    }

    private void handleAppealAnswer(UserActor userActor, String text) throws ApiException, ClientException {
        if (text.isBlank()) {
            sendMessage(userActor, userActor.getId(), "Текст ответа обязателен. Отправьте текст ответа.",
                    StandardKeyboard.createCancelKeyboard());
            return;
        }

        Optional<String> appealId = appealAnswerDraftCache.getAppealId(userActor.getId());
        if (appealId.isEmpty()) {
            userStateCache.clearState(userActor.getId());
            sendMessage(userActor, userActor.getId(), "Выбранное обращение не найдено.",
                    StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
            return;
        }

        Appeal appeal = appealService.findOpenById(appealId.get()).orElse(null);
        if (appeal == null) {
            userStateCache.clearState(userActor.getId());
            appealAnswerDraftCache.clearAppealId(userActor.getId());
            sendMessage(userActor, userActor.getId(), "Обращение не найдено или уже закрыто.",
                    StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
            return;
        }

        String answer = text.trim();
        sendMessage(userActor, appeal.getUserId(), "Ответ на ваше обращение:\n\n" + answer, null);
        appealService.answerAppeal(appeal.getId(), userActor.getId(), answer);
        userStateCache.clearState(userActor.getId());
        appealAnswerDraftCache.clearAppealId(userActor.getId());
        sendMessage(userActor, userActor.getId(), "Ответ отправлен пользователю.",
                StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
    }

    private List<String> extractPhotoUrls(CallbackRequest request) {
        List<CallbackAttachment> attachments = Optional.ofNullable(request.getObject().getMessage().getAttachments())
                .orElse(List.of());
        return attachments.stream()
                .filter(attachment -> "photo".equals(attachment.getType()))
                .map(CallbackAttachment::getPhoto)
                .filter(photo -> photo != null && photo.getOrigPhoto() != null)
                .map(photo -> photo.getOrigPhoto().getUrl())
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private void sendMessage(UserActor userActor, Long recipientUserId, String message, Keyboard keyboard)
            throws ApiException, ClientException {
        var sendRequest = vkApiClient.messages().sendDeprecated(userActor)
                .message(message)
                .userId(recipientUserId)
                .randomId(Math.abs(new Random().nextInt(10000)));
        if (keyboard != null) {
            sendRequest.keyboard(keyboard);
        }
        sendRequest.execute();
    }
}
