package com.skylanka.airline.scheduler;

import com.skylanka.airline.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Configuration
@EnableScheduling
public class FlightCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(FlightCleanupTask.class);

    @Autowired
    private FlightService flightService;

    // Run every 30 minutes
    @Scheduled(fixedRate = 1800000)
    public void scheduleFlightCleanup() {
        logger.info("Running automatic flight cleanup task...");
        try {
            int archivedCount = flightService.cleanupOldFlights();
            if (archivedCount > 0) {
                logger.info("Successfully archived {} completed flight(s).", archivedCount);
            }
        } catch (Exception e) {
            logger.error("Error during flight cleanup task: ", e);
        }
    }
}
