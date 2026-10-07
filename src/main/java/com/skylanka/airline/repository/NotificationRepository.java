package com.skylanka.airline.repository;
import com.skylanka.airline.entity.Notification;
import com.skylanka.airline.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderBySentTimeDesc(User user);
    long countByUserAndIsReadFalse(User user);
}
