package com.example.crmtgbot.service;

import com.example.crmtgbot.model.*;
import com.example.crmtgbot.repo.BookingRepository;
import com.example.crmtgbot.repo.ServiceItemRepository;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final TimeSlotRepository slotRepo;
    private final ServiceItemRepository serviceRepo;

    @Transactional
    public Booking create(Master master, Long slotId, Long serviceId, Long clientChatId, Integer clientUserId, String clientUsername, String display) {
        TimeSlot slot = slotRepo.findById(slotId).orElseThrow();
        ServiceItem service = serviceRepo.findById(serviceId).orElseThrow();
        if (slot.isBooked()) throw new IllegalStateException("Slot already booked");
        Booking b = Booking.builder().master(master).service(service).slot(slot).clientChatId(clientChatId).clientUserId(clientUserId).clientUsername(clientUsername).clientDisplayName(display).status(master.isAutoConfirm() ? BookingStatus.CONFIRMED : BookingStatus.PENDING).build();
        bookingRepo.save(b);
        if (master.isAutoConfirm()) {
            slot.setBooked(true);
            slotRepo.save(slot);
        }
        return b;
    }

    @Transactional
    public Booking approve(Long bookingId) {
        Booking b = bookingRepo.findWithDetailsById(bookingId).orElseThrow();
        // Если используете статус — проставьте его
        if (b.getStatus() != null) b.setStatus(BookingStatus.CONFIRMED);
        return b; // managed; flush сделает save
    }

    @Transactional
    public Booking reject(Long bookingId) {
        Booking b = bookingRepo.findWithDetailsById(bookingId).orElseThrow();
        // Освобождаем слот
        b.getSlot().setBooked(false);
        slotRepo.save(b.getSlot());
        if (b.getStatus() != null) b.setStatus(BookingStatus.REJECTED);
        return b; // запись можно оставить в БД с признаком REJECTED
        // Если хотите удалять запись целиком — замените на: bookingRepo.delete(b); return b;
    }

    @Transactional(readOnly = true)
    public List<Booking> upcoming(Long masterId, int limit) {
        var list = bookingRepo.upcomingWithDetails(masterId, java.time.LocalDateTime.now());
        return list.size() > limit ? list.subList(0, limit) : list;
    }

}
