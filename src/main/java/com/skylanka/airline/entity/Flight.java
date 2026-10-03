package com.skylanka.airline.entity;

import com.skylanka.airline.enums.FlightStatus;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights", indexes = {
    @Index(name = "idx_flight_departure", columnList = "departureTime"),
    @Index(name = "idx_flight_status", columnList = "flightStatus")
})
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String flightNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private String aircraftType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus flightStatus;

    private double economyFare;
    private double businessFare;
    private int totalSeats;
    private int availableSeats;

    @Column(nullable = false)
    private boolean archived = false;

    public Flight() {}

    public Flight(String flightNumber, Route route, LocalDateTime departureTime, LocalDateTime arrivalTime,
                  String aircraftType, FlightStatus flightStatus, double economyFare, double businessFare,
                  int totalSeats, int availableSeats) {
        this.flightNumber = flightNumber;
        this.route = route;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.aircraftType = aircraftType;
        this.flightStatus = flightStatus;
        this.economyFare = economyFare;
        this.businessFare = businessFare;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }

    public Flight(String flightNumber, Route route, LocalDateTime departureTime, LocalDateTime arrivalTime,
                  Aircraft aircraft, FlightStatus flightStatus, double economyFare, double businessFare,
                  int totalSeats, int availableSeats) {
        this.flightNumber = flightNumber;
        this.route = route;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.aircraft = aircraft;
        this.aircraftType = aircraft != null ? aircraft.getModel() : null;
        this.flightStatus = flightStatus;
        this.economyFare = economyFare;
        this.businessFare = businessFare;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
        if (aircraft != null) {
            this.aircraftType = aircraft.getModel();
        }
    }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getAircraftType() {
        if (aircraft != null) {
            return aircraft.getModel();
        }
        return aircraftType;
    }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }

    public FlightStatus getFlightStatus() { return flightStatus; }
    public void setFlightStatus(FlightStatus flightStatus) { this.flightStatus = flightStatus; }

    public double getEconomyFare() { return economyFare; }
    public void setEconomyFare(double economyFare) { this.economyFare = economyFare; }

    public double getBusinessFare() { return businessFare; }
    public void setBusinessFare(double businessFare) { this.businessFare = businessFare; }

    public int getTotalSeats() {
        if (totalSeats <= 0 && aircraft != null) {
            return aircraft.getTotalSeats();
        }
        return totalSeats;
    }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public long getDurationMinutes() {
        if (departureTime != null && arrivalTime != null) {
            return Duration.between(departureTime, arrivalTime).toMinutes();
        }
        if (route != null) {
            return route.getBaseDurationMinutes();
        }
        return 0;
    }
}
