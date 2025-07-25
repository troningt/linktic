package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.InventoryDto;
import com.example.inventory.controller.dto.ProductDto;
import com.example.inventory.controller.dto.UpdateInventoryRequestDto;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.InventoryMovement;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.InventoryMovementRepository;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.KafkaProducerService;
import com.example.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de inventario
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryServiceImpl implements InventoryService {

  private final InventoryRepository inventoryRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final ProductService productService;
  private final KafkaProducerService kafkaProducerService;

  @Override
  @Transactional(readOnly = true)
  public InventoryDto getInventoryByProductId(Long productId) {
    log.debug("Getting inventory for product ID: {}", productId);

    Inventory inventory = inventoryRepository.findByProductId(productId)
        .orElseThrow(() -> new ProductNotFoundException("Inventory not found for product ID: " + productId));

    return mapToDto(inventory);
  }

  @Override
  public InventoryDto updateInventory(Long productId, UpdateInventoryRequestDto request) {
    log.info("Updating inventory for product ID: {} with quantity: {}", productId, request.getQuantity());

    Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
        .orElseThrow(() -> new ProductNotFoundException("Inventory not found for product ID: " + productId));

    Integer previousQuantity = inventory.getQuantity();

    // Actualizar campos
    inventory.setQuantity(request.getQuantity());
    if (request.getMinStockLevel() != null) {
      inventory.setMinStockLevel(request.getMinStockLevel());
    }
    if (request.getMaxStockLevel() != null) {
      inventory.setMaxStockLevel(request.getMaxStockLevel());
    }

    // Validar que la cantidad reservada no exceda la nueva cantidad
    if (inventory.getReservedQuantity() > request.getQuantity()) {
      throw new InsufficientStockException(
          "Cannot set quantity below reserved amount. Reserved: " + inventory.getReservedQuantity() +
              ", New quantity: " + request.getQuantity());
    }

    Inventory savedInventory = inventoryRepository.save(inventory);

    // Registrar movimiento de inventario
    InventoryMovement movement = InventoryMovement.createAdjustmentMovement(
        productId, previousQuantity, request.getQuantity(),
        request.getReason() != null ? request.getReason() : "Manual inventory update",
        request.getReferenceId());
    inventoryMovementRepository.save(movement);

    // Publicar eventos
    kafkaProducerService.publishInventoryUpdatedEvent(savedInventory);

    if (savedInventory.isLowStock()) {
      kafkaProducerService.publishLowStockEvent(savedInventory);
    }

    if (savedInventory.isOutOfStock()) {
      kafkaProducerService.publishOutOfStockEvent(savedInventory);
    }

    log.info("Inventory updated successfully for product ID: {}", productId);
    return mapToDto(savedInventory);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InventoryDto> getAllInventories(Pageable pageable) {
    log.debug("Getting all inventories with pagination: page={}, size={}",
        pageable.getPageNumber(), pageable.getPageSize());

    Page<Inventory> inventories = inventoryRepository.findAllByOrderByUpdatedAtDesc(pageable);
    return inventories.map(this::mapToDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InventoryDto> getInventoriesWithFilters(Boolean hasStock, Boolean isLowStock,
                                                      Integer minQuantity, Integer maxQuantity,
                                                      Pageable pageable) {
    log.debug("Getting inventories with filters - hasStock: {}, isLowStock: {}, minQuantity: {}, maxQuantity: {}",
        hasStock, isLowStock, minQuantity, maxQuantity);

    Page<Inventory> inventories = inventoryRepository.findByFilters(
        hasStock, isLowStock, minQuantity, maxQuantity, pageable);

    return inventories.map(this::mapToDto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<InventoryDto> getLowStockProducts() {
    log.debug("Getting low stock products");

    List<Inventory> lowStockInventories = inventoryRepository.findLowStockProducts();
    return lowStockInventories.stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<InventoryDto> getOutOfStockProducts() {
    log.debug("Getting out of stock products");

    List<Inventory> outOfStockInventories = inventoryRepository.findOutOfStockProducts();
    return outOfStockInventories.stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean checkStockAvailability(Long productId, Integer quantity) {
    log.debug("Checking stock availability for product ID: {} and quantity: {}", productId, quantity);

    Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

    if (inventoryOpt.isEmpty()) {
      log.warn("No inventory found for product ID: {}", productId);
      return false;
    }

    Inventory inventory = inventoryOpt.get();
    boolean hasStock = inventory.hasAvailableStock(quantity);

    log.debug("Stock availability check result for product {}: {} (available: {}, required: {})",
        productId, hasStock, inventory.getAvailableQuantity(), quantity);

    return hasStock;
  }

  @Override
  public boolean reserveStock(Long productId, Integer quantity) {
    log.info("Reserving stock for product ID: {} and quantity: {}", productId, quantity);

    try {
      int updatedRows = inventoryRepository.reserveStockAtomically(productId, quantity);

      if (updatedRows > 0) {
        // Registrar movimiento de reserva
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
          Inventory inventory = inventoryOpt.get();
          InventoryMovement movement = InventoryMovement.builder()
              .productId(productId)
              .movementType(InventoryMovement.MovementType.RESERVATION)
              .quantityChange(quantity)
              .previousQuantity(inventory.getReservedQuantity() - quantity)
              .newQuantity(inventory.getReservedQuantity())
              .reason("Stock reserved for purchase")
              .build();
          inventoryMovementRepository.save(movement);

          // Publicar evento
          kafkaProducerService.publishStockReservedEvent(productId, quantity, null);
        }

        log.info("Stock reserved successfully for product ID: {}", productId);
        return true;
      } else {
        log.warn("Failed to reserve stock for product ID: {} - insufficient available stock", productId);
        return false;
      }
    } catch (Exception e) {
      log.error("Error reserving stock for product ID: {}", productId, e);
      return false;
    }
  }

  @Override
  public boolean releaseReservedStock(Long productId, Integer quantity) {
    log.info("Releasing reserved stock for product ID: {} and quantity: {}", productId, quantity);

    try {
      int updatedRows = inventoryRepository.releaseReservedStockAtomically(productId, quantity);

      if (updatedRows > 0) {
        // Registrar movimiento de liberación
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
          Inventory inventory = inventoryOpt.get();
          InventoryMovement movement = InventoryMovement.builder()
              .productId(productId)
              .movementType(InventoryMovement.MovementType.RELEASE)
              .quantityChange(-quantity)
              .previousQuantity(inventory.getReservedQuantity() + quantity)
              .newQuantity(inventory.getReservedQuantity())
              .reason("Reserved stock released")
              .build();
          inventoryMovementRepository.save(movement);

          // Publicar evento
          kafkaProducerService.publishStockReleasedEvent(productId, quantity, null);
        }

        log.info("Reserved stock released successfully for product ID: {}", productId);
        return true;
      } else {
        log.warn("Failed to release reserved stock for product ID: {}", productId);
        return false;
      }
    } catch (Exception e) {
      log.error("Error releasing reserved stock for product ID: {}", productId, e);
      return false;
    }
  }

  @Override
  public boolean confirmPurchase(Long productId, Integer quantity) {
    log.info("Confirming purchase for product ID: {} and quantity: {}", productId, quantity);

    try {
      int updatedRows = inventoryRepository.confirmPurchaseAtomically(productId, quantity);

      if (updatedRows > 0) {
        // Registrar movimiento de compra
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
          Inventory inventory = inventoryOpt.get();
          InventoryMovement movement = InventoryMovement.createPurchaseMovement(
              productId, quantity, inventory.getQuantity() + quantity, null);
          inventoryMovementRepository.save(movement);

          // Publicar eventos si es necesario
          if (inventory.isLowStock()) {
            kafkaProducerService.publishLowStockEvent(inventory);
          }

          if (inventory.isOutOfStock()) {
            kafkaProducerService.publishOutOfStockEvent(inventory);
          }
        }

        log.info("Purchase confirmed successfully for product ID: {}", productId);
        return true;
      } else {
        log.warn("Failed to confirm purchase for product ID: {} - insufficient stock or reservation", productId);
        return false;
      }
    } catch (Exception e) {
      log.error("Error confirming purchase for product ID: {}", productId, e);
      return false;
    }
  }

  @Override
  public InventoryDto createInventory(Long productId, Integer initialQuantity,
                                      Integer minStockLevel, Integer maxStockLevel) {
    log.info("Creating inventory for product ID: {} with quantity: {}", productId, initialQuantity);

    // Verificar que el producto existe
    Optional<ProductDto> productOpt = productService.getProductById(productId);
    if (productOpt.isEmpty()) {
      throw new ProductNotFoundException("Product not found with ID: " + productId);
    }

    // Verificar que no existe inventario previo
    if (inventoryRepository.existsByProductId(productId)) {
      throw new IllegalArgumentException("Inventory already exists for product ID: " + productId);
    }

    Inventory inventory = Inventory.builder()
        .productId(productId)
        .quantity(initialQuantity)
        .reservedQuantity(0)
        .minStockLevel(minStockLevel != null ? minStockLevel : 10)
        .maxStockLevel(maxStockLevel != null ? maxStockLevel : 1000)
        .build();

    Inventory savedInventory = inventoryRepository.save(inventory);

    // Registrar movimiento inicial
    InventoryMovement movement = InventoryMovement.builder()
        .productId(productId)
        .movementType(InventoryMovement.MovementType.INITIAL_STOCK)
        .quantityChange(initialQuantity)
        .previousQuantity(0)
        .newQuantity(initialQuantity)
        .reason("Initial inventory creation")
        .build();
    inventoryMovementRepository.save(movement);

    // Publicar evento
    kafkaProducerService.publishInventoryUpdatedEvent(savedInventory);

    log.info("Inventory created successfully for product ID: {}", productId);
    return mapToDto(savedInventory);
  }

  @Override
  public void deleteInventory(Long productId) {
    log.info("Deleting inventory for product ID: {}", productId);

    Inventory inventory = inventoryRepository.findByProductId(productId)
        .orElseThrow(() -> new ProductNotFoundException("Inventory not found for product ID: " + productId));

    // Verificar que no hay stock reservado
    if (inventory.getReservedQuantity() > 0) {
      throw new IllegalStateException("Cannot delete inventory with reserved stock. Reserved quantity: " +
          inventory.getReservedQuantity());
    }

    inventoryRepository.delete(inventory);

    log.info("Inventory deleted successfully for product ID: {}", productId);
  }

  @Override
  @Transactional(readOnly = true)
  public Object getInventoryStatistics() {
    log.debug("Getting inventory statistics");
    return inventoryRepository.getInventoryStatistics();
  }

  @Override
  public InventoryDto adjustInventory(Long productId, Integer newQuantity, String reason, String referenceId) {
    log.info("Adjusting inventory for product ID: {} to quantity: {}", productId, newQuantity);

    Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
        .orElseThrow(() -> new ProductNotFoundException("Inventory not found for product ID: " + productId));

    Integer previousQuantity = inventory.getQuantity();

    // Validar que la nueva cantidad no sea menor que la cantidad reservada
    if (newQuantity < inventory.getReservedQuantity()) {
      throw new InsufficientStockException(
          "Cannot adjust quantity below reserved amount. Reserved: " + inventory.getReservedQuantity() +
              ", New quantity: " + newQuantity);
    }

    inventory.setQuantity(newQuantity);
    Inventory savedInventory = inventoryRepository.save(inventory);

    // Registrar movimiento
    InventoryMovement movement = InventoryMovement.createAdjustmentMovement(
        productId, previousQuantity, newQuantity, reason, referenceId);
    inventoryMovementRepository.save(movement);

    // Publicar eventos
    kafkaProducerService.publishInventoryAdjustedEvent(savedInventory, previousQuantity, reason);

    if (savedInventory.isLowStock()) {
      kafkaProducerService.publishLowStockEvent(savedInventory);
    }

    if (savedInventory.isOutOfStock()) {
      kafkaProducerService.publishOutOfStockEvent(savedInventory);
    }

    log.info("Inventory adjusted successfully for product ID: {}", productId);
    return mapToDto(savedInventory);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Object> getTopSellingProducts(Integer limit) {
    log.debug("Getting top {} selling products", limit);

    Pageable pageable = PageRequest.of(0, limit != null ? limit : 10);
    return inventoryRepository.findTopSellingProducts(pageable);
  }

  @Override
  public InventoryDto synchronizeWithProductService(Long productId) {
    log.info("Synchronizing inventory for product ID: {}", productId);

    // Verificar que el producto existe en el servicio de productos
    Optional<ProductDto> productOpt = productService.getProductById(productId);
    if (productOpt.isEmpty()) {
      throw new ProductNotFoundException("Product not found in product service with ID: " + productId);
    }

    // Obtener o crear inventario
    Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

    if (inventoryOpt.isPresent()) {
      log.info("Inventory already exists for product ID: {}, returning current state", productId);
      return mapToDto(inventoryOpt.get());
    } else {
      log.info("Creating new inventory for product ID: {}", productId);
      return createInventory(productId, 0, 10, 1000);
    }
  }

  /**
   * Mapea una entidad Inventory a InventoryDto
   */
  private InventoryDto mapToDto(Inventory inventory) {
    // Obtener información del producto de forma segura
    ProductDto productInfo = productService.getProductInfoSafely(inventory.getProductId());

    return InventoryDto.builder()
        .id(inventory.getId())
        .productId(inventory.getProductId())
        .quantity(inventory.getQuantity())
        .reservedQuantity(inventory.getReservedQuantity())
        .availableQuantity(inventory.getAvailableQuantity())
        .minStockLevel(inventory.getMinStockLevel())
        .maxStockLevel(inventory.getMaxStockLevel())
        .isLowStock(inventory.isLowStock())
        .isOutOfStock(inventory.isOutOfStock())
        .productInfo(productInfo)
        .createdAt(inventory.getCreatedAt())
        .updatedAt(inventory.getUpdatedAt())
        .build();
  }
}