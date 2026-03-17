package com.studlgu.vkbot.service.handler.command;

import com.studlgu.vkbot.model.CallbackRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandHandlerService {

    private final Map<CommandType, CommandHandler> handlers = new HashMap<>();

    public CommandHandlerService(List<CommandHandler> handlerList) {
        handlerList.forEach(handler ->
                handlers.put(handler.getType(), handler));
    }

    public void handle(CommandType type, CallbackRequest request) {
        CommandHandler handler = handlers.get(type);
        if (handler == null) {
            throw new UnsupportedOperationException("No handler for: " + type);
        }
        handler.handle(request);
    }
}
