package edu.hostelhelp.repository;

import edu.hostelhelp.entity.BorrowingTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowingTransactionRepository extends JpaRepository<BorrowingTransaction, Long> {
	List<BorrowingTransaction> findAllByOrderByCreatedAtDesc();
	List<BorrowingTransaction> findByBorrowerIdOrHelperIdOrderByCreatedAtDesc(Long borrowerId, Long helperId);
}