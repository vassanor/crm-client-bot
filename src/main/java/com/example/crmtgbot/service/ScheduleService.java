package com.example.crmtgbot.service;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final TimeSlotRepository slotRepo;

    public List<TimeSlot> available(Master m, ServiceItem s, LocalDate day) {
        return slotRepo.findByMasterAndServiceAndBookedFalseAndStartTimeBetween(m, s, day.atStartOfDay(), day.atTime(LocalTime.MAX));
    }

    public long countDaySlots(Long masterId, LocalDate day) {
        if (day == null) return 0; // <= страховка от случайных вызовов
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        return slotRepo.countByMasterIdAndStartTimeBetween(masterId, startOfDay, endOfDay);
    }


    public List<TimeSlot> listDaySlots(Long masterId, LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        return slotRepo.findByMasterIdAndStartTimeBetweenOrderByStartTime(masterId, startOfDay, endOfDay);
    }

    @Transactional
    public void clearDay(Long masterId, LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        slotRepo.deleteByMasterIdAndStartTimeBetween(masterId, startOfDay, endOfDay);
    }

    /**
     * Генерация слотов по услуге.
     */
    @Transactional
    public int generateSlots(Master master, ServiceItem service, LocalDate day,
                             LocalTime start, LocalTime end, boolean replace) {
        if (replace) clearDay(master.getId(), day);
        int dur = service.getDuration();
        int created = 0;
        LocalTime t = start;
        while (!t.plusMinutes(dur).isAfter(end)) {
            TimeSlot s = new TimeSlot();
            s.setMaster(master);
            s.setService(service);
            s.setStartTime(LocalDateTime.of(day, t));
            s.setEndTime(LocalDateTime.of(day, t.plusMinutes(dur)));
            s.setBooked(false);
            slotRepo.save(s);
            created++;
            t = t.plusMinutes(dur);
        }
        return created;
    }

    @Transactional
    public int bulkGenerateSlots(Master master, ServiceItem service, LocalDate weekStart,
                                 int weeks, java.util.EnumSet<DayOfWeek> days,
                                 LocalTime start, LocalTime end, boolean replace) {
        int total = 0;
        for (int w = 0; w < weeks; w++) {
            LocalDate base = weekStart.plusWeeks(w);
            for (DayOfWeek dow : days) {
                LocalDate day = base.with(dow);
                if (replace) clearDay(master.getId(), day);
                int dur = service.getDuration();
                LocalTime t = start;
                while (!t.plusMinutes(dur).isAfter(end)) {
                    TimeSlot s = new TimeSlot();
                    s.setMaster(master);
                    s.setService(service);
                    s.setStartTime(LocalDateTime.of(day, t));
                    s.setEndTime(LocalDateTime.of(day, t.plusMinutes(dur)));
                    s.setBooked(false);
                    slotRepo.save(s);
                    total++;
                    t = t.plusMinutes(dur);
                }
            }
        }
        return total;
    }

    @Transactional
    public int bulkClearDays(Long masterId, LocalDate weekStart, int weeks,
                             java.util.EnumSet<java.time.DayOfWeek> days) {
        int cleared = 0;
        for (int w = 0; w < weeks; w++) {
            LocalDate base = weekStart.plusWeeks(w);
            for (java.time.DayOfWeek d : days) {
                LocalDate day = base.with(d);
                long before = countDaySlots(masterId, day);
                clearDay(masterId, day);
                cleared += (int) before;
            }
        }
        return cleared;
    }

    public boolean hasAnySlots(Long masterId) {
        return slotRepo.countByMasterId(masterId) > 0;
    }


}
