package com.example.crmtgbot.service;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.repo.MasterRepository;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MasterService {

    @Value("${bot.username:your_bot}")
    private String botUsername;
    private final MasterRepository masterRepo;
    private final TimeSlotRepository slotRepo;


    public Master findById(Long id) {
        return masterRepo.findById(id).orElseThrow();
    }

    @Transactional
    public void generateDailyTemplate(Master m, ServiceItem s, LocalDate day, LocalTime from, LocalTime to) {
        LocalDateTime t = LocalDateTime.of(day, from);
        while (!t.plusMinutes(s.getDuration()).isAfter(LocalDateTime.of(day, to))) {
            slotRepo.save(TimeSlot.builder().master(m).service(s).startTime(t).endTime(t.plusMinutes(s.getDuration())).booked(false).build());
            t = t.plusMinutes(s.getDuration());
        }
    }

    @Transactional
    public Master updateProfile(Long chatId, String name, String address, String about) {
        Master m = masterRepo.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Master not found for chatId=" + chatId));
        m.setDisplayName(name);
        m.setAddress(address);
        m.setAbout(about);
        // Возвращаем managed-объект; save не обязателен, но не вреден.
        return m;
    }

    public String deepLink(Master m) {
        // Короткий payload: m<ID>, чтобы точно уложиться в лимит Telegram (<=64 символов)
        return "https://t.me/" + botUsername + "?start=m" + m.getId();
    }

    // перегрузка, если нужно где-то подменять имя бота вручную
    public String deepLink(String botUsernameOverride, Master m) {
        String u = (botUsernameOverride == null || botUsernameOverride.isBlank()) ? botUsername : botUsernameOverride;
        return "https://t.me/" + u + "?start=m" + m.getId();
    }

    public Master toggleAuto(Master m) {
        m.setAutoConfirm(!m.isAutoConfirm());
        return masterRepo.save(m);
    }

    public Master getOrCreateMaster(Long chatId, Long userId, String username, String display){
        return masterRepo.findByChatId(chatId).orElseGet(() ->
                masterRepo.save(Master.builder()
                        .chatId(chatId)
                        .userId(userId)
                        .username(username)
                        .displayName(display)
                        .autoConfirm(true)
                        .build()));
    }

    public Master save(Master m) {
        return masterRepo.save(m);
    }

    public boolean isProfileComplete(Master m) { // NEW
        return m.getDisplayName() != null && !m.getDisplayName().isBlank()
                && m.getAddress() != null && !m.getAddress().isBlank()
                && m.getAbout() != null && !m.getAbout().isBlank();
    }

    public Master findByChatId(Long chatId) {
        return masterRepo.findByChatId(chatId).orElse(null);
    }

    public Optional<Master> findByIdOpt(Long id) {
        return masterRepo.findById(id);
    }
}
