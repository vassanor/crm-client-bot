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
        private Long masterId;
        private Long serviceId;
        private Long slotId;
        private boolean needNameInput;
        private String tempName;
        private String navBack;
    }

    // SessionStore.MasterProfileState
    @Data
    public static class MasterProfileState {
        public enum Step { NAME_CHOICE, NAME_INPUT, ADDRESS_INPUT, ABOUT_INPUT, PREVIEW }
        public enum Mode { CREATE, EDIT_NAME, EDIT_ADDRESS, EDIT_ABOUT }   // <--- NEW
        private Step step;
        private Mode mode = Mode.CREATE;                                   // <--- NEW
        private String name;
        private String address;
        private String about;
        private boolean active;
    }


    private final Map<Long, BookingSessionState> booking = new HashMap<>();
    private final Map<Long, MasterProfileState> profile = new HashMap<>();

    public Optional<BookingSessionState> get(Long chatId){ return Optional.ofNullable(booking.get(chatId)); }
    public BookingSessionState ensure(Long chatId){ return booking.computeIfAbsent(chatId,k->new BookingSessionState()); }
    public void clear(Long chatId){ booking.remove(chatId); }

    // NEW:
    public MasterProfileState ensureProfile(Long chatId){
        return profile.computeIfAbsent(chatId, id -> {
            MasterProfileState s = new MasterProfileState();
            s.setStep(MasterProfileState.Step.NAME_CHOICE);
            s.setActive(true);
            return s;
        });
    }
    public Optional<MasterProfileState> getProfile(Long chatId){ return Optional.ofNullable(profile.get(chatId)); }
    public void clearProfile(Long chatId){ profile.remove(chatId); }
}
