package com.studlgu.vkbot.service.handler.callback;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CallbackServiceTest {

    @Test
    void defineTypeRoutesCancelCommandBeforeAwaitingPhotoState() {
        UserStateCache userStateCache = new UserStateCache();
        CallbackService callbackService = new CallbackService(mock(ApplicationContext.class), userStateCache);
        long userId = 123L;
        userStateCache.setState(userId, UserState.AWAITING_PHOTO);

        CallbackRequest request = messageNewRequest(userId, "cancel");

        assertThat(callbackService.defineType(request)).isEqualTo("message_new");
    }

    private CallbackRequest messageNewRequest(long userId, String command) {
        Payload payload = new Payload();
        payload.setCommand(command);

        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        message.setMappedPayload(payload);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }
}
