package com.studlgu.vkbot.service.handler.callback;

import com.studlgu.vkbot.model.CallbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallbackService {

    private final ApplicationContext applicationContext;

    public String handle(CallbackRequest request) {
        Optional<CallbackType> callbackType = CallbackType.findByType(request.getType());
        if (callbackType.isPresent()) {

            ICallbackHandler handler = applicationContext.getBean(callbackType.get().getHandlerName(), ICallbackHandler.class);
            return handler.handle(request);
        }

        return "callback was not recognized";
    }
}
