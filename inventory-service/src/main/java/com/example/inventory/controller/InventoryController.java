package com.example.inventory.controller;

import com.example.inventory.controller.dto.InventoryDto;
import com.example.inventory.controller.dto.PurchaseRequestDto;
import com.example.inventory.controller.dto.PurchaseResponseDto;
import com.example.inventory.controller.dto.UpdateInventoryRequestDto;
import com.example.inventory.model.PurchaseHistory;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para gestión de inventario y compras
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Inventory Management", description = "APIs para gestión de inventario y procesamiento de compras")
public class InventoryController {

  private final InventoryService inventoryService;
  private final PurchaseService purchaseService;

  // ==================== ENDPOINTS DE INVENTARIO ====================

  @GetMapping("/{productId}")
  @Operation(summary = "Obtener inventario por ID de producto",
      description = "Consulta la información de inventario de un producto específico")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Inventario encontrado",
          content = @Content(schema = @Schema(implementation = InventoryDto.class))),
      @ApiResponse(responseCode = "404", description = "Inventario no encontrado"),
      @ApiResponse(responseCode = "500", description = "Error interno del servidor")
  })
  public ResponseEntity<InventoryDto> getInventoryByProductId(
      @Parameter(description = "ID del producto", required = true, example = "1")
      @PathVariable @Min(1) Long productId) {

    log.info("Getting inventory for product ID: {}", productId);
    InventoryDto inventory = inventoryService.getInventoryByProductId(productId);
    return ResponseEntity.ok(inventory);
  }

  @PutMapping("/{productId}")
  @Operation(summary = "Actualizar inventario",
      description = "Actualiza la cantidad y configuración de inventario de un producto")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Inventario actualizado exitosamente"),
      @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
      @ApiResponse(responseCode = "404", description = "Inventario no encontrado"),
      @ApiResponse(responseCode = "409", description = "Conflicto - cantidad menor a stock reservado")
  })
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_UPDATE')")
  public ResponseEntity<InventoryDto> updateInventory(
      @Parameter(description = "ID del producto", required = true)
      @PathVariable @Min(1) Long productId,
      @Parameter(description = "Datos de actualización del inventario", required = true)
      @Valid @RequestBody UpdateInventoryRequestDto request) {

    log.info("Updating inventory for product ID: {} with request: {}", productId, request);
    InventoryDto updatedInventory = inventoryService.updateInventory(productId, request);
    return ResponseEntity.ok(updatedInventory);
  }

  @GetMapping
  @Operation(summary = "Listar inventarios",
      description = "Obtiene una lista paginada de todos los inventarios con filtros opcionales")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de inventarios obtenida exitosamente")
  })
  public ResponseEntity<Page<InventoryDto>> getAllInventories(
      @Parameter(description = "Filtrar por productos con stock")
      @RequestParam(required = false) Boolean hasStock,
      @Parameter(description = "Filtrar por productos con stock bajo")
      @RequestParam(required = false) Boolean isLowStock,
      @Parameter(description = "Cantidad mínima")
      @RequestParam(required = false) @Min(0) Integer minQuantity,
      @Parameter(description = "Cantidad máxima")
      @RequestParam(required = false) @Min(0) Integer maxQuantity,
      @Parameter(description = "Número de página (0-indexado)")
      @RequestParam(defaultValue = "0") @Min(0) Integer page,
      @Parameter(description = "Tamaño de página")
      @RequestParam(defaultValue = "20") @Min(1) Integer size) {

    log.debug("Getting inventories with filters - hasStock: {}, isLowStock: {}, page: {}, size: {}",
        hasStock, isLowStock, page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<InventoryDto> inventories;

    if (hasStock != null || isLowStock != null || minQuantity != null || maxQuantity != null) {
      inventories = inventoryService.getInventoriesWithFilters(
          hasStock, isLowStock, minQuantity, maxQuantity, pageable);
    } else {
      inventories = inventoryService.getAllInventories(pageable);
    }

    return ResponseEntity.ok(inventories);
  }

  @GetMapping("/low-stock")
  @Operation(summary = "Obtener productos con stock bajo",
      description = "Lista todos los productos que están por debajo del nivel mínimo de stock")
  public ResponseEntity<List<InventoryDto>> getLowStockProducts() {
    log.info("Getting low stock products");
    List<InventoryDto> lowStockProducts = inventoryService.getLowStockProducts();
    return ResponseEntity.ok(lowStockProducts);
  }

  @GetMapping("/out-of-stock")
  @Operation(summary = "Obtener productos fuera de stock",
      description = "Lista todos los productos que no tienen stock disponible")
  public ResponseEntity<List<InventoryDto>> getOutOfStockProducts() {
    log.info("Getting out of stock products");
    List<InventoryDto> outOfStockProducts = inventoryService.getOutOfStockProducts();
    return ResponseEntity.ok(outOfStockProducts);
  }

  @GetMapping("/{productId}/availability")
  @Operation(summary = "Verificar disponibilidad de stock",
      description = "Verifica si hay suficiente stock disponible para una cantidad específica")
  public ResponseEntity<Map<String, Object>> checkStockAvailability(
      @Parameter(description = "ID del producto", required = true)
      @PathVariable @Min(1) Long productId,
      @Parameter(description = "Cantidad requerida", required = true)
      @RequestParam @Min(1) Integer quantity) {

    log.debug("Checking stock availability for product ID: {} and quantity: {}", productId, quantity);
    boolean available = inventoryService.checkStockAvailability(productId, quantity);

    Map<String, Object> response = Map.of(
        "productId", productId,
        "requestedQuantity", quantity,
        "available", available,
        "timestamp", LocalDateTime.now()
    );

    return ResponseEntity.ok(response);
  }

  @GetMapping("/statistics")
  @Operation(summary = "Obtener estadísticas de inventario",
      description = "Proporciona estadísticas generales sobre el estado del inventario")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
  public ResponseEntity<Object> getInventoryStatistics() {
    log.info("Getting inventory statistics");
    Object statistics = inventoryService.getInventoryStatistics();
    return ResponseEntity.ok(statistics);
  }

  // ==================== ENDPOINTS DE COMPRAS ====================

  @PostMapping("/purchases")
  @Operation(summary = "Procesar compra",
      description = "Procesa una compra completa verificando stock, reservando y confirmando la transacción")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Compra procesada exitosamente",
          content = @Content(schema = @Schema(implementation = PurchaseResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Datos de compra inválidos"),
      @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
      @ApiResponse(responseCode = "409", description = "Stock insuficiente"),
      @ApiResponse(responseCode = "500", description = "Error procesando la compra")
  })
  public ResponseEntity<PurchaseResponseDto> processPurchase(
      @Parameter(description = "Datos de la compra", required = true)
      @Valid @RequestBody PurchaseRequestDto request) {

    log.info("Processing purchase request: {}", request);
    PurchaseResponseDto response = purchaseService.processPurchase(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/purchases/{purchaseId}")
  @Operation(summary = "Obtener detalle de compra",
      description = "Consulta los detalles de una compra específica por su ID")
  public ResponseEntity<PurchaseResponseDto> getPurchaseById(
      @Parameter(description = "ID único de la compra", required = true)
      @PathVariable UUID purchaseId) {

    log.debug("Getting purchase by ID: {}", purchaseId);
    PurchaseResponseDto purchase = purchaseService.getPurchaseById(purchaseId);
    return ResponseEntity.ok(purchase);
  }

  @GetMapping("/purchases")
  @Operation(summary = "Obtener historial de compras",
      description = "Lista el historial de compras con filtros opcionales y paginación")
  public ResponseEntity<Page<PurchaseResponseDto>> getPurchaseHistory(
      @Parameter(description = "ID del producto para filtrar")
      @RequestParam(required = false) Long productId,
      @Parameter(description = "Estado de la compra")
      @RequestParam(required = false) PurchaseHistory.PurchaseStatus status,
      @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd'T'HH:mm:ss)")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd'T'HH:mm:ss)")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
      @Parameter(description = "Cantidad mínima comprada")
      @RequestParam(required = false) @Min(1) Integer minQuantity,
      @Parameter(description = "Cantidad máxima comprada")
      @RequestParam(required = false) @Min(1) Integer maxQuantity,
      @Parameter(description = "Número de página (0-indexado)")
      @RequestParam(defaultValue = "0") @Min(0) Integer page,
      @Parameter(description = "Tamaño de página")
      @RequestParam(defaultValue = "20") @Min(1) Integer size) {

    log.debug("Getting purchase history with filters");
    Pageable pageable = PageRequest.of(page, size);

    Page<PurchaseResponseDto> purchases;
    if (productId != null || status != null || startDate != null || endDate != null ||
        minQuantity != null || maxQuantity != null) {
      purchases = purchaseService.getPurchaseHistoryWithFilters(
          productId, status, startDate, endDate, minQuantity, maxQuantity, pageable);
    } else {
      purchases = purchaseService.getPurchaseHistory(pageable);
    }

    return ResponseEntity.ok(purchases);
  }

  @PutMapping("/purchases/{purchaseId}/cancel")
  @Operation(summary = "Cancelar compra",
      description = "Cancela una compra pendiente y libera el stock reservado")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PURCHASE_CANCEL')")
  public ResponseEntity<Map<String, Object>> cancelPurchase(
      @Parameter(description = "ID único de la compra", required = true)
      @PathVariable UUID purchaseId) {

    log.info("Cancelling purchase: {}", purchaseId);
    boolean cancelled = purchaseService.cancelPurchase(purchaseId);

    Map<String, Object> response = Map.of(
        "purchaseId", purchaseId,
        "cancelled", cancelled,
        "message", cancelled ? "Purchase cancelled successfully" : "Failed to cancel purchase",
        "timestamp", LocalDateTime.now()
    );

    HttpStatus status = cancelled ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status).body(response);
  }

  @PutMapping("/purchases/{purchaseId}/refund")
  @Operation(summary = "Reembolsar compra",
      description = "Procesa el reembolso de una compra completada y devuelve el stock")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PURCHASE_REFUND')")
  public ResponseEntity<Map<String, Object>> refundPurchase(
      @Parameter(description = "ID único de la compra", required = true)
      @PathVariable UUID purchaseId) {

    log.info("Processing refund for purchase: {}", purchaseId);
    boolean refunded = purchaseService.refundPurchase(purchaseId);

    Map<String, Object> response = Map.of(
        "purchaseId", purchaseId,
        "refunded", refunded,
        "message", refunded ? "Purchase refunded successfully" : "Failed to refund purchase",
        "timestamp", LocalDateTime.now()
    );

    HttpStatus status = refunded ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status).body(response);
  }

  // ==================== ENDPOINTS DE REPORTES Y ESTADÍSTICAS ====================

  @GetMapping("/purchases/statistics")
  @Operation(summary = "Obtener estadísticas de ventas",
      description = "Proporciona estadísticas de ventas para un período específico")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('SALES_READ')")
  public ResponseEntity<Object> getSalesStatistics(
      @Parameter(description = "Fecha de inicio")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @Parameter(description = "Fecha de fin")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    log.info("Getting sales statistics from {} to {}", startDate, endDate);
    Object statistics = purchaseService.getSalesStatistics(startDate, endDate);
    return ResponseEntity.ok(statistics);
  }

  @GetMapping("/purchases/top-products")
  @Operation(summary = "Obtener productos más vendidos",
      description = "Lista los productos más vendidos por cantidad")
  public ResponseEntity<List<Object>> getTopSellingProducts(
      @Parameter(description = "Número máximo de productos a retornar")
      @RequestParam(defaultValue = "10") @Min(1) Integer limit) {

    log.info("Getting top {} selling products", limit);
    List<Object> topProducts = purchaseService.getTopSellingProducts(limit);
    return ResponseEntity.ok(topProducts);
  }

  @GetMapping("/purchases/daily-report")
  @Operation(summary = "Obtener reporte de ventas diarias",
      description = "Genera un reporte de ventas agrupado por días")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('REPORTS_READ')")
  public ResponseEntity<List<Object>> getDailySalesReport(
      @Parameter(description = "Fecha de inicio")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @Parameter(description = "Fecha de fin")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    log.info("Getting daily sales report from {} to {}", startDate, endDate);
    List<Object> dailyReport = purchaseService.getDailySalesReport(startDate, endDate);
    return ResponseEntity.ok(dailyReport);
  }

  @GetMapping("/purchases/recent")
  @Operation(summary = "Obtener compras recientes",
      description = "Lista las compras más recientes")
  public ResponseEntity<List<PurchaseResponseDto>> getRecentPurchases(
      @Parameter(description = "Número de horas hacia atrás")
      @RequestParam(defaultValue = "24") @Min(1) Integer hours) {

    log.debug("Getting recent purchases from last {} hours", hours);
    List<PurchaseResponseDto> recentPurchases = purchaseService.getRecentPurchases(hours);
    return ResponseEntity.ok(recentPurchases);
  }

  // ==================== ENDPOINTS DE UTILIDAD ====================

  @PostMapping("/sync/{productId}")
  @Operation(summary = "Sincronizar inventario con servicio de productos",
      description = "Sincroniza la información de inventario con el servicio de productos")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_SYNC')")
  public ResponseEntity<InventoryDto> synchronizeInventory(
      @Parameter(description = "ID del producto", required = true)
      @PathVariable @Min(1) Long productId) {

    log.info("Synchronizing inventory for product ID: {}", productId);
    InventoryDto inventory = inventoryService.synchronizeWithProductService(productId);
    return ResponseEntity.ok(inventory);
  }

  @PostMapping("/{productId}/adjust")
  @Operation(summary = "Ajustar inventario",
      description = "Realiza un ajuste manual del inventario con auditoría")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_ADJUST')")
  public ResponseEntity<InventoryDto> adjustInventory(
      @Parameter(description = "ID del producto", required = true)
      @PathVariable @Min(1) Long productId,
      @Parameter(description = "Nueva cantidad", required = true)
      @RequestParam @Min(0) Integer newQuantity,
      @Parameter(description = "Razón del ajuste")
      @RequestParam(required = false) String reason,
      @Parameter(description = "ID de referencia para auditoría")
      @RequestParam(required = false) String referenceId) {

    log.info("Adjusting inventory for product ID: {} to quantity: {}", productId, newQuantity);
    InventoryDto adjustedInventory = inventoryService.adjustInventory(
        productId, newQuantity, reason, referenceId);
    return ResponseEntity.ok(adjustedInventory);
  }
}