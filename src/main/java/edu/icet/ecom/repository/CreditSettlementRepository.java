package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.CreditSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditSettlementRepository extends JpaRepository<CreditSettlementEntity, Long> {
    List<CreditSettlementEntity> findByCustomerIdOrderBySettlementDateDesc(Long customerId);
}