package edu.hostelhelp.repository;

import edu.hostelhelp.entity.BorrowRequest;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    List<BorrowRequest> findAllByOrderByCreatedAtDesc();
    List<BorrowRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from BorrowRequest request where request.id = :id")
    Optional<BorrowRequest> findLockedById(@Param("id") Long id);
}