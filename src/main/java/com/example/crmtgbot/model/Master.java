package com.example.crmtgbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Master {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private Long userId;
    private String username;

    private String displayName;

    // NEW: профиль
    private String address;   // адрес приёма
    private String about;     // описание деятельности

    private boolean autoConfirm = true;

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceItem> services = new ArrayList<>();

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSlot> slots = new ArrayList<>();
}
