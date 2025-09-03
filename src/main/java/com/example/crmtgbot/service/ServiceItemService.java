// src/main/java/com/example/crmtgbot/service/ServiceItemService.java
package com.example.crmtgbot.service;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import com.example.crmtgbot.repo.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceItemService {
    private final ServiceItemRepository repo;

    public List<ServiceItem> listForMaster(Long masterId) {
        return repo.findByMasterIdOrderByNameAsc(masterId);
    }
    public ServiceItem create(Master master, String name) {
        ServiceItem s = new ServiceItem();
        s.setMaster(master);
        s.setName(name);
        return repo.save(s);
    }

    public void save(ServiceItem s) {
        repo.save(s);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public ServiceItem get(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public ServiceItem getForMaster(Long id, Long masterId) {
        return repo.findByIdAndMasterId(id, masterId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found for master"));
    }
}
