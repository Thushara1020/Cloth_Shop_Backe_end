package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity, String> {

    @Query("SELECT s FROM SaleEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    List<SaleEntity> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0.0) FROM SaleEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    Double getTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.discountAmount), 0.0) FROM SaleEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    Double getTotalDiscount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM SaleEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    Long getSalesCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}