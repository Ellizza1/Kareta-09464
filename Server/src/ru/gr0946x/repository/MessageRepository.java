package ru.gr0946x.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gr0946x.entity.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Метод автоматически достанет историю сообщений общего чата по порядку времени
    List<Message> findByRecipientIsNullOrderBySentAtAsc();
}