package edu.hostelhelp.repository;

import edu.hostelhelp.entity.Report;
import edu.hostelhelp.enums.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}