package edu.hostelhelp.repository;

import edu.hostelhelp.entity.Offer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findAllByOrderByOfferedAtDesc();
    List<Offer> findByRequestId(Long requestId);
    List<Offer> findByHelperIdOrderByOfferedAtDesc(Long helperId);
    boolean existsByRequestIdAndHelperId(Long requestId, Long helperId);
}