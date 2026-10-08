package com.skylanka.airline.dto;

import com.skylanka.airline.entity.Flight;
import com.skylanka.airline.enums.FlightStatus;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class FlightFormDto {
    private Long id;

    @NotBlank(message = "Flight number is required (e.g. UL-301)")
    @Pattern(regexp = "^[A-Za-z]{2}-[0-9]{3,4}$", message = "Flight number must match format: 2 letters, hyphen, 3-4 digits (e.g. UL-301, EK-505)")
    private String flightNumber;

    @NotNull(message = "Flight route must be selected")
    private Long routeId;

    @NotNull(message = "Departure time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime arrivalTime;

    @NotBlank(message = "Aircraft type is required")
    private String aircraftType = "Airbus A330-300";

    private FlightStatus flightStatus = FlightStatus.SCHEDULED;

    @NotNull(message = "Economy base fare is required")
    @DecimalMin(value = "50.0", message = "Economy base fare must be at least $50.00")
    @DecimalMax(value = "10000.0", message = "Economy base fare cannot exceed $10,000.00")
    private Double economyFare = 350.0;

    @NotNull(message = "Business base fare is required")
    @DecimalMin(value = "100.0", message = "Business base fare must be at least $100.00")
    @DecimalMax(value = "25000.0", message = "Business base fare cannot exceed $25,000.00")
    private Double businessFare = 850.0;

    @Min(value = 20, message = "Total seat capacity must be at least 20")
    @Max(value = 500, message = "Total seat capacity cannot exceed 500")
    private int totalSeats = 60;

    public FlightFormDto() {}

    public FlightFormDto(Flight flight) {
        if (flight != null) {
            this.id = flight.getId();
            this.flightNumber = flight.getFlightNumber();
            if (flight.getRoute() != null) {
                this.routeId = flight.getRoute().getId();
            }
            this.departureTime = flight.getDepartureTime();
            this.arrivalTime = flight.getArrivalTime();
            this.aircraftType = flight.getAircraftType();
            this.flightStatus = flight.getFlightStatus();
            this.economyFare = flight.getEconomyFare();
            this.businessFare = flight.getBusinessFare();
            this.totalSeats = flight.getTotalSeats();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }
    public FlightStatus getFlightStatus() { return flightStatus; }
    public void setFlightStatus(FlightStatus flightStatus) { this.flightStatus = flightStatus; }
    public Double getEconomyFare() { return economyFare; }
    public void setEconomyFare(Double economyFare) { this.economyFare = economyFare; }
    public Double getBusinessFare() { return businessFare; }
    public void setBusinessFare(Double businessFare) { this.businessFare = businessFare; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
}
