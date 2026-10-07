package com.skylanka.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean emailEnabled = true;
    private boolean smsEnabled = true;
    private boolean webEnabled = true;

    public NotificationPreference() {}
    public NotificationPreference(User user) {
        this.user = user;
        this.emailEnabled = true;
        this.smsEnabled = true;
        this.webEnabled = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    public boolean isWebEnabled() { return webEnabled; }
    public void setWebEnabled(boolean webEnabled) { this.webEnabled = webEnabled; }
}
