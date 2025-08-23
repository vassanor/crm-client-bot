package com.example.crmtgbot;

import com.example.crmtgbot.handler.UpdateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateRouter {
    private final List<UpdateHandler> handlers;

    public void route(Update u) {
        for (UpdateHandler h : handlers) {
            if (h.canHandle(u)) {
                h.handle(u);
                return;
            }
        }
    }
}
