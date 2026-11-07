package edu.hostelhelp.repository;

import edu.hostelhelp.entity.Rating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByBorrowingTransactionId(Long borrowingTransactionId);
    List<Rating> findByHelperIdOrderByCreatedAtDesc(Long helperId);
    List<Rating> findAllByOrderByCreatedAtDesc();
}