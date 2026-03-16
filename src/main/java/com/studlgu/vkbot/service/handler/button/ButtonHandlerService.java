package com.studlgu.vkbot.service.handler.button;

import com.studlgu.vkbot.model.CallbackRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ButtonHandlerService {

    private final Map<ButtonType, ButtonHandler> handlers = new HashMap<>();

    public ButtonHandlerService(List<ButtonHandler> handlerList) {
        handlerList.forEach(handler ->
                handlers.put(handler.getType(), handler));
    }

    public void handle(ButtonType type, CallbackRequest request) {
        ButtonHandler handler = handlers.get(type);
        if (handler == null) {
            throw new UnsupportedOperationException("No handler for: " + type);
        }
        handler.handle(request);
    }
}
