package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.ProductDto;
import com.example.inventory.controller.dto.PurchaseRequestDto;
import com.example.inventory.controller.dto.PurchaseResponseDto;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.PurchaseHistory;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.PurchaseHistoryRepository;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.KafkaProducerService;
import com.example.inventory.service.ProductService;
import com.example.inventory.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de compras
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

  private final PurchaseHistoryRepository purchaseHistoryRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryService inventoryService;
  private final ProductService productService;
  private final KafkaProducerService kafkaProducerService;

  @Override
  public PurchaseResponseDto processPurchase(PurchaseRequestDto request) {
    log.info("Processing purchase for product ID: {} with quantity: {}",
        request.getProductId(), request.getQuantity());

    // 1. Validar la solicitud de compra
    if (!validatePurchaseRequest(request)) {
      throw new IllegalArgumentException("Invalid purchase request");
    }

    // 2. Obtener información del producto
    Optional<ProductDto> productOpt = productService.getProductById(request.getProductId());
    if (productOpt.isEmpty()) {
      throw new ProductNotFoundException("Product not found with ID: " + request.getProductId());
    }

    ProductDto product = productOpt.get();
    if (!product.getActive()) {
      throw new IllegalArgumentException("Product is not active: " + request.getProductId());
    }

    // 3. Verificar disponibilidad de stock
    if (!inventoryService.checkStockAvailability(request.getProductId(), request.getQuantity())) {
      throw new InsufficientStockException(
          "Insufficient stock for product ID: " + request.getProductId() +
              ". Requested: " + request.getQuantity());
    }

    // 4. Reservar stock
    if (!inventoryService.reserveStock(request.getProductId(), request.getQuantity())) {
      throw new InsufficientStockException("Failed to reserve stock for purchase");
    }

    try {
      // 5. Calcular precios
      BigDecimal unitPrice = product.getPrice();
      BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

      // 6. Crear registro de compra
      PurchaseHistory purchaseHistory = PurchaseHistory.builder()
          .productId(request.getProductId())
          .quantityPurchased(request.getQuantity())
          .unitPrice(unitPrice)
          .totalPrice(totalPrice)
          .customerInfo(request.getCustomerInfo())
          .purchaseStatus(PurchaseHistory.PurchaseStatus.PENDING)
          .build();

      PurchaseHistory savedPurchase = purchaseHistoryRepository.save(purchaseHistory);

      // 7. Confirmar la compra (reducir stock físico)
      if (!inventoryService.confirmPurchase(request.getProductId(), request.getQuantity())) {
        // Si falla la confirmación, marcar como cancelada y liberar reserva
        savedPurchase.markAsCancelled();
        purchaseHistoryRepository.save(savedPurchase);
        inventoryService.releaseReservedStock(request.getProductId(), request.getQuantity());

        throw new RuntimeException("Failed to confirm purchase - insufficient stock");
      }

      // 8. Marcar compra como completada
      savedPurchase.markAsCompleted();
      savedPurchase = purchaseHistoryRepository.save(savedPurchase);

      // 9. Obtener inventario actualizado
      Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(request.getProductId());

      // 10. Publicar eventos
      if (inventoryOpt.isPresent()) {
        kafkaProducerService.publishPurchaseCompletedEvent(savedPurchase, inventoryOpt.get());
      }

      log.info("Purchase processed successfully with ID: {}", savedPurchase.getPurchaseId());

      return mapToResponseDto(savedPurchase, product,
          inventoryOpt.map(Inventory::getQuantity).orElse(0));

    } catch (Exception e) {
      // En caso de error, liberar la reserva de stock
      inventoryService.releaseReservedStock(request.getProductId(), request.getQuantity());
      log.error("Error processing purchase for product ID: {}", request.getProductId(), e);
      throw e;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseResponseDto getPurchaseById(UUID purchaseId) {
    log.debug("Getting purchase by ID: {}", purchaseId);

    PurchaseHistory purchase = purchaseHistoryRepository.findByPurchaseId(purchaseId)
        .orElseThrow(() -> new ProductNotFoundException("Purchase not found with ID: " + purchaseId));

    ProductDto product = productService.getProductInfoSafely(purchase.getProductId());

    return mapToResponseDto(purchase, product, null);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PurchaseResponseDto> getPurchaseHistory(Pageable pageable) {
    log.debug("Getting purchase history with pagination: page={}, size={}",
        pageable.getPageNumber(), pageable.getPageSize());

    Page<PurchaseHistory> purchases = purchaseHistoryRepository.findAll(pageable);
    return purchases.map(purchase -> {
      ProductDto product = productService.getProductInfoSafely(purchase.getProductId());
      return mapToResponseDto(purchase, product, null);
    });
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PurchaseResponseDto> getPurchaseHistoryByProduct(Long productId, Pageable pageable) {
    log.debug("Getting purchase history for product ID: {}", productId);

    Page<PurchaseHistory> purchases = purchaseHistoryRepository
        .findByProductIdOrderByPurchaseDateDesc(productId, pageable);

    ProductDto product = productService.getProductInfoSafely(productId);

    return purchases.map(purchase -> mapToResponseDto(purchase, product, null));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PurchaseResponseDto> getPurchaseHistoryWithFilters(Long productId,
                                                                 PurchaseHistory.PurchaseStatus status,
                                                                 LocalDateTime startDate,
                                                                 LocalDateTime endDate,
                                                                 Integer minQuantity,
                                                                 Integer maxQuantity,
                                                                 Pageable pageable) {
    log.debug("Getting purchase history with filters");

    Page<PurchaseHistory> purchases = purchaseHistoryRepository.findWithFilters(
        productId, status, startDate, endDate, minQuantity, maxQuantity, pageable);

    return purchases.map(purchase -> {
      ProductDto product = productService.getProductInfoSafely(purchase.getProductId());
      return mapToResponseDto(purchase, product, null);
    });
  }

  @Override
  public boolean cancelPurchase(UUID purchaseId) {
    log.info("Cancelling purchase with ID: {}", purchaseId);

    Optional<PurchaseHistory> purchaseOpt = purchaseHistoryRepository.findByPurchaseId(purchaseId);
    if (purchaseOpt.isEmpty()) {
      log.warn("Purchase not found with ID: {}", purchaseId);
      return false;
    }

    PurchaseHistory purchase = purchaseOpt.get();

    // Solo se pueden cancelar compras pendientes
    if (!purchase.isPending()) {
      log.warn("Cannot cancel purchase with status: {}", purchase.getPurchaseStatus());
      return false;
    }

    // Liberar stock reservado
    inventoryService.releaseReservedStock(purchase.getProductId(), purchase.getQuantityPurchased());

    // Marcar como cancelada
    purchase.markAsCancelled();
    purchaseHistoryRepository.save(purchase);

    // Publicar evento
    kafkaProducerService.publishPurchaseCancelledEvent(purchase);

    log.info("Purchase cancelled successfully: {}", purchaseId);
    return true;
  }

  @Override
  public boolean refundPurchase(UUID purchaseId) {
    log.info("Processing refund for purchase ID: {}", purchaseId);

    Optional<PurchaseHistory> purchaseOpt = purchaseHistoryRepository.findByPurchaseId(purchaseId);
    if (purchaseOpt.isEmpty()) {
      log.warn("Purchase not found with ID: {}", purchaseId);
      return false;
    }

    PurchaseHistory purchase = purchaseOpt.get();

    // Solo se pueden reembolsar compras completadas
    if (!purchase.isCompleted()) {
      log.warn("Cannot refund purchase with status: {}", purchase.getPurchaseStatus());
      return false;
    }

    // Devolver stock al inventario
    Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdForUpdate(purchase.getProductId());
    if (inventoryOpt.isPresent()) {
      Inventory inventory = inventoryOpt.get();
      inventory.addStock(purchase.getQuantityPurchased());
      inventoryRepository.save(inventory);
    }

    // Marcar como reembolsada
    purchase.markAsRefunded();
    purchaseHistoryRepository.save(purchase);

    log.info("Purchase refunded successfully: {}", purchaseId);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public Object getSalesStatistics(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting sales statistics from {} to {}", startDate, endDate);
    return purchaseHistoryRepository.getSalesStatistics(startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Object> getPurchaseStatisticsByProduct() {
    log.debug("Getting purchase statistics by product");
    return purchaseHistoryRepository.getPurchaseStatisticsByProduct();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Object> getTopSellingProducts(Integer limit) {
    log.debug("Getting top {} selling products", limit);

    Pageable pageable = PageRequest.of(0, limit != null ? limit : 10);
    return purchaseHistoryRepository.getTopSellingProducts(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Object> getDailySalesReport(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting daily sales report from {} to {}", startDate, endDate);
    return purchaseHistoryRepository.getDailySalesReport(startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PurchaseResponseDto> getStalePendingPurchases(Integer hoursThreshold) {
    log.debug("Getting stale pending purchases older than {} hours", hoursThreshold);

    LocalDateTime thresholdDate = LocalDateTime.now().minusHours(hoursThreshold);
    List<PurchaseHistory> stalePurchases = purchaseHistoryRepository
        .findStalesPendingPurchases(thresholdDate);

    return stalePurchases.stream()
        .map(purchase -> {
          ProductDto product = productService.getProductInfoSafely(purchase.getProductId());
          return mapToResponseDto(purchase, product, null);
        })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Object> getRevenueByProduct(LocalDateTime startDate, LocalDateTime endDate) {
    log.debug("Getting revenue by product from {} to {}", startDate, endDate);
    return purchaseHistoryRepository.getRevenueByProduct(startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean validatePurchaseRequest(PurchaseRequestDto request) {
    if (request == null) {
      log.warn("Purchase request is null");
      return false;
    }

    if (request.getProductId() == null || request.getProductId() <= 0) {
      log.warn("Invalid product ID: {}", request.getProductId());
      return false;
    }

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      log.warn("Invalid quantity: {}", request.getQuantity());
      return false;
    }

    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal calculateTotalPrice(Long productId, Integer quantity) {
    log.debug("Calculating total price for product ID: {} and quantity: {}", productId, quantity);

    Optional<ProductDto> productOpt = productService.getProductById(productId);
    if (productOpt.isEmpty()) {
      throw new ProductNotFoundException("Product not found with ID: " + productId);
    }

    BigDecimal unitPrice = productOpt.get().getPrice();
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PurchaseResponseDto> getRecentPurchases(Integer hours) {
    log.debug("Getting recent purchases from last {} hours", hours);

    LocalDateTime since = LocalDateTime.now().minusHours(hours != null ? hours : 24);
    List<PurchaseHistory> recentPurchases = purchaseHistoryRepository.findRecentPurchases(since);

    return recentPurchases.stream()
        .map(purchase -> {
          ProductDto product = productService.getProductInfoSafely(purchase.getProductId());
          return mapToResponseDto(purchase, product, null);
        })
        .collect(Collectors.toList());
  }

  @Override
  public boolean retryFailedPurchase(UUID purchaseId) {
    log.info("Retrying failed purchase with ID: {}", purchaseId);

    Optional<PurchaseHistory> purchaseOpt = purchaseHistoryRepository.findByPurchaseId(purchaseId);
    if (purchaseOpt.isEmpty()) {
      log.warn("Purchase not found with ID: {}", purchaseId);
      return false;
    }

    PurchaseHistory purchase = purchaseOpt.get();

    // Solo reintentar compras pendientes o canceladas
    if (purchase.isCompleted()) {
      log.warn("Cannot retry completed purchase: {}", purchaseId);
      return false;
    }

    try {
      // Recrear la solicitud de compra
      PurchaseRequestDto request = PurchaseRequestDto.builder()
          .productId(purchase.getProductId())
          .quantity(purchase.getQuantityPurchased())
          .customerInfo(purchase.getCustomerInfo())
          .build();

      // Procesar la compra nuevamente
      PurchaseResponseDto response = processPurchase(request);

      // Marcar la compra original como cancelada si el reintento fue exitoso
      purchase.markAsCancelled();
      purchaseHistoryRepository.save(purchase);

      log.info("Purchase retry successful. Original: {}, New: {}",
          purchaseId, response.getPurchaseId());
      return true;

    } catch (Exception e) {
      log.error("Failed to retry purchase: {}", purchaseId, e);
      return false;
    }
  }

  /**
   * Mapea una entidad PurchaseHistory a PurchaseResponseDto
   */
  private PurchaseResponseDto mapToResponseDto(PurchaseHistory purchase, ProductDto product,
                                               Integer remainingStock) {
    return PurchaseResponseDto.builder()
        .purchaseId(purchase.getPurchaseId())
        .productId(purchase.getProductId())
        .productInfo(product)
        .quantityPurchased(purchase.getQuantityPurchased())
        .unitPrice(purchase.getUnitPrice())
        .totalPrice(purchase.getTotalPrice())
        .purchaseStatus(purchase.getPurchaseStatus())
        .customerInfo(purchase.getCustomerInfo())
        .purchaseDate(purchase.getPurchaseDate())
        .remainingStock(remainingStock)
        .message(generatePurchaseMessage(purchase))
        .build();
  }

  /**
   * Genera un mensaje descriptivo para la compra
   */
  private String generatePurchaseMessage(PurchaseHistory purchase) {
    switch (purchase.getPurchaseStatus()) {
      case COMPLETED:
        return "Purchase completed successfully";
      case PENDING:
        return "Purchase is pending confirmation";
      case CANCELLED:
        return "Purchase was cancelled";
      case REFUNDED:
        return "Purchase was refunded";
      default:
        return "Unknown purchase status";
    }
  }
}