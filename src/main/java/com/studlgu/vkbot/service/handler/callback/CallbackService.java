package com.studlgu.vkbot.service.handler.callback;

import com.studlgu.vkbot.model.CallbackAttachment;
import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallbackService {

    private final ApplicationContext applicationContext;
    private final UserStateCache userStateCache;

    public String handle(CallbackRequest request) {
        Optional<CallbackType> callbackType = CallbackType.findByType(defineType(request));
        if (callbackType.isPresent()) {
            ICallbackHandler handler = applicationContext.getBean(callbackType.get().getHandlerName(), ICallbackHandler.class);
            return handler.handle(request);
        }
        return "callback was not recognized";
    }

    public String defineType(CallbackRequest request) {
        if ("message_new".equals(request.getType())) {
            Long userId = Optional.ofNullable(request.getObject())
                    .map(CallbackObject::getMessage)
                    .map(CallbackMessage::getFromId)
                    .orElse(null);

            if (userId != null) {
                String command = Optional.ofNullable(request.getObject())
                        .map(CallbackObject::getMessage)
                        .map(CallbackMessage::getMappedPayload)
                        .map(payload -> payload.getCommand())
                        .orElse(null);
                if ("cancel".equalsIgnoreCase(command)) {
                    return request.getType();
                }

                Optional<UserState> state = userStateCache.getState(userId);
                if (state.isPresent()) {
                    return switch (state.get()) {
                        case AWAITING_ADD_EVENT,
                             AWAITING_EVENT_TITLE,
                             AWAITING_EVENT_DATE,
                             AWAITING_EVENT_TIME,
                             AWAITING_EVENT_DESCRIPTION,
                             AWAITING_EVENT_LOCATION -> "add_event_input";
                        case AWAITING_DELETE_ID -> "delete_event_input";
                        case AWAITING_APPEAL_TEXT,
                             AWAITING_APPEAL_ANSWER -> "appeal_input";
                        default -> request.getType();
                    };
                }
            }
        }

        List<CallbackAttachment> attachments = Optional.ofNullable(request.getObject())
                .map(CallbackObject::getMessage)
                .map(CallbackMessage::getAttachments)
                .orElse(null);

        if (attachments != null && !attachments.isEmpty()) {
            boolean isAllAttachmentsIsPhoto = attachments
                    .stream()
                    .allMatch(attachment -> "photo".equals(attachment.getType()));

            if (isAllAttachmentsIsPhoto) {
                return "upload_photo";
            }
        }

        return request.getType();
    }
}
