package com.skylanka.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "airports")
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String airportCode;

    @Column(nullable = false)
    private String airportName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false, columnDefinition = "BIT DEFAULT 1")
    private boolean active = true;

    public Airport() {}
    public Airport(String airportCode, String airportName, String city, String country) {
        this.airportCode = airportCode;
        this.airportName = airportName;
        this.city = city;
        this.country = country;
        this.active = true;
    }
    public Airport(String airportCode, String airportName, String city, String country, boolean active) {
        this.airportCode = airportCode;
        this.airportName = airportName;
        this.city = city;
        this.country = country;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAirportCode() { return airportCode; }
    public void setAirportCode(String airportCode) { this.airportCode = airportCode; }
    public String getAirportName() { return airportName; }
    public String getName() { return airportName; }
    public String getCode() { return airportCode; }
    public void setAirportName(String airportName) { this.airportName = airportName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
