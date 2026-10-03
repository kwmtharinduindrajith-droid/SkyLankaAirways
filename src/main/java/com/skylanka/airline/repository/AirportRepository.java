package com.skylanka.airline.repository;

import com.skylanka.airline.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByAirportCode(String airportCode);
    boolean existsByAirportCode(String airportCode);
    List<Airport> findByActiveTrue();
    Optional<Airport> findByAirportCodeAndActiveTrue(String airportCode);
}
