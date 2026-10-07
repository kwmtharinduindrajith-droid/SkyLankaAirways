package com.skylanka.airline.entity;

import com.skylanka.airline.enums.NotificationChannel;
import com.skylanka.airline.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    private boolean isRead = false;
    private LocalDateTime sentTime = LocalDateTime.now();
    private String status = "SENT";

    public Notification() {}

    public Notification(User user, String title, String message, NotificationChannel channel, NotificationType notificationType) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.channel = channel;
        this.notificationType = notificationType;
        this.sentTime = LocalDateTime.now();
        this.isRead = false;
        this.status = "SENT";
    }

    public Notification(User user, Booking booking, String title, String message, NotificationChannel channel, NotificationType notificationType) {
        this.user = user;
        this.booking = booking;
        this.title = title;
        this.message = message;
        this.channel = channel;
        this.notificationType = notificationType;
        this.sentTime = LocalDateTime.now();
        this.isRead = false;
        this.status = "SENT";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public NotificationType getType() { return notificationType; }
    public void setType(NotificationType type) { this.notificationType = type; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getSentTime() { return sentTime; }
    public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
    public LocalDateTime getSentAt() { return sentTime; }
    public void setSentAt(LocalDateTime sentAt) { this.sentTime = sentAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
