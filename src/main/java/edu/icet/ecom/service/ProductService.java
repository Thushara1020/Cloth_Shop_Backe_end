package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.enums.ProductUnit;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.dto.ProductVariantDto;
import edu.icet.ecom.model.entity.ProductEntity;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockBatchEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.model.entity.SupplierEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.StockBatchRepository;
import edu.icet.ecom.repository.StockLogRepository;
import edu.icet.ecom.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final StockLogRepository logRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SupplierRepository supplierRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public void saveProduct(ProductDto productDto) {
        ProductEntity entity = modelMapper.map(productDto, ProductEntity.class);

        if (productDto.getUnit() != null) {
            entity.setUnit(ProductUnit.valueOf(productDto.getUnit().toUpperCase()));
        }

        if (productDto.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findById(productDto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + productDto.getSupplierId()));
            entity.setSupplier(supplier);
        }

        if (entity.getCreatedAt() == null) entity.setCreatedAt(LocalDateTime.now());
        if (entity.getUpdatedAt() == null) entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                variant.setProduct(entity);
                variant.setSku(generateSku(entity, variant));
                if (variant.getBarcodeId() == null || variant.getBarcodeId().isEmpty()) {
                    variant.setBarcodeId(generateUniqueBarcode());
                }
            });
        }

        refreshProductMetrics(entity);
        ProductEntity savedEntity = productRepository.save(entity);
        createInitialStockLogs(savedEntity);
    }

    private void createInitialStockLogs(ProductEntity entity) {
        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                StockLogEntity log = new StockLogEntity();
                log.setVariant(variant);
                log.setBarcodeId(variant.getBarcodeId());
                log.setQuantityChanged(variant.getStockQuantity());
                log.setUpdateReason("INITIAL_STOCK_ADD");
                log.setTimestamp(entity.getCreatedAt());
                logRepository.save(log);
            });
        }
    }

    @Transactional
    public void updateProduct(ProductDto productDto) {
        ProductEntity existingEntity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        existingEntity.setUpdatedAt(LocalDateTime.now());

        if (productDto.getProductName() != null) existingEntity.setProductName(productDto.getProductName());
        if (productDto.getCategory() != null) existingEntity.setCategory(productDto.getCategory());
        if (productDto.getWholesalePrice() != null) existingEntity.setWholesalePrice(productDto.getWholesalePrice());
        if (productDto.getRetailPrice() != null) existingEntity.setRetailPrice(productDto.getRetailPrice());

        if (productDto.getUnit() != null) {
            existingEntity.setUnit(ProductUnit.valueOf(productDto.getUnit().toUpperCase()));
        }

        if (productDto.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findById(productDto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + productDto.getSupplierId()));
            existingEntity.setSupplier(supplier);
        }

        List<ProductVariantEntity> newlyAddedVariants = new ArrayList<>();

        if (productDto.getVariants() != null) {
            for (ProductVariantDto vDto : productDto.getVariants()) {
                if (vDto.getVariantId() != null && !vDto.getVariantId().isEmpty()) {
                    existingEntity.getVariants().stream()
                            .filter(v -> v.getVariantId().equals(vDto.getVariantId()))
                            .findFirst()
                            .ifPresent(existingVar -> modelMapper.map(vDto, existingVar));
                } else {
                    ProductVariantEntity newVar = modelMapper.map(vDto, ProductVariantEntity.class);
                    newVar.setProduct(existingEntity);
                    newVar.setSku(generateSku(existingEntity, newVar));
                    if (newVar.getBarcodeId() == null || newVar.getBarcodeId().isEmpty()) {
                        newVar.setBarcodeId(generateUniqueBarcode());
                    }
                    ProductVariantEntity savedVar = variantRepository.save(newVar);
                    newlyAddedVariants.add(savedVar);
                }
            }
        }

        refreshProductMetrics(existingEntity);
        productRepository.saveAndFlush(existingEntity);

        for (ProductVariantEntity savedVar : newlyAddedVariants) {
            StockLogEntity log = new StockLogEntity();
            log.setVariant(savedVar);
            log.setBarcodeId(savedVar.getBarcodeId());
            log.setQuantityChanged(savedVar.getStockQuantity());
            log.setUpdateReason("NEW_VARIANT_ADDED");
            log.setTimestamp(LocalDateTime.now());
            logRepository.save(log);
        }
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Integer id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + id + " not found"));
        return convertToDto(entity);
    }

    private ProductDto convertToDto(ProductEntity entity) {
        ProductDto dto = modelMapper.map(entity, ProductDto.class);

        if (entity.getUnit() != null) {
            dto.setUnit(entity.getUnit().name());
        }

        if (entity.getSupplier() != null) {
            dto.setSupplierId(entity.getSupplier().getId());
            dto.setSupplierName(entity.getSupplier().getName());
        }

        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            dto.setAvailableSizes(entity.getVariants().stream()
                    .map(ProductVariantEntity::getDimensions).distinct().toList());
            dto.setAvailableColors(entity.getVariants().stream()
                    .map(ProductVariantEntity::getMaterialOrType).distinct().toList());
            dto.setTotalQuantity(entity.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum());

            if (entity.getRetailPrice() != null && entity.getDiscountPercentage() != null) {
                dto.setDiscountedPrice(entity.getRetailPrice() * (1 - (entity.getDiscountPercentage() / 100)));
            }
        }
        return dto;
    }

    private void refreshProductMetrics(ProductEntity entity) {
        int totalQty = (entity.getVariants() != null)
                ? entity.getVariants().stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum()
                : 0;

        if (totalQty <= 0) entity.setStockStatus(StockStatus.OUT_OF_STOCK);
        else if (totalQty < 10) entity.setStockStatus(StockStatus.LOW_STOCK);
        else entity.setStockStatus(StockStatus.AVAILABLE);
    }

    private String generateSku(ProductEntity p, ProductVariantEntity v) {
        String cat = (p.getCategory() != null)
                ? p.getCategory().substring(0, Math.min(p.getCategory().length(), 3)) : "GEN";
        String name = (p.getProductName() != null)
                ? p.getProductName().substring(0, Math.min(p.getProductName().length(), 3)) : "PRD";
        return (cat + "-" + name + "-" + (v.getDimensions() != null ? v.getMaterialOrType() : "NA"))
                .toUpperCase().replace(" ", "");
    }

    private String generateUniqueBarcode() {
        String prefix = "479";
        String company = "8000";

        int random = (int) (Math.random() * 100000);
        String productPart = String.format("%05d", random);

        String base = prefix + company + productPart;

        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            int digit = Character.getNumericValue(base.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return base + checkDigit;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getVariants() != null) {
            for (ProductVariantEntity variant : product.getVariants()) {
                logRepository.deleteByVariant(variant);
            }
        }

        productRepository.delete(product);
    }

    public List<Map<String, Object>> getBarcodesAndPrices() {
        return variantRepository.findAll().stream()
                .map(variant -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("barcodeId", variant.getBarcodeId() != null ? variant.getBarcodeId() : "N/A");

                    List<StockBatchEntity> openBatches = stockBatchRepository
                            .findByBarcodeIdOrderByRestockDateAsc(variant.getBarcodeId())
                            .stream()
                            .filter(b -> b.getQuantityRemaining() != null && b.getQuantityRemaining() > 0)
                            .collect(Collectors.toList());

                    Double price;
                    if (!openBatches.isEmpty()) {
                        price = openBatches.get(0).getBatchPrice();
                    } else {
                        price = (variant.getPriceOverride() != null)
                                ? variant.getPriceOverride()
                                : variant.getProduct().getRetailPrice();
                    }

                    map.put("price", price != null ? price : 0.0);
                    return map;
                })
                .toList();
    }

    @Transactional
    public void updatePriceByBarcode(String barcode, Double price) {
        ProductVariantEntity variant = variantRepository.findByBarcodeId(barcode)
                .orElseThrow(() -> new RuntimeException("Barcode " + barcode + " not found"));

        variant.setPriceOverride(price);
        variantRepository.save(variant);

        List<StockBatchEntity> openBatches = stockBatchRepository
                .findByBarcodeIdOrderByRestockDateAsc(barcode)
                .stream()
                .filter(b -> b.getQuantityRemaining() != null && b.getQuantityRemaining() > 0)
                .collect(Collectors.toList());

        for (StockBatchEntity batch : openBatches) {
            batch.setBatchPrice(price);
            stockBatchRepository.save(batch);
        }
    }
}