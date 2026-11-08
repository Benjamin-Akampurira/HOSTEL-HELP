package edu.hostelhelp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "wardens")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("WARDEN")
@lombok.Getter
@lombok.Setter
public class Warden extends User {
    @Column(name = "staff_number", unique = true, length = 50)
    private String staffNumber;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    public Warden() {
    }

    @Override
    public String showDashboard() {
        return "WARDEN_REPORTS";
    }
}