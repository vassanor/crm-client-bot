package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Master;
import com.example.crmtgbot.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findByMaster(Master m);
    List<ServiceItem> findByMasterId(Long masterId);
    List<ServiceItem> findByMasterIdOrderByNameAsc(Long masterId);
    Optional<ServiceItem> findByIdAndMasterId(Long id, Long masterId);
}