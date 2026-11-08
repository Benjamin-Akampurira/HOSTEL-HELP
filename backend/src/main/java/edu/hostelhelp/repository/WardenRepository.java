package edu.hostelhelp.repository;

import edu.hostelhelp.entity.Warden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardenRepository extends JpaRepository<Warden, Long> {
}