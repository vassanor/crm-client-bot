package com.example.crmtgbot.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class SessionStore {

    @Data
    public static class BookingSessionState {
        private Long masterId, serviceId, slotId;
        private boolean needNameInput;
        private String tempName;
        private String navBack;
    }

    private final Map<Long, BookingSessionState> map = new HashMap<>();

    public Optional<BookingSessionState> get(Long chatId) {
        return Optional.ofNullable(map.get(chatId));
    }

    public BookingSessionState ensure(Long chatId) {
        return map.computeIfAbsent(chatId, k -> new BookingSessionState());
    }

    public void clear(Long chatId) {
        map.remove(chatId);
    }
}
