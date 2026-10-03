package com.skylanka.airline.repository;

import com.skylanka.airline.entity.FlightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightHistoryRepository extends JpaRepository<FlightHistory, Long> {
    boolean existsByOriginalFlightId(Long originalFlightId);
    java.util.List<FlightHistory> findAllByOrderByArchivedAtDesc();
}
