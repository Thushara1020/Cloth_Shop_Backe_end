package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.SupplierDto;
import java.util.List;

public interface SupplierService {
    void saveSupplier(SupplierDto supplierDto);
    List<SupplierDto> getAllSuppliers();
    SupplierDto getSupplierById(Long id);
    void updateSupplier(SupplierDto supplierDto);
    void deleteSupplier(Long id);
}