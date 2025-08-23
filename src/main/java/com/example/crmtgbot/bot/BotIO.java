package com.example.crmtgbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
public class BotIO {

    private final TelegramClient client;

    @SneakyThrows
    public void send(Long chatId, String text, InlineKeyboardMarkup kb) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(kb)
                .build();
        client.execute(sm);
    }

    @SneakyThrows
    public void edit(Long chatId, Integer messageId, String text, InlineKeyboardMarkup kb) {
        EditMessageText em = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(kb)
                .build();
        client.execute(em);
    }

    public void answerCallback(String id, String text, boolean alert) {
        try {
            AnswerCallbackQuery ans = AnswerCallbackQuery.builder()
                    .callbackQueryId(id)
                    .text(text)
                    .showAlert(alert)
                    .build();
            client.execute(ans);
        } catch (TelegramApiException ignored) {}
    }
}
