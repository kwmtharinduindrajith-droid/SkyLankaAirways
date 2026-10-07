package com.skylanka.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "passport_no", length = 50)
    private String passportNo;

    @Column(length = 100)
    private String nationality;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints = 0;

    public Passenger() {}

    public Passenger(User user, String passportNo, String nationality, int loyaltyPoints) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.passportNo = passportNo;
        this.nationality = nationality;
        this.loyaltyPoints = loyaltyPoints;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
}
