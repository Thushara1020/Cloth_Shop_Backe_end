package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    SupplierEntity findByNic(String nic);
}