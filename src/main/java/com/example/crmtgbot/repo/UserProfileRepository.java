package com.example.crmtgbot.repo;

import com.example.crmtgbot.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByChatId(Long chatId);
}