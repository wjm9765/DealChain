package com.dealchain.dealchain.domain.chat.repository;

import com.dealchain.dealchain.domain.chat.entity.ChatNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatNotificationRepository extends JpaRepository<ChatNotification,Long> {
}
