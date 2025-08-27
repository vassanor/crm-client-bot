package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    long countByMasterIdAndStartTimeBetween(Long masterId, LocalDateTime start, LocalDateTime end);
    List<TimeSlot> findByMasterIdAndStartTimeBetweenOrderByStartTime(Long masterId, LocalDateTime start, LocalDateTime end);
    void deleteByMasterIdAndStartTimeBetween(Long masterId, LocalDateTime start, LocalDateTime end);
    List<TimeSlot> findByMasterAndServiceAndBookedFalseAndStartTimeBetween(Master m, ServiceItem s, LocalDateTime from, LocalDateTime to);
    long countByMasterId(Long masterId);
}