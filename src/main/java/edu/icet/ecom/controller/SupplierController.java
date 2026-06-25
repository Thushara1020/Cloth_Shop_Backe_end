package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.SupplierDto;
import edu.icet.ecom.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin()
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/add")
    public ResponseEntity<String> addSupplier(@RequestBody SupplierDto supplierDto) {
        supplierService.saveSupplier(supplierDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Supplier added successfully");
    }

    @GetMapping("/all")
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateSupplier(@PathVariable Long id, @RequestBody SupplierDto supplierDto) {
        supplierDto.setId(id);
        supplierService.updateSupplier(supplierDto);
        return ResponseEntity.ok("Supplier ID: " + id + " updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok("Supplier deleted successfully");
    }
}