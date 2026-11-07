package edu.hostelhelp.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StudentTest {
    @Test
    void updatesTrustScoreWithTwoDecimalPrecision() {
        Student student = new Student();

        student.updateTrustScore(new BigDecimal("4.256"));

        assertEquals(new BigDecimal("4.26"), student.getTrustScore());
    }

    @Test
    void rejectsTrustScoreOutsideAllowedRange() {
        Student student = new Student();

        assertThrows(IllegalArgumentException.class,
                () -> student.updateTrustScore(new BigDecimal("5.1")));
    }
}