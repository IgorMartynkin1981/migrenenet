package ru.migrenenet.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.migrenenet.model.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    List<Subscriber> findByActiveTrue();
}
