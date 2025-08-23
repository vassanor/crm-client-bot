package com.example.crmtgbot.service;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final TimeSlotRepository slotRepo;

    public List<TimeSlot> available(Master m, ServiceItem s, LocalDate day) {
        return slotRepo.findByMasterAndServiceAndBookedFalseAndStartTimeBetween(m, s, day.atStartOfDay(), day.atTime(LocalTime.MAX));
    }
}
