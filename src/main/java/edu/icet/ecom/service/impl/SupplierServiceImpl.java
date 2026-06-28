package edu.icet.ecom.service.impl;

import edu.icet.ecom.model.dto.SupplierDto;
import edu.icet.ecom.model.entity.SupplierEntity;
import edu.icet.ecom.repository.SupplierRepository;
import edu.icet.ecom.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public void saveSupplier(SupplierDto supplierDto) {
        SupplierEntity entity = new SupplierEntity();
        entity.setName(supplierDto.getName());
        entity.setPhone(supplierDto.getPhone());
        entity.setNic(supplierDto.getNic());
        entity.setAddress(supplierDto.getAddress());

        supplierRepository.save(entity);
    }

    @Override
    public List<SupplierDto> getAllSuppliers() {
        List<SupplierEntity> entities = supplierRepository.findAll();
        List<SupplierDto> dtoList = new ArrayList<>();

        for (SupplierEntity entity : entities) {
            SupplierDto dto = new SupplierDto();
            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setPhone(entity.getPhone());
            dto.setNic(entity.getNic());
            dto.setAddress(entity.getAddress());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public SupplierDto getSupplierById(Long id) {
        Optional<SupplierEntity> optionalEntity = supplierRepository.findById(id);
        if (optionalEntity.isPresent()) {
            SupplierEntity entity = optionalEntity.get();
            SupplierDto dto = new SupplierDto();
            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setPhone(entity.getPhone());
            dto.setNic(entity.getNic());
            dto.setAddress(entity.getAddress());
            return dto;
        }
        return null;
    }

    @Override
    public void updateSupplier(SupplierDto supplierDto) {
        Optional<SupplierEntity> optionalEntity = supplierRepository.findById(supplierDto.getId());
        if (optionalEntity.isPresent()) {
            SupplierEntity entity = optionalEntity.get();
            entity.setName(supplierDto.getName());
            entity.setPhone(supplierDto.getPhone());
            entity.setNic(supplierDto.getNic());
            entity.setAddress(supplierDto.getAddress());

            supplierRepository.save(entity);
        }
    }

    @Override
    public void deleteSupplier(Long id) {
        if (supplierRepository.existsById(id)) {
            supplierRepository.deleteById(id);
        }
    }
}