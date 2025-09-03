package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Booking;
import com.example.crmtgbot.model.BookingStatus;
import com.example.crmtgbot.model.Master;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByMasterAndStatus(Master m, BookingStatus status);
    List<Booking> findByMasterIdAndSlotStartTimeAfterOrderBySlotStartTimeAsc(Long masterId, LocalDateTime from);

    @Query("""
           select b from Booking b
           join fetch b.master m
           join fetch b.service s
           join fetch b.slot t
           where b.id = :id
           """)
    Optional<Booking> findWithDetailsById(@Param("id") Long id);

    @Query("""
       select b from Booking b
       join fetch b.service s
       join fetch b.slot t
       where b.master.id = :masterId
         and t.startTime > :from
       order by t.startTime asc
       """)
    List<Booking> upcomingWithDetails(@Param("masterId") Long masterId,
                                      @Param("from") java.time.LocalDateTime from);

}