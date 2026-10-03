package com.skylanka.airline.entity;

import com.skylanka.airline.enums.CabinClass;
import jakarta.persistence.*;

@Entity
@Table(name = "fares", indexes = {
    @Index(name = "idx_fare_flight", columnList = "flight_id")
})
public class Fare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(name = "fare_type", nullable = false, length = 50)
    private CabinClass fareType;

    @Column(name = "base_fare", nullable = false)
    private double baseFare;

    @Column(nullable = false)
    private double tax;

    public Fare() {}

    public Fare(Flight flight, CabinClass fareType, double baseFare, double tax) {
        this.flight = flight;
        this.fareType = fareType;
        this.baseFare = baseFare;
        this.tax = tax;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public CabinClass getFareType() { return fareType; }
    public void setFareType(CabinClass fareType) { this.fareType = fareType; }

    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotalFare() {
        return baseFare + tax;
    }
}
