package com.example.crmtgbot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;         // Название
    private String description;  // Описание
    private String address;      // Адрес (если отличается от адреса профиля мастера)
    private Integer price;       // Стоимость (в условных единицах)
    private Integer duration;    // Длительность в минутах (15/30/45/60 и т.д.)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id")
    private Master master;
}
