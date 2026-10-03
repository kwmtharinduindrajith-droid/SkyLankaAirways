package com.skylanka.airline.repository;

import com.skylanka.airline.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByModel(String model);
    boolean existsByModel(String model);
    List<Aircraft> findByManufacturer(String manufacturer);
    
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    List<Aircraft> findByActiveTrue();
}
