package com.skylanka.airline.service;

import com.skylanka.airline.dto.FlightFormDto;
import com.skylanka.airline.dto.FlightSearchDto;
import com.skylanka.airline.entity.Aircraft;
import com.skylanka.airline.entity.Airport;
import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.entity.Route;
import com.skylanka.airline.enums.FlightStatus;

import java.util.List;
import java.util.Optional;

public interface FlightService {
    // Aircraft Operations
    List<Aircraft> getAllAircraft();
    List<Aircraft> getActiveAircraft();
    Optional<Aircraft> getAircraftById(Long id);
    Aircraft saveAircraft(Aircraft aircraft);
    void toggleAircraftStatus(Long id);
    String deleteAircraft(Long id);
    java.util.Set<Long> getProtectedAircraftIds();
    Aircraft getOrCreateAircraft(String model, int totalSeats);

    // Airport Operations
    List<Airport> getAllAirports();
    List<Airport> getAllActiveAirports();
    Airport addAirport(String code, String name, String city, String country);
    Airport updateAirport(Long id, String code, String name, String city, String country);
    void deleteAirport(Long id);

    // Route Operations
    List<Route> getAllRoutes();
    List<Route> getAllActiveRoutes();
    Route addRoute(Long originId, Long destinationId, double distanceKm, int durationMinutes);
    Route updateRoute(Long id, Long originId, Long destinationId, double distanceKm, int durationMinutes);
    void deleteRoute(Long id);

    // Flight Operations
    List<Flight> getAllFlights();
    List<com.skylanka.airline.entity.FlightHistory> getFlightHistory();
    Optional<Flight> getFlightById(Long id);

    // Overloaded to support both Entity (DataInitializer / services) and DTO (Manager Controller)
    Flight createFlight(Flight flight);
    Flight createFlight(FlightFormDto dto);

    Flight updateFlight(Long id, Flight flight);
    Flight updateFlight(Long id, FlightFormDto dto);

    void updateFlightStatus(Long flightId, FlightStatus status);
    String deleteFlight(Long id);

    List<Flight> searchFlights(FlightSearchDto criteria);

    Flight registerFleetFlight(String flightNumber, int totalSeats, String aircraftType, Long routeId,
                               java.time.LocalDateTime departureTime, java.time.LocalDateTime arrivalTime,
                               Double economyFare, Double businessFare, FlightStatus flightStatus);

    void syncAvailableSeats(Long flightId);

    // Active operational flights & booking verification
    List<Flight> getActiveOperationalFlights();
    java.util.Set<Long> getBookedFlightIds();

    // Old Flight Schedule Automatic Cleanup
    int cleanupOldFlights();
}