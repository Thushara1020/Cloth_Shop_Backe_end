package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockLogRepository extends JpaRepository<StockLogEntity, Long> {
    List<StockLogEntity> findAllByOrderByLogIdDesc();

    // Replace the 'LIKE' query with a range query
    @Query("SELECT s FROM StockLogEntity s WHERE s.timestamp >= :start AND s.timestamp < :end")
    List<StockLogEntity> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Transactional
    void deleteByVariant(ProductVariantEntity variant);
}