package com.skylanka.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "origin_airport_id")
    private Airport origin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_airport_id")
    private Airport destination;

    private double distanceKm;
    private int baseDurationMinutes;

    @Column(nullable = false, columnDefinition = "BIT DEFAULT 1")
    private boolean active = true;

    public Route() {}

    public Route(Airport origin, Airport destination, double distanceKm, int baseDurationMinutes) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.baseDurationMinutes = baseDurationMinutes;
        this.active = true;
    }

    public Route(Airport origin, Airport destination, double distanceKm, int baseDurationMinutes, boolean active) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.baseDurationMinutes = baseDurationMinutes;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }
    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public int getBaseDurationMinutes() { return baseDurationMinutes; }
    public int getDurationMinutes() { return baseDurationMinutes; }
    public void setBaseDurationMinutes(int baseDurationMinutes) { this.baseDurationMinutes = baseDurationMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
