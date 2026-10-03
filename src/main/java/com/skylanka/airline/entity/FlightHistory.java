package com.skylanka.airline.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_history")
public class FlightHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long originalFlightId;

    @Column(nullable = false, length = 20)
    private String flightNumber;

    private String origin;
    private String destination;
    private String aircraftRegistration;
    private String aircraftModel;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private String finalStatus;
    private int totalSeats;
    private int availableSeats;
    private double economyFare;
    private double businessFare;

    @Column(nullable = false)
    private LocalDateTime archivedAt;

    public FlightHistory() {}

    public FlightHistory(Long originalFlightId, String flightNumber, String origin, String destination, 
                         String aircraftRegistration, String aircraftModel, LocalDateTime departureTime, 
                         LocalDateTime arrivalTime, String finalStatus, int totalSeats, int availableSeats, 
                         double economyFare, double businessFare, LocalDateTime archivedAt) {
        this.originalFlightId = originalFlightId;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.aircraftRegistration = aircraftRegistration;
        this.aircraftModel = aircraftModel;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.finalStatus = finalStatus;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.economyFare = economyFare;
        this.businessFare = businessFare;
        this.archivedAt = archivedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOriginalFlightId() { return originalFlightId; }
    public void setOriginalFlightId(Long originalFlightId) { this.originalFlightId = originalFlightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getAircraftRegistration() { return aircraftRegistration; }
    public void setAircraftRegistration(String aircraftRegistration) { this.aircraftRegistration = aircraftRegistration; }

    public String getAircraftModel() { return aircraftModel; }
    public void setAircraftModel(String aircraftModel) { this.aircraftModel = aircraftModel; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getEconomyFare() { return economyFare; }
    public void setEconomyFare(double economyFare) { this.economyFare = economyFare; }

    public double getBusinessFare() { return businessFare; }
    public void setBusinessFare(double businessFare) { this.businessFare = businessFare; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}
