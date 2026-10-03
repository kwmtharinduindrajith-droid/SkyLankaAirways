package com.skylanka.airline.repository;

import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightNumber(String flightNumber);
    boolean existsByFlightNumber(String flightNumber);
    boolean existsByAircraft(com.skylanka.airline.entity.Aircraft aircraft);
    List<Flight> findByRouteAndDepartureTimeBetween(Route route, LocalDateTime start, LocalDateTime end);
}
