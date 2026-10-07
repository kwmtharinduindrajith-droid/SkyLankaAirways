package com.skylanka.airline.service.impl;

import com.skylanka.airline.entity.*;
import com.skylanka.airline.enums.NotificationChannel;
import com.skylanka.airline.enums.NotificationType;
import com.skylanka.airline.repository.BookingRepository;
import com.skylanka.airline.repository.FlightRepository;
import com.skylanka.airline.repository.NotificationPreferenceRepository;
import com.skylanka.airline.repository.NotificationRepository;
import com.skylanka.airline.service.AuditLogService;
import com.skylanka.airline.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationPreferenceRepository preferenceRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AuditLogService auditLogService;

    @Override
    @Transactional
    public Notification sendNotification(User user, String title, String message, NotificationType type) {
        return createAndSendNotification(user, title, message, NotificationChannel.WEB, type);
    }

    @Override
    @Transactional
    public Notification createAndSendNotification(User user, String title, String message, 
                                                 NotificationChannel channel, NotificationType type) {
        NotificationPreference pref = getUserPreference(user);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setChannel(channel != null ? channel : NotificationChannel.WEB);
        notification.setSentTime(LocalDateTime.now());
        notification.setRead(false);
        notification.setStatus("DELIVERED");

        if (channel == NotificationChannel.EMAIL && !pref.isEmailEnabled()) {
            notification.setStatus("ESCALATED_TO_WEB");
            notification.setChannel(NotificationChannel.WEB);
        } else if (channel == NotificationChannel.SMS && !pref.isSmsEnabled()) {
            notification.setStatus("ESCALATED_TO_WEB");
            notification.setChannel(NotificationChannel.WEB);
        }

        Notification saved = notificationRepository.save(notification);
        auditLogService.log(user.getEmail(), "NOTIFICATION_DISPATCHED", 
                "[" + notification.getChannel() + "] " + title, "127.0.0.1");
        return saved;
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderBySentTimeDesc(user);
    }

    @Override
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void resendOrEscalateNotification(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getChannel() == NotificationChannel.EMAIL) {
                n.setChannel(NotificationChannel.SMS);
            } else if (n.getChannel() == NotificationChannel.SMS) {
                n.setChannel(NotificationChannel.WEB);
            }
            n.setStatus("RE_DELIVERED");
            n.setSentTime(LocalDateTime.now());
            notificationRepository.save(n);
            auditLogService.log(n.getUser().getEmail(), "NOTIFICATION_ESCALATED", 
                    "Alert escalated to " + n.getChannel(), "127.0.0.1");
        });
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void sendBookingConfirmationAndETicket(Booking booking) {
        String title = "✈️ E-Ticket & Booking Confirmed: " + booking.getPnrNumber();
        String msg = "Dear " + booking.getUser().getFullName() + ", your e-ticket for Flight " 
                   + booking.getFlight().getFlightNumber() + " is confirmed. PNR: " + booking.getPnrNumber()
                   + ". Departure: " + booking.getFlight().getDepartureTime();
        
        createAndSendNotification(booking.getUser(), title, msg, NotificationChannel.EMAIL, NotificationType.BOOKING_CONFIRMATION);
        createAndSendNotification(booking.getUser(), "📱 SMS Alert: Flight " + booking.getFlight().getFlightNumber() + " Booked", 
                "SkyLanka: E-ticket issued for PNR " + booking.getPnrNumber() + ". Seat: " + booking.getSelectedSeatNumber(), 
                NotificationChannel.SMS, NotificationType.BOOKING_CONFIRMATION);
    }

    @Override
    @Transactional
    public void broadcastFlightDelayAlert(Long flightId, String delayReason) {
        flightRepository.findById(flightId).ifPresent(flight -> {
            List<Booking> bookings = bookingRepository.findByFlight(flight);
            for (Booking b : bookings) {
                String title = "⚠️ Flight Delay Notice: " + flight.getFlightNumber();
                String msg = "Flight " + flight.getFlightNumber() + " is delayed. Reason: " 
                           + (delayReason != null && !delayReason.isBlank() ? delayReason : "Operational requirement")
                           + ". Estimated Departure: " + flight.getDepartureTime();
                createAndSendNotification(b.getUser(), title, msg, NotificationChannel.EMAIL, NotificationType.FLIGHT_DELAY);
                createAndSendNotification(b.getUser(), "🚨 SMS Alert: Flight " + flight.getFlightNumber() + " Delayed", 
                        msg, NotificationChannel.SMS, NotificationType.FLIGHT_DELAY);
            }
        });
    }

    @Override
    public NotificationPreference getUserPreference(User user) {
        return preferenceRepository.findByUser(user)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(user)));
    }

    @Override
    @Transactional
    public void updatePreference(User user, boolean email, boolean sms, boolean web) {
        NotificationPreference pref = getUserPreference(user);
        pref.setEmailEnabled(email);
        pref.setSmsEnabled(sms);
        pref.setWebEnabled(web);
        preferenceRepository.save(pref);
    }
}
