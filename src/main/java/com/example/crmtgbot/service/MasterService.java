package com.example.crmtgbot.service;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.repo.MasterRepository;
import com.example.crmtgbot.repo.ServiceItemRepository;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterService {

    private final MasterRepository masterRepo;
    private final ServiceItemRepository serviceRepo;
    private final TimeSlotRepository slotRepo;

    public Master findById(Long id) {
        return masterRepo.findById(id).orElseThrow();
    }

    public List<ServiceItem> getServices(Master m) {
        return serviceRepo.findByMaster(m);
    }

    @Transactional
    public ServiceItem addService(Master m, String name, int duration) {
        return serviceRepo.save(ServiceItem.builder().name(name).durationMinutes(duration).master(m).build());
    }

    @Transactional
    public void generateDailyTemplate(Master m, ServiceItem s, LocalDate day, LocalTime from, LocalTime to) {
        LocalDateTime t = LocalDateTime.of(day, from);
        while (!t.plusMinutes(s.getDurationMinutes()).isAfter(LocalDateTime.of(day, to))) {
            slotRepo.save(TimeSlot.builder().master(m).service(s).startTime(t).endTime(t.plusMinutes(s.getDurationMinutes())).booked(false).build());
            t = t.plusMinutes(s.getDurationMinutes());
        }
    }

    public String deepLink(String botUsername, Master m) {
        return "https://t.me/" + botUsername + "?start=book_" + m.getId();
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
}
