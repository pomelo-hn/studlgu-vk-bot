package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandlerService;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MessageNewHandler implements ICallbackHandler {

    private final CommandHandlerService buttonHandlerService;

    @Override
    public String handle(CallbackRequest request) {
        String command = Optional.ofNullable(request.getObject())
                .map(object -> object.getMessage())
                .map(message -> message.getMappedPayload())
                .map(payload -> payload.getCommand())
                .orElse(null);
        if (command == null || command.isBlank()) {
            return "ok";
        }

        String buttonType = command.toUpperCase();
        buttonHandlerService.handle(CommandType.valueOf(buttonType), request);

        return "ok";
    }
}
