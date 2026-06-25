package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity, String> {

    // IMPORTANT: Remove findByTimestampStartingWith. Use this instead:
    @Query("SELECT s FROM SaleEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    List<SaleEntity> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}