package com.skylanka.airline.repository;

import com.skylanka.airline.entity.Airport;
import com.skylanka.airline.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByOriginAndDestination(Airport origin, Airport destination);
    boolean existsByOriginAndDestination(Airport origin, Airport destination);
    List<Route> findByActiveTrue();
    Optional<Route> findByOriginAndDestinationAndActiveTrue(Airport origin, Airport destination);
    boolean existsByOriginAndDestinationAndActiveTrue(Airport origin, Airport destination);
}
