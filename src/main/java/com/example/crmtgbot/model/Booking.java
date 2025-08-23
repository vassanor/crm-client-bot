package com.example.crmtgbot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Master master;
    @ManyToOne(fetch = FetchType.LAZY)
    private ServiceItem service;
    @ManyToOne(fetch = FetchType.LAZY)
    private TimeSlot slot;
    private Long clientChatId;
    private Integer clientUserId;
    private String clientUsername;
    private String clientDisplayName;
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
}
