package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findByMaster(Master m);
    List<ServiceItem> findByMasterId(Long masterId);
}