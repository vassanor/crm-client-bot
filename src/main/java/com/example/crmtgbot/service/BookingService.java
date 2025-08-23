package com.example.crmtgbot.service;

import com.example.crmtgbot.model.*;
import com.example.crmtgbot.repo.BookingRepository;
import com.example.crmtgbot.repo.ServiceItemRepository;
import com.example.crmtgbot.repo.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Booking approve(Booking b) {
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            b.setStatus(BookingStatus.CONFIRMED);
            b.getSlot().setBooked(true);
            slotRepo.save(b.getSlot());
        }
        return bookingRepo.save(b);
    }

    @Transactional
    public Booking reject(Booking b) {
        b.setStatus(BookingStatus.REJECTED);
        return bookingRepo.save(b);
    }
}
