package edu.hostelhelp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "students")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("STUDENT")
@lombok.Getter
@lombok.Setter
public class Student extends User {
    @Column(name = "registration_number", nullable = false, unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "hostel_name", nullable = false, length = 120)
    private String hostelName;

    @Column(name = "hostel_block", nullable = false, length = 80)
    private String hostelBlock;

    @Column(name = "room_number", nullable = false, length = 30)
    private String roomNumber;

    @lombok.Setter(lombok.AccessLevel.NONE)
    @Column(name = "trust_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal trustScore = BigDecimal.ZERO;

    public Student() {
    }

    public void updateTrustScore(BigDecimal averageScore) {
        if (averageScore == null || averageScore.compareTo(BigDecimal.ZERO) < 0
                || averageScore.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Trust score must be between 0 and 5.");
        }
        trustScore = averageScore.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String showDashboard() {
        return "STUDENT_OVERVIEW";
    }
}