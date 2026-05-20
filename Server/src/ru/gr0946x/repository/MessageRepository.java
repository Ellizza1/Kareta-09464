package ru.gr0946x.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gr0946x.entity.Message;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByRecipientIsNullOrderBySentAtAsc();

    List<Message> findTop20ByOrderBySentAtDesc();

    @Query("""
        SELECT m FROM Message m
        WHERE
        (LOWER(m.sender.username) = LOWER(?1)
        AND LOWER(m.recipient.username) = LOWER(?2))

        OR

        (LOWER(m.sender.username) = LOWER(?2)
        AND LOWER(m.recipient.username) = LOWER(?1))

        ORDER BY m.sentAt DESC
    """)
    List<Message> findChatHistory(String user1, String user2);
    List<Message> findByTextContainingIgnoreCase(
            String text
    );
}