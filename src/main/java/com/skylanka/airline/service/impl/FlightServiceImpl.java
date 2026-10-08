package com.skylanka.airline.service.impl;

import com.skylanka.airline.dto.FlightFormDto;
import com.skylanka.airline.dto.FlightSearchDto;
import com.skylanka.airline.entity.*;
import com.skylanka.airline.enums.CabinClass;
import com.skylanka.airline.enums.FlightStatus;
import com.skylanka.airline.enums.SeatStatus;
import com.skylanka.airline.repository.*;
import com.skylanka.airline.service.AuditLogService;
import com.skylanka.airline.service.FlightService;
import com.skylanka.airline.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FlightServiceImpl implements FlightService {

    @Autowired private AircraftRepository aircraftRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private RouteRepository routeRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private com.skylanka.airline.repository.FlightHistoryRepository flightHistoryRepository;
    @Autowired(required = false) private FareRepository fareRepository;
    @Autowired(required = false) private FlightSeatRepository flightSeatRepository;

    // =========================================================================
    // 1. AIRCRAFT MANAGEMENT
    // =========================================================================
    @Override
    public List<Aircraft> getAllAircraft() {
        return aircraftRepository.findAll();
    }

    @Override
    public List<Aircraft> getActiveAircraft() {
        return aircraftRepository.findByActiveTrue();
    }

    @Override
    public Optional<Aircraft> getAircraftById(Long id) {
        return aircraftRepository.findById(id);
    }

    @Override
    @Transactional
    public Aircraft saveAircraft(Aircraft aircraft) {
        if (aircraft.getTotalSeats() <= 0) {
            throw new IllegalArgumentException("Total seats must be greater than 0");
        }
        if (aircraft.getEconomySeats() < 0 || aircraft.getBusinessSeats() < 0) {
            throw new IllegalArgumentException("Seats cannot be negative");
        }
        if (aircraft.getEconomySeats() + aircraft.getBusinessSeats() != aircraft.getTotalSeats()) {
            throw new IllegalArgumentException("Economy and Business seats must equal total seats");
        }
        if (aircraft.getRegistrationNumber() == null || aircraft.getRegistrationNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number is required");
        }
        if (aircraft.getModel() == null || aircraft.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Aircraft model is required");
        }
        if (aircraft.getManufacturer() == null || aircraft.getManufacturer().trim().isEmpty()) {
            throw new IllegalArgumentException("Manufacturer is required");
        }
        
        String cleanReg = aircraft.getRegistrationNumber().trim().toUpperCase();
        aircraft.setRegistrationNumber(cleanReg);
        
        Optional<Aircraft> existing = aircraftRepository.findByRegistrationNumber(cleanReg);
        if (existing.isPresent() && !existing.get().getId().equals(aircraft.getId())) {
            throw new IllegalArgumentException("Registration number already exists");
        }
        
        return aircraftRepository.save(aircraft);
    }

    @Override
    @Transactional
    public void toggleAircraftStatus(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found with ID: " + id));

        aircraft.setActive(!aircraft.isActive());
        aircraftRepository.save(aircraft);
        
        String action = aircraft.isActive() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log("MANAGER", "AIRCRAFT_" + action, 
                "Aircraft " + aircraft.getRegistrationNumber() + " " + action.toLowerCase(), "127.0.0.1");
    }

    @Override
    @Transactional
    public String deleteAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found with ID: " + id));

        boolean inUseByFlight = flightRepository.existsByAircraft(aircraft);
        boolean inUseBySeat = seatRepository.existsByAircraft(aircraft);

        if (inUseByFlight || inUseBySeat) {
            return "PROTECTED_REFERENCE: Aircraft " + aircraft.getRegistrationNumber() + " cannot be permanently deleted because it is referenced by existing flight or seat records.";
        }

        aircraftRepository.deleteById(id);
        auditLogService.log("MANAGER", "AIRCRAFT_DELETED", "Aircraft ID: " + id + " deleted", "127.0.0.1");
        return "DELETED: Aircraft removed from system successfully.";
    }

    @Override
    public java.util.Set<Long> getProtectedAircraftIds() {
        java.util.Set<Long> protectedIds = new java.util.HashSet<>();
        // IDs used by flights
        flightRepository.findAll().stream()
            .filter(f -> f.getAircraft() != null)
            .map(f -> f.getAircraft().getId())
            .forEach(protectedIds::add);
        // IDs used by seats
        seatRepository.findAll().stream()
            .filter(s -> s.getAircraft() != null)
            .map(s -> s.getAircraft().getId())
            .forEach(protectedIds::add);
        return protectedIds;
    }

    @Override
    @Transactional
    public Aircraft getOrCreateAircraft(String model, int totalSeats) {
        if (model == null || model.isBlank()) {
            model = "Airbus A320-200";
        }
        String cleanModel = model.trim();
        Optional<Aircraft> opt = aircraftRepository.findByModel(cleanModel);
        if (opt.isPresent()) {
            return opt.get();
        }
        String manufacturer = cleanModel.toLowerCase().contains("boeing") ? "Boeing" :
                              (cleanModel.toLowerCase().contains("atr") ? "ATR" : "Airbus");
        int capacity = totalSeats > 0 ? totalSeats : 60;
        Aircraft aircraft = new Aircraft(cleanModel, manufacturer, capacity);
        return aircraftRepository.save(aircraft);
    }

    // =========================================================================
    // 2. AIRPORT CRUD & VALIDATIONS
    // =========================================================================
    @Override public List<Airport> getAllAirports() { return airportRepository.findAll(); }
    @Override public List<Airport> getAllActiveAirports() { return airportRepository.findByActiveTrue(); }

    @Override
    @Transactional
    public Airport addAirport(String code, String name, String city, String country) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport code cannot be empty");
        }
        String cleanCode = code.toUpperCase().trim();
        if (cleanCode.length() != 3) {
            throw new IllegalArgumentException("Airport IATA code must be exactly 3 uppercase letters (e.g. CMB, DXB)");
        }
        Optional<Airport> existing = airportRepository.findByAirportCode(cleanCode);
        if (existing.isPresent()) {
            Airport ap = existing.get();
            if (!ap.isActive()) {
                ap.setActive(true);
                ap.setAirportName(name.trim());
                ap.setCity(city.trim());
                ap.setCountry(country.trim());
                return airportRepository.save(ap);
            }
            throw new IllegalArgumentException("Airport with code " + cleanCode + " already exists in the system.");
        }
        Airport airport = new Airport(cleanCode, name.trim(), city.trim(), country.trim(), true);
        return airportRepository.save(airport);
    }

    @Override
    @Transactional
    public Airport updateAirport(Long id, String code, String name, String city, String country) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport code cannot be empty");
        }
        String cleanCode = code.toUpperCase().trim();
        if (cleanCode.length() != 3) {
            throw new IllegalArgumentException("Airport IATA code must be exactly 3 uppercase letters (e.g. CMB, DXB)");
        }
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));

        Optional<Airport> existing = airportRepository.findByAirportCode(cleanCode);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Airport code " + cleanCode + " already exists in the system.");
        }

        airport.setAirportCode(cleanCode);
        airport.setAirportName(name.trim());
        airport.setCity(city.trim());
        airport.setCountry(country.trim());
        return airportRepository.save(airport);
    }

    @Override
    @Transactional
    public void deleteAirport(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found with ID: " + id));

        List<Route> routesForAirport = routeRepository.findAll().stream()
                .filter(r -> r.getOrigin().getId().equals(id) || r.getDestination().getId().equals(id))
                .toList();

        boolean hasActiveRoutes = routesForAirport.stream().anyMatch(Route::isActive);
        if (hasActiveRoutes) {
            throw new IllegalStateException("Cannot delete airport " + airport.getAirportCode() + " because active flight routes are linked to it.");
        }

        if (!routesForAirport.isEmpty()) {
            // Soft delete / deactivate to preserve historical route & flight references
            airport.setActive(false);
            airportRepository.save(airport);
            auditLogService.log("MANAGER", "AIRPORT_DEACTIVATED", 
                    "Airport " + airport.getAirportCode() + " deactivated (preserved for historical records)", "127.0.0.1");
        } else {
            airportRepository.deleteById(id);
            auditLogService.log("MANAGER", "AIRPORT_DELETED", "Airport ID: " + id + " deleted", "127.0.0.1");
        }
    }

    // =========================================================================
    // 3. ROUTE CRUD & VALIDATIONS
    // =========================================================================
    @Override public List<Route> getAllRoutes() { return routeRepository.findAll(); }
    @Override public List<Route> getAllActiveRoutes() { return routeRepository.findByActiveTrue(); }

    @Override
    @Transactional
    public Route addRoute(Long originId, Long destinationId, double distanceKm, int durationMinutes) {
        if (originId.equals(destinationId)) {
            throw new IllegalArgumentException("Origin airport and destination airport cannot be the same!");
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Route distance must be greater than 0 km.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Estimated flight duration must be greater than 0 minutes.");
        }

        Airport origin = airportRepository.findById(originId)
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found."));
        Airport dest = airportRepository.findById(destinationId)
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found."));

        Optional<Route> existing = routeRepository.findByOriginAndDestination(origin, dest);
        if (existing.isPresent()) {
            Route r = existing.get();
            if (!r.isActive()) {
                r.setActive(true);
                r.setDistanceKm(distanceKm);
                r.setBaseDurationMinutes(durationMinutes);
                return routeRepository.save(r);
            }
            throw new IllegalArgumentException("Route from " + origin.getAirportCode() + " to " + dest.getAirportCode() + " already exists!");
        }

        Route route = new Route(origin, dest, distanceKm, durationMinutes, true);
        return routeRepository.save(route);
    }

    @Override
    @Transactional
    public Route updateRoute(Long id, Long originId, Long destinationId, double distanceKm, int durationMinutes) {
        if (originId.equals(destinationId)) {
            throw new IllegalArgumentException("Origin airport and destination airport cannot be the same!");
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Route distance must be greater than 0 km.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Estimated flight duration must be greater than 0 minutes.");
        }

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        Airport origin = airportRepository.findById(originId)
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found."));
        Airport dest = airportRepository.findById(destinationId)
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found."));

        Optional<Route> existing = routeRepository.findByOriginAndDestination(origin, dest);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Route from " + origin.getAirportCode() + " to " + dest.getAirportCode() + " already exists!");
        }

        route.setOrigin(origin);
        route.setDestination(dest);
        route.setDistanceKm(distanceKm);
        route.setBaseDurationMinutes(durationMinutes);
        return routeRepository.save(route);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found with ID: " + id));

        List<Flight> flightsOnRoute = flightRepository.findAll().stream()
                .filter(f -> f.getRoute() != null && f.getRoute().getId().equals(id))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        // Check ONLY active / future operational flights
        boolean hasActiveFlights = flightsOnRoute.stream()
                .anyMatch(f -> f.getDepartureTime().isAfter(now) ||
                               f.getFlightStatus() == FlightStatus.SCHEDULED ||
                               f.getFlightStatus() == FlightStatus.ON_TIME ||
                               f.getFlightStatus() == FlightStatus.DELAYED ||
                               f.getFlightStatus() == FlightStatus.BOARDING);

        if (hasActiveFlights) {
            throw new IllegalStateException("Cannot remove route because future scheduled flights are operating on it.");
        }

        if (!flightsOnRoute.isEmpty()) {
            // Has historical flights (DEPARTED/ARRIVED/CANCELLED in past) -> Soft delete / Deactivate to protect FK
            route.setActive(false);
            routeRepository.save(route);
            auditLogService.log("MANAGER", "ROUTE_DEACTIVATED", 
                    "Route " + route.getOrigin().getAirportCode() + " ➔ " + route.getDestination().getAirportCode() + " deactivated (preserved for historical flights)", "127.0.0.1");
        } else {
            // No flights at all -> Safe to physically delete
            routeRepository.delete(route);
            auditLogService.log("MANAGER", "ROUTE_DELETED", "Route ID: " + id + " deleted", "127.0.0.1");
        }
    }

    // =========================================================================
    // 4. FLIGHT SCHEDULES CRUD & BUSINESS RULE VALIDATIONS
    // =========================================================================
    @Override public List<Flight> getAllFlights() { return flightRepository.findAll().stream().filter(f -> !f.isArchived()).toList(); }
    @Override public List<com.skylanka.airline.entity.FlightHistory> getFlightHistory() { return flightHistoryRepository.findAllByOrderByArchivedAtDesc(); }
    @Override public Optional<Flight> getFlightById(Long id) { return flightRepository.findById(id); }

    // Direct Entity creation (used by DataInitializer and programmatic services)
    @Override
    @Transactional
    public Flight createFlight(Flight flight) {
        if (flight.getAircraft() == null && flight.getAircraftType() != null) {
            Aircraft aircraft = getOrCreateAircraft(flight.getAircraftType(), flight.getTotalSeats());
            flight.setAircraft(aircraft);
        }
        if (flight.getTotalSeats() <= 0 && flight.getAircraft() != null) {
            flight.setTotalSeats(flight.getAircraft().getTotalSeats());
        }
        if (flight.getAvailableSeats() <= 0) {
            flight.setAvailableSeats(flight.getTotalSeats());
        }
        Flight saved = flightRepository.save(flight);
        generateSeatsForFlight(saved, saved.getTotalSeats());
        return saved;
    }

    // DTO based creation with comprehensive validation rules (used by Manager Controller)
    @Override
    @Transactional
    public Flight createFlight(FlightFormDto dto) {
        validateFlightRules(dto, true);

        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("Selected route not found."));
        if (!route.isActive()) {
            throw new IllegalArgumentException("Cannot schedule flight on an inactive/deactivated route.");
        }

        Aircraft aircraft = getOrCreateAircraft(dto.getAircraftType(), dto.getTotalSeats());

        Flight flight = new Flight();
        flight.setFlightNumber(dto.getFlightNumber().toUpperCase().trim());
        flight.setRoute(route);
        flight.setAircraft(aircraft);
        flight.setAircraftType(aircraft.getModel());
        flight.setDepartureTime(dto.getDepartureTime());
        flight.setArrivalTime(dto.getArrivalTime());
        flight.setFlightStatus(dto.getFlightStatus() != null ? dto.getFlightStatus() : FlightStatus.SCHEDULED);
        flight.setEconomyFare(dto.getEconomyFare());
        flight.setBusinessFare(dto.getBusinessFare());
        
        // Enforce capacity from Aircraft to prevent manual UI override
        int actualCapacity = aircraft.getTotalSeats();
        flight.setTotalSeats(actualCapacity);
        flight.setAvailableSeats(actualCapacity);

        Flight saved = flightRepository.save(flight);
        generateSeatsForFlight(saved, actualCapacity);

        auditLogService.log("MANAGER", "FLIGHT_CREATED", "Flight: " + saved.getFlightNumber(), "127.0.0.1");
        return saved;
    }

    @Override
    @Transactional
    public Flight updateFlight(Long id, Flight updatedFlight) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        flight.setRoute(updatedFlight.getRoute());
        flight.setDepartureTime(updatedFlight.getDepartureTime());
        flight.setArrivalTime(updatedFlight.getArrivalTime());

        if (updatedFlight.getAircraft() != null) {
            flight.setAircraft(updatedFlight.getAircraft());
        } else if (updatedFlight.getAircraftType() != null) {
            flight.setAircraft(getOrCreateAircraft(updatedFlight.getAircraftType(), flight.getTotalSeats()));
        }

        flight.setEconomyFare(updatedFlight.getEconomyFare());
        flight.setBusinessFare(updatedFlight.getBusinessFare());
        if (updatedFlight.getFlightStatus() != null) {
            flight.setFlightStatus(updatedFlight.getFlightStatus());
        }
        return flightRepository.save(flight);
    }

    @Override
    @Transactional
    public Flight updateFlight(Long id, FlightFormDto dto) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        validateFlightRules(dto, false);

        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("Selected route not found."));

        Aircraft aircraft = getOrCreateAircraft(dto.getAircraftType(), dto.getTotalSeats());

        flight.setFlightNumber(dto.getFlightNumber().toUpperCase().trim());
        flight.setRoute(route);
        Long oldAircraftId = flight.getAircraft() != null ? flight.getAircraft().getId() : null;
        flight.setAircraft(aircraft);
        flight.setAircraftType(aircraft.getModel());
        flight.setDepartureTime(dto.getDepartureTime());
        flight.setArrivalTime(dto.getArrivalTime());
        flight.setEconomyFare(dto.getEconomyFare());
        flight.setBusinessFare(dto.getBusinessFare());
        if (dto.getFlightStatus() != null) {
            flight.setFlightStatus(dto.getFlightStatus());
        }
        
        // Enforce capacity from Aircraft
        int actualCapacity = aircraft.getTotalSeats();
        
        boolean aircraftChanged = oldAircraftId == null || !oldAircraftId.equals(aircraft.getId());
        
        if (aircraftChanged || flight.getTotalSeats() != actualCapacity) {
             flight.setTotalSeats(actualCapacity);
             // Readjust available seats based on capacity difference
             // Better to just call syncAvailableSeats later or reset
             // Actually, the seats will be regenerated or adjusted if needed.
             // If we want to be safe, just update totalSeats. But wait, generateSeatsForFlight wasn't called here for update.
             // We should call generateSeatsForFlight if aircraft changed!
        }

        Flight updated = flightRepository.save(flight);
        
        if (aircraftChanged || flight.getTotalSeats() != actualCapacity) {
             generateSeatsForFlight(updated, actualCapacity);
             syncAvailableSeats(updated.getId());
        }
        
        auditLogService.log("MANAGER", "FLIGHT_UPDATED", "Flight: " + updated.getFlightNumber(), "127.0.0.1");
        return updated;
    }

    private void validateFlightRules(FlightFormDto dto, boolean isNew) {
        if (dto.getFlightNumber() == null || dto.getFlightNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation Error: Flight number cannot be empty.");
        }

        // VALIDATION RULE 1: Cannot schedule a flight in the past!
        if (dto.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Validation Error: Cannot schedule a flight in the past! Departure time must be a future date and time.");
        }

        // VALIDATION RULE 2: Flight number uniqueness
        String cleanFlightNo = dto.getFlightNumber().toUpperCase().trim();
        if (isNew && flightRepository.existsByFlightNumber(cleanFlightNo)) {
            throw new IllegalArgumentException("Validation Error: Flight number " + dto.getFlightNumber() + " already exists in the schedule!");
        } else if (!isNew) {
            Optional<Flight> existing = flightRepository.findByFlightNumber(cleanFlightNo);
            if (existing.isPresent() && !existing.get().getId().equals(dto.getId())) {
                throw new IllegalArgumentException("Validation Error: Flight number " + dto.getFlightNumber() + " already exists in the schedule!");
            }
        }

        // VALIDATION RULE 3: Chronological timing (Arrival must be strictly after Departure)
        if (!dto.getArrivalTime().isAfter(dto.getDepartureTime())) {
            throw new IllegalArgumentException("Validation Error: Arrival time must be strictly AFTER the departure time!");
        }

        // VALIDATION RULE 4: Minimum duration check (at least 30 minutes)
        if (Duration.between(dto.getDepartureTime(), dto.getArrivalTime()).toMinutes() < 30) {
            throw new IllegalArgumentException("Validation Error: Flight duration must be at least 30 minutes.");
        }

        // VALIDATION RULE 5: Fare Hierarchy (Business fare must be greater than Economy fare)
        if (dto.getBusinessFare() <= dto.getEconomyFare()) {
            throw new IllegalArgumentException("Validation Error: Business Class fare ($" + dto.getBusinessFare() + ") must be higher than Economy Class fare ($" + dto.getEconomyFare() + ")!");
        }
    }

    private void generateSeatsForFlight(Flight flight, int totalSeats) {
        // Delete any existing seats for this flight to avoid duplicates
        List<Seat> existing = seatRepository.findByFlightOrderBySeatNumberAsc(flight);
        if (!existing.isEmpty()) {
            if (flightSeatRepository != null) {
                flightSeatRepository.deleteByFlight(flight);
            }
            seatRepository.deleteAll(existing);
        }

        Aircraft aircraft = flight.getAircraft();
        if (aircraft == null && flight.getAircraftType() != null) {
            aircraft = getOrCreateAircraft(flight.getAircraftType(), totalSeats);
            flight.setAircraft(aircraft);
        }

        List<Seat> seats = new ArrayList<>();
        // Row 1-2: Business class
        for (int r = 1; r <= 2; r++) {
            for (char c : new char[]{'A', 'B', 'C', 'D'}) {
                Seat s = new Seat(flight, r + String.valueOf(c), CabinClass.BUSINESS, SeatStatus.AVAILABLE);
                s.setAircraft(aircraft);
                seats.add(s);
            }
        }
        // Row 3+: Economy class
        int economyRows = Math.max(1, (totalSeats - 8) / 4);
        for (int r = 3; r <= (2 + economyRows); r++) {
            for (char c : new char[]{'A', 'B', 'C', 'D'}) {
                Seat s = new Seat(flight, r + String.valueOf(c), CabinClass.ECONOMY, SeatStatus.AVAILABLE);
                s.setAircraft(aircraft);
                seats.add(s);
            }
        }
        List<Seat> savedSeats = seatRepository.saveAll(seats);

        // Normalize FlightSeat records
        if (flightSeatRepository != null) {
            List<FlightSeat> flightSeats = new ArrayList<>();
            for (Seat s : savedSeats) {
                flightSeats.add(new FlightSeat(flight, s, SeatStatus.AVAILABLE));
            }
            flightSeatRepository.saveAll(flightSeats);
        }

        // Normalize Fare records
        if (fareRepository != null) {
            fareRepository.deleteByFlight(flight);
            if (flight.getEconomyFare() > 0) {
                fareRepository.save(new Fare(flight, CabinClass.ECONOMY, flight.getEconomyFare(), Math.round(flight.getEconomyFare() * 0.12 * 100.0) / 100.0));
            }
            if (flight.getBusinessFare() > 0) {
                fareRepository.save(new Fare(flight, CabinClass.BUSINESS, flight.getBusinessFare(), Math.round(flight.getBusinessFare() * 0.12 * 100.0) / 100.0));
            }
        }
    }

    @Override
    @Transactional
    public void updateFlightStatus(Long flightId, FlightStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Validation Error: Flight status cannot be null.");
        }
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Validation Error: Flight not found with ID: " + flightId));
        
        flight.setFlightStatus(status);
        flightRepository.save(flight);
        auditLogService.log("MANAGER", "FLIGHT_STATUS_UPDATED", flight.getFlightNumber() + " -> " + status, "127.0.0.1");
        if (status == FlightStatus.DELAYED || status == FlightStatus.CANCELLED) {
            notificationService.broadcastFlightDelayAlert(flightId, "Flight schedule status updated to " + status);
        }
    }

    @Override
    @Transactional
    public String deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        boolean hasBookings = !bookingRepository.findByFlight(flight).isEmpty();

        if (hasBookings) {
            boolean isActive = flight.getFlightStatus() == FlightStatus.SCHEDULED || 
                               flight.getFlightStatus() == FlightStatus.ON_TIME || 
                               flight.getFlightStatus() == FlightStatus.DELAYED || 
                               flight.getFlightStatus() == FlightStatus.BOARDING;
            
            if (isActive) {
                flight.setFlightStatus(FlightStatus.CANCELLED);
                flight.setAvailableSeats(0);
                flightRepository.save(flight);
                auditLogService.log("MANAGER", "FLIGHT_CANCELLED", "Flight " + flight.getFlightNumber() + " cancelled instead of deleted due to existing bookings", "127.0.0.1");
                return "CANCELLED_INSTEAD: Flight " + flight.getFlightNumber() + " has active bookings and was safely CANCELLED instead of physically deleted.";
            } else {
                return "PROTECTED_HISTORY: Flight " + flight.getFlightNumber() + " cannot be permanently deleted because passenger booking history exists.";
            }
        }

        if (flightSeatRepository != null) {
            flightSeatRepository.deleteByFlight(flight);
        }
        if (fareRepository != null) {
            fareRepository.deleteByFlight(flight);
        }
        seatRepository.deleteAll(seatRepository.findByFlightOrderBySeatNumberAsc(flight));
        flightRepository.delete(flight);
        auditLogService.log("MANAGER", "FLIGHT_DELETED", "Flight ID: " + id, "127.0.0.1");
        return "DELETED: Flight schedule removed successfully.";
    }

    // =========================================================================
    // 5. SEAT SYNCHRONIZATION & OPERATIONAL FILTERING
    // =========================================================================
    @Override
    @Transactional
    public void syncAvailableSeats(Long flightId) {
        flightRepository.findById(flightId).ifPresent(flight -> {
            long availableCount = seatRepository.findByFlightOrderBySeatNumberAsc(flight).stream()
                    .filter(s -> s.getSeatStatus() == SeatStatus.AVAILABLE)
                    .count();
            flight.setAvailableSeats((int) availableCount);
            flightRepository.save(flight);
        });
    }

    @Override
    public List<Flight> getActiveOperationalFlights() {
        LocalDateTime now = LocalDateTime.now();
        return flightRepository.findAll().stream()
                .filter(f -> !f.isArchived())
                // Only include operational current/future flight statuses
                .filter(f -> f.getFlightStatus() == FlightStatus.SCHEDULED ||
                             f.getFlightStatus() == FlightStatus.ON_TIME ||
                             f.getFlightStatus() == FlightStatus.DELAYED ||
                             f.getFlightStatus() == FlightStatus.BOARDING)
                // Strict exclusion of historical completed or cancelled flights
                .filter(f -> f.getFlightStatus() != FlightStatus.DEPARTED &&
                             f.getFlightStatus() != FlightStatus.ARRIVED &&
                             f.getFlightStatus() != FlightStatus.COMPLETED &&
                             f.getFlightStatus() != FlightStatus.CANCELLED)
                // Departure time must be relevant to current/future operations
                .filter(f -> f.getDepartureTime() != null && 
                            (f.getDepartureTime().isAfter(now.minusHours(4)) || 
                             f.getFlightStatus() == FlightStatus.DELAYED || 
                             f.getFlightStatus() == FlightStatus.BOARDING))
                .sorted((a, b) -> a.getDepartureTime().compareTo(b.getDepartureTime()))
                .toList();
    }

    @Override
    public Set<Long> getBookedFlightIds() {
        return bookingRepository.findAll().stream()
                .map(b -> b.getFlight() != null ? b.getFlight().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // 6. MULTI-CRITERIA FLIGHT SEARCH ENGINE (STRICT FUTURE ONLY)
    // =========================================================================
    @Override
    public List<Flight> searchFlights(FlightSearchDto criteria) {
        List<Flight> all = flightRepository.findAll().stream().filter(f -> !f.isArchived()).toList();
        LocalDateTime now = LocalDateTime.now();

        return all.stream()
                // Strict Future Departure Check: Never show flights departing in the past
                .filter(f -> f.getDepartureTime() != null && f.getDepartureTime().isAfter(now))
                // Only flights on active routes and active airports
                .filter(f -> f.getRoute() != null && f.getRoute().isActive() &&
                             f.getRoute().getOrigin() != null && f.getRoute().getOrigin().isActive() &&
                             f.getRoute().getDestination() != null && f.getRoute().getDestination().isActive())
                // Origin
                .filter(f -> criteria.getOriginCode() == null || criteria.getOriginCode().isBlank() ||
                        f.getRoute().getOrigin().getAirportCode().equalsIgnoreCase(criteria.getOriginCode()))
                // Destination
                .filter(f -> criteria.getDestinationCode() == null || criteria.getDestinationCode().isBlank() ||
                        f.getRoute().getDestination().getAirportCode().equalsIgnoreCase(criteria.getDestinationCode()))
                // Travel Date
                .filter(f -> criteria.getTravelDate() == null || 
                        f.getDepartureTime().toLocalDate().isEqual(criteria.getTravelDate()))
                // Min Price
                .filter(f -> criteria.getMinPrice() == null || criteria.getMinPrice() < 0 ||
                        (criteria.getCabinClass() == CabinClass.BUSINESS ? f.getBusinessFare() >= criteria.getMinPrice() : f.getEconomyFare() >= criteria.getMinPrice()))
                // Max Price
                .filter(f -> criteria.getMaxPrice() == null || criteria.getMaxPrice() <= 0 ||
                        (criteria.getCabinClass() == CabinClass.BUSINESS ? f.getBusinessFare() <= criteria.getMaxPrice() : f.getEconomyFare() <= criteria.getMaxPrice()))
                // Seat availability
                .filter(f -> f.getAvailableSeats() > 0)
                // Hide Cancelled, Departed, Arrived, or Completed flights
                .filter(f -> f.getFlightStatus() != FlightStatus.CANCELLED && 
                             f.getFlightStatus() != FlightStatus.DEPARTED && 
                             f.getFlightStatus() != FlightStatus.ARRIVED &&
                             f.getFlightStatus() != FlightStatus.COMPLETED)
                .sorted((a, b) -> a.getDepartureTime().compareTo(b.getDepartureTime()))
                .toList();
    }

    // =========================================================================
    // 7. OLD FLIGHT SCHEDULE AUTOMATIC CLEANUP & LIFECYCLE MANAGEMENT
    // =========================================================================
    @Override
    @Transactional
    public int cleanupOldFlights() {
        LocalDateTime now = LocalDateTime.now();
        List<Flight> allFlights = flightRepository.findAll();
        
        // Find flights where current time >= arrivalTime + 2 hours and not already archived
        List<Flight> eligibleFlights = allFlights.stream()
                .filter(f -> !f.isArchived())
                .filter(f -> f.getArrivalTime() != null && now.isAfter(f.getArrivalTime().plusHours(2)))
                .toList();

        int archivedCount = 0;

        for (Flight flight : eligibleFlights) {
            // Check for duplicates in history
            if (flightHistoryRepository.existsByOriginalFlightId(flight.getId())) {
                flight.setArchived(true);
                flightRepository.save(flight);
                continue;
            }

            // Set original flight to completed and archived
            flight.setFlightStatus(FlightStatus.COMPLETED);
            flight.setAvailableSeats(0);
            flight.setArchived(true);
            flightRepository.save(flight);
            
            // Create history record
            com.skylanka.airline.entity.FlightHistory history = new com.skylanka.airline.entity.FlightHistory(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getRoute() != null ? flight.getRoute().getOrigin().getAirportCode() : "N/A",
                flight.getRoute() != null ? flight.getRoute().getDestination().getAirportCode() : "N/A",
                flight.getAircraft() != null ? flight.getAircraft().getRegistrationNumber() : "N/A",
                flight.getAircraftType(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                "COMPLETED",
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getEconomyFare(),
                flight.getBusinessFare(),
                LocalDateTime.now()
            );
            flightHistoryRepository.save(history);
            
            archivedCount++;
            auditLogService.log("SYSTEM", "OLD_FLIGHT_ARCHIVED",
                    "Auto-archived completed flight: " + flight.getFlightNumber() + " -> FlightHistory", "127.0.0.1");
        }
        return archivedCount;
    }

    @Override
    @Transactional
    public Flight registerFleetFlight(String flightNumber, int totalSeats, String aircraftType, Long routeId,
                                      LocalDateTime departureTime, LocalDateTime arrivalTime,
                                      Double economyFare, Double businessFare, FlightStatus flightStatus) {
        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation Error: Flight Number is required (e.g. UL-980).");
        }
        String cleanFlightNo = flightNumber.trim().toUpperCase();
        if (flightRepository.existsByFlightNumber(cleanFlightNo)) {
            throw new IllegalArgumentException("Validation Error: Flight " + cleanFlightNo + " already exists in the system!");
        }

        int seats = totalSeats > 0 ? totalSeats : 60;
        String plane = (aircraftType != null && !aircraftType.trim().isEmpty()) ? aircraftType.trim() : "Airbus A320-200";

        Aircraft aircraft = getOrCreateAircraft(plane, seats);

        Route route = null;
        if (routeId != null) {
            route = routeRepository.findById(routeId).orElse(null);
        }
        if (route == null) {
            List<Route> routes = routeRepository.findByActiveTrue();
            if (routes.isEmpty()) {
                throw new IllegalStateException("No active routes exist in the system. Please create or activate a route first.");
            }
            route = routes.get(0);
        } else if (!route.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive route to fleet flight.");
        }

        LocalDateTime dep = departureTime != null ? departureTime : LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        if (dep.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Validation Error: Cannot schedule a flight in the past! Departure time must be in the future.");
        }

        LocalDateTime arr = arrivalTime != null ? arrivalTime : dep.plusHours(4).plusMinutes(30);
        if (!arr.isAfter(dep)) {
            throw new IllegalArgumentException("Validation Error: Arrival time must be strictly AFTER departure time.");
        }

        double eco = (economyFare != null && economyFare > 0) ? economyFare : 250.0;
        double bus = (businessFare != null && businessFare > eco) ? businessFare : (eco + 350.0);
        FlightStatus status = flightStatus != null ? flightStatus : FlightStatus.SCHEDULED;

        Flight flight = new Flight();
        flight.setFlightNumber(cleanFlightNo);
        flight.setAircraft(aircraft);
        flight.setAircraftType(aircraft.getModel());
        flight.setRoute(route);
        flight.setDepartureTime(dep);
        flight.setArrivalTime(arr);
        flight.setEconomyFare(eco);
        flight.setBusinessFare(bus);
        flight.setTotalSeats(seats);
        flight.setAvailableSeats(seats);
        flight.setFlightStatus(status);

        Flight saved = flightRepository.save(flight);
        generateSeatsForFlight(saved, seats);

        auditLogService.log("MANAGER", "FLEET_FLIGHT_REGISTERED", "Flight: " + saved.getFlightNumber() + " with " + seats + " seats", "127.0.0.1");
        return saved;
    }
}