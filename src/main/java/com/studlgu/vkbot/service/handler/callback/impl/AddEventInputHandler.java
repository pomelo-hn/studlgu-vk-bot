package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.EventDraft;
import com.studlgu.vkbot.service.handler.utils.EventDraftCache;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Random;

@Component("addEventInputHandler")
@RequiredArgsConstructor
public class AddEventInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final UserStateCache userStateCache;
    private final EventDraftCache eventDraftCache;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String text = request.getObject().getMessage().getText();
        try {
            UserState state = userStateCache.getState(userActor.getId()).orElse(null);
            EventDraft draft = eventDraftCache.getDraft(userActor.getId()).orElse(null);
            if (state == null || draft == null) {
                userStateCache.clearState(userActor.getId());
                eventDraftCache.clearDraft(userActor.getId());
                sendMessage(userActor, "Добавление события не найдено. Нажми «Добавить событие» ещё раз.",
                        StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
                return "ok";
            }

            switch (state) {
                case AWAITING_EVENT_TITLE, AWAITING_ADD_EVENT -> handleTitle(userActor, draft, text);
                case AWAITING_EVENT_DATE -> handleDate(userActor, draft, text);
                case AWAITING_EVENT_TIME -> handleTime(userActor, draft, text);
                case AWAITING_EVENT_DESCRIPTION -> handleDescription(userActor, draft, text);
                case AWAITING_EVENT_LOCATION -> handleLocation(userActor, draft, text);
                default -> {
                    userStateCache.clearState(userActor.getId());
                    eventDraftCache.clearDraft(userActor.getId());
                    sendMessage(userActor, "Добавление события остановлено.",
                            StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
                }
            }
        } catch (IllegalArgumentException e) {
            try {
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("❌ Ошибка: " + e.getMessage())
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
            } catch (ApiException | ClientException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }

    private void handleTitle(UserActor userActor, EventDraft draft, String text) throws ApiException, ClientException {
        if (text == null || text.isBlank()) {
            sendMessage(userActor, "Название события обязательно. Отправь название события.",
                    StandardKeyboard.createCancelKeyboard());
            return;
        }

        draft.setTitle(text.trim());
        userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_DATE);
        sendMessage(userActor, "Отправь дату события в формате YYYY-MM-DD. Например: 2026-05-15",
                StandardKeyboard.createCancelKeyboard());
    }

    private void handleDate(UserActor userActor, EventDraft draft, String text) throws ApiException, ClientException {
        try {
            draft.setDate(LocalDate.parse(text.trim()));
            userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_TIME);
            sendMessage(userActor, "Отправь время в формате HH:mm или нажми «Пропустить».",
                    StandardKeyboard.createOptionalEventStepKeyboard());
        } catch (DateTimeParseException | NullPointerException e) {
            sendMessage(userActor, "Неверная дата. Отправь дату в формате YYYY-MM-DD. Например: 2026-05-15",
                    StandardKeyboard.createCancelKeyboard());
        }
    }

    private void handleTime(UserActor userActor, EventDraft draft, String text) throws ApiException, ClientException {
        if (isSkipped(text)) {
            draft.setTime(null);
            userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_DESCRIPTION);
            sendMessage(userActor, "Отправь описание события или нажми «Пропустить».",
                    StandardKeyboard.createOptionalEventStepKeyboard());
            return;
        }

        try {
            draft.setTime(LocalTime.parse(text.trim()));
            userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_DESCRIPTION);
            sendMessage(userActor, "Отправь описание события или нажми «Пропустить».",
                    StandardKeyboard.createOptionalEventStepKeyboard());
        } catch (DateTimeParseException | NullPointerException e) {
            sendMessage(userActor, "Неверное время. Отправь время в формате HH:mm или нажми «Пропустить».",
                    StandardKeyboard.createOptionalEventStepKeyboard());
        }
    }

    private void handleDescription(UserActor userActor, EventDraft draft, String text) throws ApiException, ClientException {
        draft.setDescription(isSkipped(text) ? null : text.trim());
        userStateCache.setState(userActor.getId(), UserState.AWAITING_EVENT_LOCATION);
        sendMessage(userActor, "Отправь место проведения или нажми «Пропустить».",
                StandardKeyboard.createOptionalEventStepKeyboard());
    }

    private void handleLocation(UserActor userActor, EventDraft draft, String text) throws ApiException, ClientException {
        draft.setLocation(isSkipped(text) ? null : text.trim());
        eventService.addEvent(
                draft.getTitle(),
                draft.getDate(),
                draft.getTime(),
                draft.getDescription(),
                draft.getLocation()
        );
        userStateCache.clearState(userActor.getId());
        eventDraftCache.clearDraft(userActor.getId());
        sendMessage(userActor, "✅ Событие добавлено!",
                StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)));
    }

    private boolean isSkipped(String text) {
        return text == null || text.isBlank() || text.toLowerCase().contains("пропустить");
    }

    private void sendMessage(UserActor userActor, String message, Keyboard keyboard) throws ApiException, ClientException {
        vkApiClient.messages().sendDeprecated(userActor)
                .message(message)
                .keyboard(keyboard)
                .userId(userActor.getId())
                .randomId(Math.abs(new Random().nextInt(10000)))
                .execute();
    }
}
