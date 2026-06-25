package edu.icet.ecom.service.impl;

import edu.icet.ecom.model.dto.SupplierDto;
import edu.icet.ecom.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    @Override
    public void saveSupplier(SupplierDto supplierDto) {

    }

    @Override
    public List<SupplierDto> getAllSuppliers() {
        return List.of();
    }

    @Override
    public SupplierDto getSupplierById(Long id) {
        return null;
    }

    @Override
    public void updateSupplier(SupplierDto supplierDto) {

    }

    @Override
    public void deleteSupplier(Long id) {

    }
}