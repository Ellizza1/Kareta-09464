package ru.gr0946x.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gr0946x.entity.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring сам сгенерирует поиск пользователя по его имени для авторизации!
    Optional<User> findByUsername(String username);
}