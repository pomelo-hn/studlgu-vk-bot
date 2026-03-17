package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandlerService;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor //TODO: deprecated
public class MessageEventHandler implements ICallbackHandler {

    private final CommandHandlerService buttonHandlerService;

    @Override
    public String handle(CallbackRequest request) {
        String buttonType = request.getObject().getPayload().getCommand().toUpperCase();
        buttonHandlerService.handle(CommandType.valueOf(buttonType), request);

        return "ok";
    }
}
