package com.skylanka.airline.scheduler;

import com.skylanka.airline.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FlightCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FlightCleanupScheduler.class);

    @Autowired
    private FlightService flightService;

    /**
     * Daily automatic cleanup of old flight schedules whose departureTime is in the past.
     * Runs once per day at 02:00 AM server time.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyFlightCleanup() {
        logger.info("[FlightCleanupScheduler] Running scheduled daily old-flight cleanup...");
        int cleaned = flightService.cleanupOldFlights();
        logger.info("[FlightCleanupScheduler] Scheduled cleanup complete. Obsolete flights removed: {}", cleaned);
    }
}
