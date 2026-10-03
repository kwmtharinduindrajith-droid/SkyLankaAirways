package com.skylanka.airline.repository;

import com.skylanka.airline.entity.Fare;
import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.enums.CabinClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRepository extends JpaRepository<Fare, Long> {
    List<Fare> findByFlight(Flight flight);
    List<Fare> findByFlight_Id(Long flightId);
    Optional<Fare> findByFlightAndFareType(Flight flight, CabinClass fareType);
    void deleteByFlight(Flight flight);
}
