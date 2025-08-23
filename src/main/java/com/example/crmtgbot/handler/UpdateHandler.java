package com.example.crmtgbot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {

    boolean canHandle(Update u);

    void handle(Update u);
}