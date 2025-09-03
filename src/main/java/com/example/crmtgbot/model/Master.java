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
    @OrderBy("id ASC")
    private List<ServiceItem> services = new ArrayList<>();

    // ВАЖНО: не меняем ссылку! Только очищаем и добавляем с проставлением обратной связи
    public void setServices(List<ServiceItem> items) {
        this.services.clear();
        if (items != null) {
            for (ServiceItem s : items) addService(s);
        }
    }

    public void addService(ServiceItem s) {
        if (s == null) return;
        s.setMaster(this);
        this.services.add(s);
    }

    public void removeService(ServiceItem s) {
        if (s == null) return;
        this.services.remove(s);
        s.setMaster(null);
    }



    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSlot> slots = new ArrayList<>();
}
