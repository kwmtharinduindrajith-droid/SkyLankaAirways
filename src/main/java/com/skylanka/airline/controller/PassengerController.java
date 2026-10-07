package com.skylanka.airline.controller;

import com.skylanka.airline.entity.Booking;
import com.skylanka.airline.entity.Payment;
import com.skylanka.airline.entity.User;
import com.skylanka.airline.enums.BookingStatus;
import com.skylanka.airline.repository.PaymentRepository;
import com.skylanka.airline.service.BookingService;
import com.skylanka.airline.service.NotificationService;
import com.skylanka.airline.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired private UserService userService;
    @Autowired private BookingService bookingService;
    @Autowired private NotificationService notificationService;
    @Autowired private PaymentRepository paymentRepository;

    private User getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        List<Booking> bookings = bookingService.getUserBookings(user);
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("unreadNotifications", notificationService.getUnreadCount(user));
        return "passenger/dashboard";
    }

    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        model.addAttribute("bookings", bookingService.getUserBookings(user));
        return "passenger/my-bookings";
    }

    // Member 6: Profile & Centralized Travel Lifecycle History
    @GetMapping("/profile-history")
    public String viewProfileAndTravelHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        List<Booking> allBookings = bookingService.getUserBookings(user);

        long totalFlights = allBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED).count();
        double totalSpent = allBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                .mapToDouble(Booking::getTotalAmount).sum();

        model.addAttribute("user", user);
        model.addAttribute("bookings", allBookings);
        model.addAttribute("totalFlights", totalFlights);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "passenger/profile-history";
    }

    // Member 6: Automated E-Ticket Distribution View
    @GetMapping("/eticket/{bookingId}")
    public String viewETicket(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId).orElseThrow();
        model.addAttribute("booking", booking);
        return "passenger/eticket-view";
    }

    // Member 6: Official Booking & Tax Invoice Receipt
    @GetMapping("/receipt/{bookingId}")
    public String viewBookingReceipt(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId).orElseThrow();
        Payment payment = paymentRepository.findByBooking(booking).stream().findFirst().orElse(null);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        return "passenger/receipt-view";
    }

    // Member 6: Notification Inbox & Preferences
    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        model.addAttribute("notifications", notificationService.getUserNotifications(user));
        model.addAttribute("preference", notificationService.getUserPreference(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "passenger/notifications";
    }

    @PostMapping("/notifications/preferences")
    public String updatePreferences(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(value = "emailEnabled", defaultValue = "false") boolean email,
                                    @RequestParam(value = "smsEnabled", defaultValue = "false") boolean sms,
                                    @RequestParam(value = "webEnabled", defaultValue = "false") boolean web,
                                    RedirectAttributes redirectAttributes) {
        User user = getUser(userDetails);
        notificationService.updatePreference(user, email, sms, web);
        redirectAttributes.addFlashAttribute("successMessage", "Notification preferences saved successfully!");
        return "redirect:/passenger/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/passenger/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteAlert(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        notificationService.deleteNotification(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notification dismissed.");
        return "redirect:/passenger/notifications";
    }
}
