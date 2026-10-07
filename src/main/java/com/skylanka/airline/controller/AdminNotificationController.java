package com.skylanka.airline.controller;

import com.skylanka.airline.entity.Booking;
import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.entity.Notification;
import com.skylanka.airline.entity.User;
import com.skylanka.airline.enums.NotificationChannel;
import com.skylanka.airline.enums.NotificationType;
import com.skylanka.airline.service.BookingService;
import com.skylanka.airline.service.FlightService;
import com.skylanka.airline.service.NotificationService;
import com.skylanka.airline.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminNotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private UserService userService;
    @Autowired private FlightService flightService;
    @Autowired private BookingService bookingService;

    @GetMapping("/notifications")
    public String notificationManagement(Model model) {
        List<Notification> allNotifications = notificationService.getAllNotifications();
        model.addAttribute("notifications", allNotifications);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("flights", flightService.getAllFlights());
        model.addAttribute("channels", NotificationChannel.values());
        model.addAttribute("types", NotificationType.values());

        long totalDispatched = allNotifications.size();
        long emailCount = allNotifications.stream().filter(n -> n.getChannel() == NotificationChannel.EMAIL).count();
        long smsCount = allNotifications.stream().filter(n -> n.getChannel() == NotificationChannel.SMS).count();
        long webCount = allNotifications.stream().filter(n -> n.getChannel() == NotificationChannel.WEB).count();

        model.addAttribute("totalDispatched", totalDispatched);
        model.addAttribute("emailCount", emailCount);
        model.addAttribute("smsCount", smsCount);
        model.addAttribute("webCount", webCount);

        return "admin/notification-management";
    }

    @PostMapping("/notifications/send")
    public String sendCustomNotification(@RequestParam("userId") Long userId,
                                         @RequestParam("title") String title,
                                         @RequestParam("message") String message,
                                         @RequestParam("channel") NotificationChannel channel,
                                         @RequestParam("type") NotificationType type,
                                         RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getAllUsers().stream().filter(u -> u.getId().equals(userId)).findFirst().orElseThrow();
            notificationService.createAndSendNotification(user, title, message, channel, type);
            redirectAttributes.addFlashAttribute("successMessage", "Alert dispatched successfully to " + user.getEmail());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to dispatch alert: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/broadcast-delay")
    public String broadcastFlightDelay(@RequestParam("flightId") Long flightId,
                                       @RequestParam("reason") String reason,
                                       RedirectAttributes redirectAttributes) {
        try {
            notificationService.broadcastFlightDelayAlert(flightId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Flight delay notifications successfully broadcast to all passengers!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error broadcasting delay: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/resend")
    public String resendNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.resendOrEscalateNotification(id);
            redirectAttributes.addFlashAttribute("successMessage", "Notification re-sent and escalated to secondary channel!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error escalating: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @GetMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(id);
            redirectAttributes.addFlashAttribute("successMessage", "Notification record removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @GetMapping("/passengers/history")
    public String centralizedPassengerHistory(@RequestParam(value = "searchQuery", required = false) String searchQuery,
                                              Model model) {
        List<User> allPassengers = userService.getAllUsers().stream()
                .filter(u -> u.getRole().name().equals("ROLE_PASSENGER"))
                .toList();

        User selectedPassenger = null;
        List<Booking> passengerBookings = null;

        if (searchQuery != null && !searchQuery.isBlank()) {
            selectedPassenger = allPassengers.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(searchQuery.trim()) ||
                                 u.getFullName().toLowerCase().contains(searchQuery.toLowerCase().trim()))
                    .findFirst().orElse(null);
            
            if (selectedPassenger != null) {
                passengerBookings = bookingService.getUserBookings(selectedPassenger);
            }
        }

        model.addAttribute("passengers", allPassengers);
        model.addAttribute("selectedPassenger", selectedPassenger);
        model.addAttribute("passengerBookings", passengerBookings);
        model.addAttribute("searchQuery", searchQuery);
        return "admin/passenger-history";
    }
}
