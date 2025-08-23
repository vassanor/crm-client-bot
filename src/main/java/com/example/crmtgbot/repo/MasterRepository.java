package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterRepository extends JpaRepository<Master, Long> {

    Optional<Master> findByChatId(Long chatId);
}