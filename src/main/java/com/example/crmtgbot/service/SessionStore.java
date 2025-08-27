package com.example.crmtgbot.service;

import com.example.crmtgbot.handler.ServiceItemHandler;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private final Map<Long, BookingSessionState> booking = new HashMap<>();
    private final Map<Long, MasterProfileState> profile = new HashMap<>();
    private final Map<Long, ServiceSession> serviceSessions = new ConcurrentHashMap<>();

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

        public enum Step {NAME_CHOICE, NAME_INPUT, ADDRESS_INPUT, ABOUT_INPUT, PREVIEW}
        public enum Mode {CREATE, EDIT_NAME, EDIT_ADDRESS, EDIT_ABOUT}   // <--- NEW
        private Step step;
        private Mode mode = Mode.CREATE;                                   // <--- NEW
        private String name;
        private String address;
        private String about;
        private boolean active;

    }

    public Optional<BookingSessionState> get(Long chatId) {
        return Optional.ofNullable(booking.get(chatId));
    }

    public BookingSessionState ensure(Long chatId) {
        return booking.computeIfAbsent(chatId, k -> new BookingSessionState());
    }
    public void clear(Long chatId) {
        booking.remove(chatId);
    }
    // NEW:

    public MasterProfileState ensureProfile(Long chatId) {
        return profile.computeIfAbsent(chatId, id -> {
            MasterProfileState s = new MasterProfileState();
            s.setStep(MasterProfileState.Step.NAME_CHOICE);
            s.setActive(true);
            return s;
        });
    }

    public Optional<MasterProfileState> getProfile(Long chatId) {
        return Optional.ofNullable(profile.get(chatId));
    }

    public void clearProfile(Long chatId) {
        profile.remove(chatId);
    }

    public enum ServiceMode { CREATE, EDIT }
    public enum ServiceEditField { NONE, NAME, DESCRIPTION, ADDRESS, PRICE, DURATION }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ServiceSession {
        private ServiceMode mode;
        private ServiceItemHandler.Step step;   // для CREATE
        private ServiceEditField editField;     // для EDIT
        private Long editingItemId;             // для EDIT
        private com.example.crmtgbot.model.ServiceItem item; // draft для CREATE
    }


    public void startCreateService(Long chatId) {
        serviceSessions.put(chatId, new ServiceSession(ServiceMode.CREATE, ServiceItemHandler.Step.NAME,
                ServiceEditField.NONE, null, new com.example.crmtgbot.model.ServiceItem()));
    }
    public void startEditService(Long chatId, Long itemId) {
        serviceSessions.put(chatId, new ServiceSession(ServiceMode.EDIT, null,
                ServiceEditField.NONE, itemId, null));
    }
    public SessionStore.ServiceSession getServiceSession(Long chatId) {
        return serviceSessions.get(chatId);
    }
    public ServiceItemHandler.Step getServiceStep(Long chatId) {
        var s = serviceSessions.get(chatId);
        return s == null ? null : s.getStep();
    }
    public void setServiceStep(Long chatId, ServiceItemHandler.Step step,
                               com.example.crmtgbot.model.ServiceItem item) {
        var s = serviceSessions.get(chatId);
        if (s == null) s = new ServiceSession(ServiceMode.CREATE, step, ServiceEditField.NONE, null, item);
        else { s.setStep(step); s.setItem(item); s.setMode(ServiceMode.CREATE); }
        serviceSessions.put(chatId, s);
    }
    public void setServiceEditField(Long chatId, ServiceEditField field) {
        var s = serviceSessions.get(chatId);
        if (s == null) return;
        s.setMode(ServiceMode.EDIT);
        s.setEditField(field);
    }
    public void clearServiceSession(Long chatId) { serviceSessions.remove(chatId); }

    @Data
    public static class CalendarSession {
        // внутри SessionStore.CalendarSession
        public enum Step {
            NONE, PICK_SERVICE, ASK_START, ASK_END, OVERLAP,
            BULK_WEEKS, BULK_WEEKDAYS, BULK_OVERLAP, BULK_CONFIRM
        }

        private boolean bulk;                    // включен ли bulk-режим
        private Integer weeks;                   // 1/2/4
        private java.util.EnumSet<java.time.DayOfWeek> weekdays; // выбранные дни

        private LocalDate weekStart;      // понедельник текущего экрана
        private LocalDate targetDay;      // выбранный день
        private Long serviceId;           // выбранная услуга для генерации
        private String startStr;          // сырой ввод
        private String endStr;            // сырой ввод
        private Step step = Step.NONE;
        private boolean active;
        private Boolean bulkReplace;

    }
    private final Map<Long, CalendarSession> calendar = new ConcurrentHashMap<>();

    public CalendarSession ensureCalendar(Long chatId) {
        return calendar.computeIfAbsent(chatId, id -> {
            CalendarSession cs = new CalendarSession();
            cs.setWeekStart(weekStart(LocalDate.now())); // понедельник текущей недели
            cs.setActive(false);

            // bulk defaults
            cs.setBulk(false);
            cs.setWeeks(null);
            cs.setWeekdays(EnumSet.noneOf(DayOfWeek.class));
            cs.setServiceId(null);
            cs.setStartStr(null);
            cs.setEndStr(null);
            cs.setBulkReplace(null);
            cs.setStep(CalendarSession.Step.NONE);

            return cs;
        });
    }

    public Optional<CalendarSession> getCalendar(Long chatId) { return Optional.ofNullable(calendar.get(chatId)); }
    public void clearCalendar(Long chatId) { calendar.remove(chatId); }

    private LocalDate weekStart(LocalDate any) {
        return any.with(java.time.DayOfWeek.MONDAY);
    }

}
