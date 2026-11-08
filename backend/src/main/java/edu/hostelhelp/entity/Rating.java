package edu.hostelhelp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "ratings")
@lombok.Getter
@lombok.Setter
public class Rating implements Rateable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Student borrower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "helper_id", nullable = false)
    private Student helper;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_transaction_id", nullable = false, unique = true)
    private BorrowingTransaction borrowingTransaction;

    @Column(nullable = false)
    private short score;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public Rating() {
    }

    @Override
    public short getScore() {
        return score;
    }
}