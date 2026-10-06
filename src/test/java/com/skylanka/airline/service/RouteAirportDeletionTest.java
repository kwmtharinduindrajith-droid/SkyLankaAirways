package com.skylanka.airline.service;

import com.skylanka.airline.dto.FlightSearchDto;
import com.skylanka.airline.entity.*;
import com.skylanka.airline.enums.CabinClass;
import com.skylanka.airline.enums.FlightStatus;
import com.skylanka.airline.repository.*;
import com.skylanka.airline.service.impl.FlightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RouteAirportDeletionTest {

    @Mock private AircraftRepository aircraftRepository;
    @Mock private AirportRepository airportRepository;
    @Mock private RouteRepository routeRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private FlightServiceImpl flightService;

    private Airport cmb;
    private Airport sin;
    private Airport dxb;
    private Route routeCmbSin;
    private Route routeCmbDxb;
    private Aircraft a330;

    @BeforeEach
    void setUp() {
        cmb = new Airport("CMB", "Bandaranaike Intl Airport", "Colombo", "Sri Lanka", true);
        cmb.setId(1L);
        sin = new Airport("SIN", "Singapore Changi Airport", "Singapore", "Singapore", true);
        sin.setId(2L);
        dxb = new Airport("DXB", "Dubai Intl Airport", "Dubai", "UAE", true);
        dxb.setId(3L);

        routeCmbSin = new Route(cmb, sin, 2750.0, 240, true);
        routeCmbSin.setId(10L);

        routeCmbDxb = new Route(cmb, dxb, 3290.0, 280, true);
        routeCmbDxb.setId(20L);

        a330 = new Aircraft("Airbus A330-300", "Airbus", 60);
        a330.setId(100L);
    }

    @Test
    @DisplayName("Scenario 1 & 2 & 4: Route with ONLY historical DEPARTED flight is deactivated, preserving flight and FK data")
    void testDeactivateRouteWithHistoricalDepartedFlight() {
        LocalDateTime pastDeparture = LocalDateTime.now().minusDays(10);
        Flight historicalFlight = new Flight("UL-302", routeCmbSin, pastDeparture, pastDeparture.plusHours(4), a330, FlightStatus.DEPARTED, 350.0, 850.0, 60, 0);
        historicalFlight.setId(302L);

        when(routeRepository.findById(10L)).thenReturn(Optional.of(routeCmbSin));
        when(flightRepository.findAll()).thenReturn(List.of(historicalFlight));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Attempt deletion/deactivation of Route 10
        flightService.deleteRoute(10L);

        // Verification
        assertFalse(routeCmbSin.isActive(), "Route should be marked inactive (soft deleted)");
        verify(routeRepository, never()).delete(any(Route.class)); // Physical delete NOT called
        verify(routeRepository).save(routeCmbSin);

        // Scenario 4: Historical flight still references CMB -> SIN correctly
        assertNotNull(historicalFlight.getRoute());
        assertEquals("CMB", historicalFlight.getRoute().getOrigin().getAirportCode());
        assertEquals("SIN", historicalFlight.getRoute().getDestination().getAirportCode());
        assertEquals(FlightStatus.DEPARTED, historicalFlight.getFlightStatus());
    }

    @Test
    @DisplayName("Scenario 5: Route with future SCHEDULED flight blocks deletion")
    void testBlockRouteDeletionWithFutureScheduledFlight() {
        LocalDateTime futureDeparture = LocalDateTime.now().plusDays(2);
        Flight futureFlight = new Flight("UL-225", routeCmbDxb, futureDeparture, futureDeparture.plusHours(4), a330, FlightStatus.SCHEDULED, 450.0, 950.0, 60, 60);
        futureFlight.setId(225L);

        when(routeRepository.findById(20L)).thenReturn(Optional.of(routeCmbDxb));
        when(flightRepository.findAll()).thenReturn(List.of(futureFlight));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> flightService.deleteRoute(20L));
        assertEquals("Cannot remove route because future scheduled flights are operating on it.", ex.getMessage());
        assertTrue(routeCmbDxb.isActive(), "Route should remain active");
    }

    @Test
    @DisplayName("Scenario 3: Inactive route is excluded from active routes list")
    void testInactiveRouteExcludedFromActiveRoutes() {
        routeCmbSin.setActive(false);
        when(routeRepository.findByActiveTrue()).thenReturn(List.of(routeCmbDxb));

        List<Route> activeRoutes = flightService.getAllActiveRoutes();
        assertEquals(1, activeRoutes.size());
        assertEquals(20L, activeRoutes.get(0).getId());
        assertFalse(activeRoutes.contains(routeCmbSin));
    }

    @Test
    @DisplayName("Scenario 6: Airport with historical routes/flights is deactivated rather than physically deleted")
    void testDeactivateAirportWithHistoricalDependencies() {
        routeCmbSin.setActive(false); // Historical route
        when(airportRepository.findById(2L)).thenReturn(Optional.of(sin));
        when(routeRepository.findAll()).thenReturn(List.of(routeCmbSin));
        when(airportRepository.save(any(Airport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        flightService.deleteAirport(2L);

        assertFalse(sin.isActive(), "Airport should be deactivated to preserve historical references");
        verify(airportRepository, never()).deleteById(anyLong());
        verify(airportRepository).save(sin);
    }

    @Test
    @DisplayName("Scenario 6b: Airport with ACTIVE routes blocks deletion")
    void testBlockAirportDeletionWithActiveRoutes() {
        routeCmbSin.setActive(true); // Active route
        when(airportRepository.findById(1L)).thenReturn(Optional.of(cmb));
        when(routeRepository.findAll()).thenReturn(List.of(routeCmbSin, routeCmbDxb));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> flightService.deleteAirport(1L));
        assertTrue(ex.getMessage().contains("Cannot delete airport CMB because active flight routes are linked to it."));
        assertTrue(cmb.isActive());
    }

    @Test
    @DisplayName("Scenario D: Completely unused route without flights can be physically deleted")
    void testPhysicalDeleteUnusedRoute() {
        when(routeRepository.findById(10L)).thenReturn(Optional.of(routeCmbSin));
        when(flightRepository.findAll()).thenReturn(Collections.emptyList());

        flightService.deleteRoute(10L);

        verify(routeRepository).delete(routeCmbSin);
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    @DisplayName("Scenario 8: Search flights does not return flights on inactive routes or past flights")
    void testSearchFlightsExcludesInactiveRoutesAndPastFlights() {
        LocalDateTime now = LocalDateTime.now();
        
        // Past flight (should be excluded)
        Flight pastFlight = new Flight("UL-301", routeCmbSin, now.minusDays(2), now.minusDays(2).plusHours(4), a330, FlightStatus.DEPARTED, 300.0, 700.0, 60, 0);
        
        // Future flight on inactive route (should be excluded)
        Route inactiveRoute = new Route(cmb, sin, 2750.0, 240, false);
        Flight futureInactiveFlight = new Flight("UL-303", inactiveRoute, now.plusDays(1), now.plusDays(1).plusHours(4), a330, FlightStatus.SCHEDULED, 350.0, 850.0, 60, 50);

        // Future flight on active route (should be included)
        Flight futureActiveFlight = new Flight("UL-225", routeCmbDxb, now.plusDays(1), now.plusDays(1).plusHours(4), a330, FlightStatus.SCHEDULED, 400.0, 900.0, 60, 50);

        when(flightRepository.findAll()).thenReturn(List.of(pastFlight, futureInactiveFlight, futureActiveFlight));

        FlightSearchDto searchDto = new FlightSearchDto();
        List<Flight> results = flightService.searchFlights(searchDto);

        assertEquals(1, results.size());
        assertEquals("UL-225", results.get(0).getFlightNumber());
    }

    @Test
    @DisplayName("Fleet Registry: getActiveOperationalFlights excludes past/departed/arrived/cancelled flights")
    void testGetActiveOperationalFlightsExcludesHistorical() {
        LocalDateTime now = LocalDateTime.now();
        Flight departedFlight = new Flight("UL-302", routeCmbSin, now.minusDays(5), now.minusDays(5).plusHours(4), a330, FlightStatus.DEPARTED, 350.0, 850.0, 60, 0);
        Flight arrivedFlight = new Flight("UL-222", routeCmbSin, now.minusDays(3), now.minusDays(3).plusHours(4), a330, FlightStatus.ARRIVED, 350.0, 850.0, 60, 0);
        Flight cancelledFlight = new Flight("UL-999", routeCmbSin, now.plusDays(1), now.plusDays(1).plusHours(4), a330, FlightStatus.CANCELLED, 350.0, 850.0, 60, 0);
        Flight futureScheduled = new Flight("UL-503", routeCmbDxb, now.plusDays(2), now.plusDays(2).plusHours(4), a330, FlightStatus.SCHEDULED, 450.0, 950.0, 60, 60);
        Flight futureDelayed = new Flight("UL-101", routeCmbDxb, now.plusHours(5), now.plusHours(9), a330, FlightStatus.DELAYED, 400.0, 900.0, 60, 45);

        when(flightRepository.findAll()).thenReturn(List.of(departedFlight, arrivedFlight, cancelledFlight, futureScheduled, futureDelayed));

        List<Flight> activeFlights = flightService.getActiveOperationalFlights();

        assertEquals(2, activeFlights.size());
        assertTrue(activeFlights.stream().anyMatch(f -> f.getFlightNumber().equals("UL-503")));
        assertTrue(activeFlights.stream().anyMatch(f -> f.getFlightNumber().equals("UL-101")));
        assertFalse(activeFlights.stream().anyMatch(f -> f.getFlightNumber().equals("UL-302")));
        assertFalse(activeFlights.stream().anyMatch(f -> f.getFlightNumber().equals("UL-222")));
        assertFalse(activeFlights.stream().anyMatch(f -> f.getFlightNumber().equals("UL-999")));
    }

    @Test
    @DisplayName("Fleet Registry: getBookedFlightIds correctly identifies flights with bookings")
    void testGetBookedFlightIds() {
        Flight f1 = new Flight(); f1.setId(101L);
        Flight f2 = new Flight(); f2.setId(102L);
        
        Booking b1 = new Booking(); b1.setFlight(f1);
        Booking b2 = new Booking(); b2.setFlight(f1);

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2));

        Set<Long> bookedIds = flightService.getBookedFlightIds();
        assertEquals(1, bookedIds.size());
        assertTrue(bookedIds.contains(101L));
        assertFalse(bookedIds.contains(102L));
    }
}
