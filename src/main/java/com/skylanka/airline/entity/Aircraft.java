package com.skylanka.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "aircrafts")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String model;

    @Column(nullable = false, length = 100)
    private String manufacturer;

    @Column(nullable = false)
    private int totalSeats;

    @Column(unique = true, length = 50)
    private String registrationNumber;

    @Column(nullable = false)
    private int economySeats = 0;

    @Column(nullable = false)
    private int businessSeats = 0;

    @Column(nullable = false)
    private boolean active = true;

    public Aircraft() {}

    public Aircraft(String model, String manufacturer, int totalSeats) {
        this.model = model;
        this.manufacturer = manufacturer;
        this.totalSeats = totalSeats;
        this.registrationNumber = "REG-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.active = true;
        this.businessSeats = 8; // Default split for DataInitializer backward compatibility
        this.economySeats = totalSeats > 8 ? totalSeats - 8 : totalSeats;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public int getEconomySeats() { return economySeats; }
    public void setEconomySeats(int economySeats) { this.economySeats = economySeats; }

    public int getBusinessSeats() { return businessSeats; }
    public void setBusinessSeats(int businessSeats) { this.businessSeats = businessSeats; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
