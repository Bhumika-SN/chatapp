package com.example.chatapp.repository;

import com.example.chatapp.model.VaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface VaultRepository extends JpaRepository<VaultItem, Long> {
    List<VaultItem> findByOwnerOrderByCreatedAtDesc(String owner);
    List<VaultItem> findByOwnerAndTypeOrderByCreatedAtDesc(String owner, String type);
    List<VaultItem> findByScheduledAtBeforeAndSentFalse(LocalDateTime now);
}