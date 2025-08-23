package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Booking;
import com.example.crmtgbot.model.BookingStatus;
import com.example.crmtgbot.model.Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByMasterAndStatus(Master m, BookingStatus status);
}