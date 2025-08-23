package com.example.crmtgbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean booked;
    @ManyToOne(fetch = FetchType.LAZY)
    private Master master;
    @ManyToOne(fetch = FetchType.LAZY)
    private ServiceItem service;
}
