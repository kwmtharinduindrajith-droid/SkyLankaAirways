package com.skylanka.airline.controller;

import com.skylanka.airline.dto.FlightFormDto;
import com.skylanka.airline.dto.FlightSearchDto;
import com.skylanka.airline.entity.Airport;
import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.entity.Route;
import com.skylanka.airline.enums.CabinClass;
import com.skylanka.airline.enums.FlightStatus;
import com.skylanka.airline.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class FlightManagerController {

    @Autowired private FlightService flightService;

    // MANAGER DASHBOARD
    @GetMapping({"/manager/dashboard", "/manager"})
    public String managerDashboard(Model model) {
        List<Flight> allFlights = flightService.getAllFlights();
        long totalFlights = allFlights.size();

        // --- Status counts ---
        long scheduledFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.SCHEDULED || f.getFlightStatus() == FlightStatus.ON_TIME)
                .count();
        long activeFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.BOARDING || f.getFlightStatus() == FlightStatus.DEPARTED)
                .count();
        long delayedFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.DELAYED)
                .count();
        long cancelledFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.CANCELLED)
                .count();
        long completedFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.COMPLETED || f.getFlightStatus() == FlightStatus.ARRIVED)
                .count();

        // --- Percentage of on-time (scheduled + active + completed vs total) ---
        long onTimeCount = scheduledFlights + activeFlights + completedFlights;
        int onTimePct = totalFlights > 0 ? (int)((onTimeCount * 100) / totalFlights) : 0;
        int delayedPct = totalFlights > 0 ? (int)((delayedFlights * 100) / totalFlights) : 0;
        int cancelledPct = totalFlights > 0 ? (int)((cancelledFlights * 100) / totalFlights) : 0;
        int completedPct = totalFlights > 0 ? (int)((completedFlights * 100) / totalFlights) : 0;

        model.addAttribute("totalFlights", totalFlights);
        model.addAttribute("scheduledFlights", scheduledFlights);
        model.addAttribute("activeFlights", activeFlights);
        model.addAttribute("delayedFlights", delayedFlights);
        model.addAttribute("cancelledFlights", cancelledFlights);
        model.addAttribute("completedFlights", completedFlights);
        model.addAttribute("onTimePct", onTimePct);
        model.addAttribute("delayedPct", delayedPct);
        model.addAttribute("cancelledPct", cancelledPct);
        model.addAttribute("completedPct", completedPct);

        // --- Routes & Airports ---
        List<Route> routes = flightService.getAllRoutes();
        List<Route> activeRoutes = flightService.getAllActiveRoutes();
        model.addAttribute("totalRoutes", routes.size());
        model.addAttribute("activeRoutes", activeRoutes.size());

        List<?> airports = flightService.getAllAirports();
        List<?> activeAirports = flightService.getAllActiveAirports();
        model.addAttribute("totalAirports", airports.size());
        model.addAttribute("activeAirports", activeAirports.size());

        // --- Upcoming departures (next 8) ---
        List<Flight> upcomingFlights = allFlights.stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.SCHEDULED || f.getFlightStatus() == FlightStatus.DELAYED)
                .sorted((f1, f2) -> f1.getDepartureTime().compareTo(f2.getDepartureTime()))
                .limit(8)
                .toList();
        model.addAttribute("upcomingFlights", upcomingFlights);

        // --- Recent flights (last 5, any status, sorted by departure desc) ---
        List<Flight> recentFlights = allFlights.stream()
                .sorted((f1, f2) -> f2.getDepartureTime().compareTo(f1.getDepartureTime()))
                .limit(5)
                .toList();
        model.addAttribute("recentFlights", recentFlights);

        return "manager/dashboard";
    }

    // FLIGHT SCHEDULES DASHBOARD
    @GetMapping("/manager/flights")
    public String flightSchedulesDashboard(@RequestParam(value = "filterStatus", required = false) FlightStatus filterStatus,
                                          Model model) {
        List<Flight> allFlights = flightService.getAllFlights().stream()
                .filter(f -> f.getFlightStatus() != FlightStatus.COMPLETED)
                .toList();
        List<Flight> displayedFlights = (filterStatus != null) ?
                allFlights.stream().filter(f -> f.getFlightStatus() == filterStatus).toList() : allFlights;

        model.addAttribute("flights", displayedFlights);
        model.addAttribute("routes", flightService.getAllRoutes());
        model.addAttribute("statuses", FlightStatus.values());
        model.addAttribute("filterStatus", filterStatus);

        if (!model.containsAttribute("flightForm")) {
            FlightFormDto form = new FlightFormDto();
            form.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            form.setArrivalTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(30));
            form.setFlightNumber("UL-" + (300 + (int)(Math.random() * 600)));
            form.setEconomyFare(350.0);
            form.setBusinessFare(850.0);
            form.setTotalSeats(60);
            model.addAttribute("flightForm", form);
        }

        long totalCount = allFlights.size();
        long scheduledCount = allFlights.stream().filter(f -> f.getFlightStatus() == FlightStatus.SCHEDULED).count();
        long delayedCount = allFlights.stream().filter(f -> f.getFlightStatus() == FlightStatus.DELAYED).count();
        long cancelledCount = allFlights.stream().filter(f -> f.getFlightStatus() == FlightStatus.CANCELLED).count();
        double avgEconomy = allFlights.stream().mapToDouble(Flight::getEconomyFare).average().orElse(0.0);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("delayedCount", delayedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("avgEconomy", avgEconomy);
        model.addAttribute("bookedFlightIds", flightService.getBookedFlightIds());

        return "manager/flight-schedules";
    }

    // DEDICATED NEW FLIGHT CREATION PAGE
    @GetMapping({"/manager/flights/new", "/manager/flights/create"})
    public String newFlightPage(@RequestParam(value = "routeId", required = false) Long routeId, Model model) {
        if (!model.containsAttribute("flightForm")) {
            FlightFormDto form = new FlightFormDto();
            if (routeId != null) {
                form.setRouteId(routeId);
            }
            form.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            form.setArrivalTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(30));
            form.setFlightNumber("UL-" + (300 + (int)(Math.random() * 600)));
            form.setEconomyFare(350.0);
            form.setBusinessFare(850.0);
            form.setTotalSeats(60);
            model.addAttribute("flightForm", form);
        }
        model.addAttribute("routes", flightService.getAllActiveRoutes());
        model.addAttribute("aircraftList", flightService.getActiveAircraft());
        return "manager/flight-create";
    }

    // EDIT EXISTING FLIGHT
    @GetMapping("/manager/flights/edit/{id}")
    public String editFlightPage(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Flight flight = flightService.getFlightById(id).orElse(null);
        if (flight == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Flight not found.");
            return "redirect:/manager/flights";
        }
        model.addAttribute("flightForm", new FlightFormDto(flight));
        model.addAttribute("routes", flightService.getAllActiveRoutes());
        model.addAttribute("aircraftList", flightService.getAllAircraft());
        return "manager/flight-create";
    }

    // SAVE / SCHEDULE FLIGHT WITH STRICT VALIDATION
    @PostMapping("/manager/flights/save")
    public String saveFlight(@Valid @ModelAttribute("flightForm") FlightFormDto flightForm,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.flightForm", result);
            redirectAttributes.addFlashAttribute("flightForm", flightForm);
            redirectAttributes.addFlashAttribute("errorMessage", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/manager/flights/new";
        }

        try {
            if (flightForm.getId() != null) {
                flightService.updateFlight(flightForm.getId(), flightForm);
                redirectAttributes.addFlashAttribute("successMessage", "Flight " + flightForm.getFlightNumber() + " schedule updated successfully!");
            } else {
                flightService.createFlight(flightForm);
                redirectAttributes.addFlashAttribute("successMessage", "New flight " + flightForm.getFlightNumber() + " scheduled and seats generated successfully!");
            }
            return "redirect:/manager/flights";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flightForm", flightForm);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/flights/new";
        }
    }

    // QUICK STATUS UPDATER
    @PostMapping("/manager/flights/update-status")
    public String updateFlightStatus(@RequestParam(value = "flightId", required = false) Long flightId,
                                     @RequestParam(value = "status", required = false) String statusStr,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (flightId == null) {
                throw new IllegalArgumentException("Flight ID is required to update status.");
            }
            if (statusStr == null || statusStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Flight status value is required.");
            }
            FlightStatus status;
            try {
                status = FlightStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid flight status provided: " + statusStr);
            }
            flightService.updateFlightStatus(flightId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Flight status updated to " + status + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/manager/flights";
    }

    // DELETE FLIGHT
    @GetMapping("/manager/flights/delete/{id}")
    public String deleteFlight(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            String result = flightService.deleteFlight(id);
            if (result.startsWith("PROTECTED_HISTORY:")) {
                redirectAttributes.addFlashAttribute("errorMessage", result.replace("PROTECTED_HISTORY:", "").trim());
            } else if (result.startsWith("CANCELLED_INSTEAD:")) {
                redirectAttributes.addFlashAttribute("infoMessage", result.replace("CANCELLED_INSTEAD:", "").trim());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result.replace("DELETED:", "").trim());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/flights";
    }

    // ROUTE & AIRPORT MANAGEMENT
    @GetMapping("/manager/routes")
    public String routeManagement(Model model) {
        model.addAttribute("routes", flightService.getAllRoutes());
        model.addAttribute("activeRoutes", flightService.getAllActiveRoutes());
        model.addAttribute("airports", flightService.getAllAirports());
        model.addAttribute("activeAirports", flightService.getAllActiveAirports());
        return "manager/route-management";
    }

    @GetMapping("/manager/airports")
    public String airportManagement(Model model) {
        model.addAttribute("airports", flightService.getAllAirports());
        model.addAttribute("activeAirports", flightService.getAllActiveAirports());
        return "manager/airport-management";
    }

    @PostMapping("/manager/airports/save")
    public String saveAirport(@RequestParam(value = "id", required = false) Long id,
                              @RequestParam("code") String code,
                              @RequestParam("name") String name,
                              @RequestParam("city") String city,
                              @RequestParam("country") String country,
                              RedirectAttributes redirectAttributes) {
        try {
            if (id != null) {
                Airport airport = flightService.updateAirport(id, code, name, city, country);
                redirectAttributes.addFlashAttribute("successMessage", "Airport " + airport.getAirportCode() + " updated successfully!");
            } else {
                Airport airport = flightService.addAirport(code, name, city, country);
                redirectAttributes.addFlashAttribute("successMessage", "Airport " + airport.getAirportCode() + " added successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/airports";
    }

    @GetMapping("/manager/airports/delete/{id}")
    public String deleteAirport(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            flightService.deleteAirport(id);
            redirectAttributes.addFlashAttribute("successMessage", "Airport removed from active management successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/airports";
    }

    // AIRCRAFT FLEET MANAGEMENT
    @GetMapping("/manager/aircrafts")
    public String aircraftManagement(Model model) {
        model.addAttribute("aircraftList", flightService.getAllAircraft());
        long activeCount = flightService.getAllAircraft().stream().filter(com.skylanka.airline.entity.Aircraft::isActive).count();
        int totalFleetCapacity = flightService.getAllAircraft().stream().mapToInt(com.skylanka.airline.entity.Aircraft::getTotalSeats).sum();
        model.addAttribute("activeAircrafts", activeCount);
        model.addAttribute("totalFleetCapacity", totalFleetCapacity);
        model.addAttribute("protectedAircraftIds", flightService.getProtectedAircraftIds());
        return "manager/aircraft-management";
    }

    @PostMapping("/manager/aircrafts/save")
    public String saveAircraft(@RequestParam(value = "id", required = false) Long id,
                               @RequestParam("registrationNumber") String registrationNumber,
                               @RequestParam("manufacturer") String manufacturer,
                               @RequestParam("model") String aircraftModel,
                               @RequestParam("economySeats") int economySeats,
                               @RequestParam("businessSeats") int businessSeats,
                               @RequestParam(value = "active", defaultValue = "false") boolean active,
                               RedirectAttributes redirectAttributes) {
        try {
            com.skylanka.airline.entity.Aircraft aircraft = new com.skylanka.airline.entity.Aircraft();
            if (id != null) {
                aircraft = flightService.getAircraftById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
            }
            aircraft.setRegistrationNumber(registrationNumber);
            aircraft.setManufacturer(manufacturer);
            aircraft.setModel(aircraftModel);
            aircraft.setEconomySeats(economySeats);
            aircraft.setBusinessSeats(businessSeats);
            aircraft.setTotalSeats(economySeats + businessSeats);
            aircraft.setActive(active);
            
            flightService.saveAircraft(aircraft);
            redirectAttributes.addFlashAttribute("successMessage", "Aircraft " + registrationNumber + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/aircrafts";
    }

    @GetMapping("/manager/aircrafts/toggle/{id}")
    public String toggleAircraft(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            flightService.toggleAircraftStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Aircraft status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/aircrafts";
    }

    @PostMapping("/manager/aircrafts/delete/{id}")
    public String deleteAircraft(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            String result = flightService.deleteAircraft(id);
            if (result.startsWith("PROTECTED_REFERENCE:")) {
                redirectAttributes.addFlashAttribute("errorMessage", result.replace("PROTECTED_REFERENCE:", "").trim());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result.replace("DELETED:", "").trim());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/aircrafts";
    }

    @PostMapping({"/manager/routes/save", "/manager/routes/create"})
    public String saveRoute(@RequestParam(value = "id", required = false) Long id,
                            @RequestParam("originId") Long originId,
                            @RequestParam("destinationId") Long destinationId,
                            @RequestParam("distanceKm") double distanceKm,
                            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,
                            @RequestParam(value = "baseDurationMinutes", required = false) Integer baseDurationMinutes,
                            RedirectAttributes redirectAttributes) {
        try {
            int duration = (durationMinutes != null) ? durationMinutes : (baseDurationMinutes != null ? baseDurationMinutes : 60);
            if (id != null) {
                Route route = flightService.updateRoute(id, originId, destinationId, distanceKm, duration);
                redirectAttributes.addFlashAttribute("successMessage", "Route updated successfully!");
            } else {
                Route route = flightService.addRoute(originId, destinationId, distanceKm, duration);
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Route " + route.getOrigin().getAirportCode() + " ➔ " + route.getDestination().getAirportCode() + " established!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/routes";
    }

    @GetMapping("/manager/routes/delete/{id}")
    public String deleteRoute(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            flightService.deleteRoute(id);
            redirectAttributes.addFlashAttribute("successMessage", "Route removed from active management successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/routes";
    }

    // MULTI-CRITERIA FLIGHT SEARCH & REAL-TIME STATUS BOARD
    @GetMapping({"/search", "/flights/search"})
    public String publicFlightSearch(@ModelAttribute("searchDto") FlightSearchDto searchDto, Model model) {
        model.addAttribute("airports", flightService.getAllActiveAirports());
        model.addAttribute("cabinClasses", CabinClass.values());

        boolean isSearchSubmitted = searchDto.getOriginCode() != null || searchDto.getDestinationCode() != null || searchDto.getTravelDate() != null;

        if (isSearchSubmitted) {
            boolean hasErrors = false;
            
            if (searchDto.getOriginCode() == null || searchDto.getOriginCode().isBlank()) {
                model.addAttribute("errorMessage", "Origin airport is required.");
                hasErrors = true;
            } else if (searchDto.getDestinationCode() == null || searchDto.getDestinationCode().isBlank()) {
                model.addAttribute("errorMessage", "Destination airport is required.");
                hasErrors = true;
            } else if (searchDto.getTravelDate() == null) {
                model.addAttribute("errorMessage", "Travel date is required.");
                hasErrors = true;
            } else if (searchDto.getOriginCode().equalsIgnoreCase(searchDto.getDestinationCode())) {
                model.addAttribute("errorMessage", "Origin and destination airports cannot be the same.");
                hasErrors = true;
            } else if (searchDto.getMinPrice() != null && searchDto.getMinPrice() < 0) {
                model.addAttribute("errorMessage", "Minimum price cannot be negative.");
                hasErrors = true;
            } else if (searchDto.getMaxPrice() != null && searchDto.getMaxPrice() < 0) {
                model.addAttribute("errorMessage", "Maximum price cannot be negative.");
                hasErrors = true;
            } else if (searchDto.getMinPrice() != null && searchDto.getMaxPrice() != null && searchDto.getMinPrice() > searchDto.getMaxPrice()) {
                model.addAttribute("errorMessage", "Invalid price range. Minimum price cannot be greater than maximum price.");
                hasErrors = true;
            }

            if (!hasErrors) {
                List<Flight> results = flightService.searchFlights(searchDto);
                model.addAttribute("searchResults", results);
                
                if (results.isEmpty()) {
                    model.addAttribute("infoMessage", "No flights found for the selected route and date. Please try different dates or airports.");
                }
            }
        } else {
            // Default page load shows all active operational flights
            model.addAttribute("searchResults", flightService.getActiveOperationalFlights());
        }
        
        model.addAttribute("searchDto", searchDto);
        return "passenger/flight-search";
    }

    @GetMapping("/flights/status")
    public String liveFlightStatusBoard(Model model) {
        LocalDateTime recentThreshold = LocalDateTime.now().minusHours(4);
        List<Flight> activeFlights = flightService.getAllFlights().stream()
                .filter(f -> f.getDepartureTime().isAfter(recentThreshold))
                .sorted((a, b) -> a.getDepartureTime().compareTo(b.getDepartureTime()))
                .toList();
        model.addAttribute("flights", activeFlights);
        return "flight/flight-status";
    }

    // DEDICATED FLIGHT & FLEET REGISTRY PAGE
    @GetMapping({"/manager/flight-registry", "/manager/fleet"})
    public String flightRegistryPage(@RequestParam(value = "search", required = false) String search,
                                     Model model) {
        List<Flight> allFlights = flightService.getAllFlights();
        List<Flight> displayedFlights = allFlights;
        if (search != null && !search.trim().isEmpty()) {
            String query = search.trim().toLowerCase();
            displayedFlights = allFlights.stream()
                    .filter(f -> f.getFlightNumber().toLowerCase().contains(query) ||
                            (f.getAircraftType() != null && f.getAircraftType().toLowerCase().contains(query)) ||
                            (f.getRoute() != null && (
                                    f.getRoute().getOrigin().getAirportCode().toLowerCase().contains(query) ||
                                    f.getRoute().getDestination().getAirportCode().toLowerCase().contains(query)
                            )))
                    .toList();
        }

        List<Route> routes = flightService.getAllActiveRoutes();
        int totalSeats = allFlights.stream().mapToInt(Flight::getTotalSeats).sum();
        int totalAvailable = allFlights.stream().mapToInt(Flight::getAvailableSeats).sum();

        model.addAttribute("flights", displayedFlights);
        model.addAttribute("allFlightsCount", allFlights.size());
        model.addAttribute("totalSeats", totalSeats);
        model.addAttribute("totalAvailable", totalAvailable);
        model.addAttribute("routes", routes);
        model.addAttribute("statuses", FlightStatus.values());
        model.addAttribute("search", search);
        model.addAttribute("bookedFlightIds", flightService.getBookedFlightIds());

        // Pre-fill defaults for quick addition
        model.addAttribute("defaultFlightNumber", "UL-" + (700 + (int)(Math.random() * 290)));
        model.addAttribute("defaultDepartureTime", LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        model.addAttribute("defaultArrivalTime", LocalDateTime.now().plusDays(1).withHour(14).withMinute(30));

        return "manager/flight-registry";
    }

    @PostMapping({"/manager/flight-registry/save", "/manager/fleet/save"})
    public String saveFleetFlight(@RequestParam("flightNumber") String flightNumber,
                                  @RequestParam(value = "totalSeats", defaultValue = "60") int totalSeats,
                                  @RequestParam(value = "aircraftType", required = false) String aircraftType,
                                  @RequestParam(value = "routeId", required = false) Long routeId,
                                  @RequestParam(value = "departureTime", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTime,
                                  @RequestParam(value = "arrivalTime", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime,
                                  @RequestParam(value = "economyFare", required = false) Double economyFare,
                                  @RequestParam(value = "businessFare", required = false) Double businessFare,
                                  @RequestParam(value = "flightStatus", required = false) FlightStatus flightStatus,
                                  RedirectAttributes redirectAttributes) {
        try {
            Flight saved = flightService.registerFleetFlight(flightNumber, totalSeats, aircraftType, routeId, departureTime, arrivalTime, economyFare, businessFare, flightStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Flight " + saved.getFlightNumber() + " successfully registered into fleet with " + saved.getTotalSeats() + " seats generated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/flight-registry";
    }

    @GetMapping("/manager/flight-registry/delete/{id}")
    public String deleteFleetFlight(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            String result = flightService.deleteFlight(id);
            if (result.startsWith("PROTECTED_HISTORY:")) {
                redirectAttributes.addFlashAttribute("errorMessage", result.replace("PROTECTED_HISTORY:", "").trim());
            } else if (result.startsWith("CANCELLED_INSTEAD:")) {
                redirectAttributes.addFlashAttribute("infoMessage", result.replace("CANCELLED_INSTEAD:", "").trim());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result.replace("DELETED:", "").trim());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/flight-registry";
    }

    @GetMapping("/manager/flight-history")
    public String flightHistory(Model model) {
        java.util.List<com.skylanka.airline.entity.FlightHistory> history = flightService.getFlightHistory();
        
        long totalArchived = history.size();
        
        java.time.LocalDate today = java.time.LocalDate.now();
        long archivedToday = history.stream()
                .filter(h -> h.getArchivedAt() != null && h.getArchivedAt().toLocalDate().equals(today))
                .count();
                
        long archivedThisMonth = history.stream()
                .filter(h -> h.getArchivedAt() != null && 
                        h.getArchivedAt().getYear() == today.getYear() && 
                        h.getArchivedAt().getMonth() == today.getMonth())
                .count();

        model.addAttribute("historyList", history);
        model.addAttribute("totalArchived", totalArchived);
        model.addAttribute("archivedToday", archivedToday);
        model.addAttribute("archivedThisMonth", archivedThisMonth);
        return "manager/flight-history";
    }
}