package com.skylanka.airline.service;

import com.skylanka.airline.entity.Booking;
import com.skylanka.airline.entity.Notification;
import com.skylanka.airline.entity.NotificationPreference;
import com.skylanka.airline.entity.User;
import com.skylanka.airline.enums.NotificationChannel;
import com.skylanka.airline.enums.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification sendNotification(User user, String title, String message, NotificationType type);
    Notification createAndSendNotification(User user, String title, String message, 
                                          NotificationChannel channel, NotificationType type);
    List<Notification> getAllNotifications();
    List<Notification> getUserNotifications(User user);
    Optional<Notification> getNotificationById(Long id);
    long getUnreadCount(User user);
    void markAsRead(Long notificationId);
    void resendOrEscalateNotification(Long notificationId);
    void deleteNotification(Long notificationId);

    void sendBookingConfirmationAndETicket(Booking booking);
    void broadcastFlightDelayAlert(Long flightId, String delayReason);

    NotificationPreference getUserPreference(User user);
    void updatePreference(User user, boolean email, boolean sms, boolean web);
}
