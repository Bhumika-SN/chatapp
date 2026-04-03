package com.example.chatapp.repository;

import com.example.chatapp.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomNameOrderBySentAtAsc(String roomName);

    @Query("SELECT m FROM Message m WHERE m.roomName = :roomName AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Message> searchInRoom(String roomName, String keyword);

    List<Message> findByStarredTrueAndSender(String sender);
}