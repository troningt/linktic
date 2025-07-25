package com.example.inventory.repository;

import com.example.inventory.model.PurchaseHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para la entidad PurchaseHistory
 */
@Repository
public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {

  /**
   * Busca una compra por su UUID
   */
  Optional<PurchaseHistory> findByPurchaseId(UUID purchaseId);

  /**
   * Busca compras por ID de producto
   */
  Page<PurchaseHistory> findByProductIdOrderByPurchaseDateDesc(Long productId, Pageable pageable);

  /**
   * Busca compras por estado
   */
  Page<PurchaseHistory> findByPurchaseStatusOrderByPurchaseDateDesc(
      PurchaseHistory.PurchaseStatus status, Pageable pageable);

  /**
   * Busca compras en un rango de fechas
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.purchaseDate BETWEEN :startDate AND :endDate " +
      "ORDER BY p.purchaseDate DESC")
  Page<PurchaseHistory> findByPurchaseDateBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  Pageable pageable);

  /**
   * Busca compras por producto y rango de fechas
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.productId = :productId " +
      "AND p.purchaseDate BETWEEN :startDate AND :endDate " +
      "ORDER BY p.purchaseDate DESC")
  List<PurchaseHistory> findByProductIdAndDateRange(@Param("productId") Long productId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

  /**
   * Obtiene las compras más recientes
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.purchaseDate >= :since " +
      "ORDER BY p.purchaseDate DESC")
  List<PurchaseHistory> findRecentPurchases(@Param("since") LocalDateTime since);

  /**
   * Cuenta compras por estado
   */
  long countByPurchaseStatus(PurchaseHistory.PurchaseStatus status);

  /**
   * Cuenta compras de un producto específico
   */
  long countByProductId(Long productId);

  /**
   * Obtiene estadísticas de compras por producto
   */
  @Query("SELECT p.productId, COUNT(p) as totalPurchases, SUM(p.quantityPurchased) as totalQuantity, " +
      "SUM(p.totalPrice) as totalRevenue, AVG(p.totalPrice) as avgOrderValue " +
      "FROM PurchaseHistory p WHERE p.purchaseStatus = 'COMPLETED' " +
      "GROUP BY p.productId " +
      "ORDER BY totalRevenue DESC")
  List<Object> getPurchaseStatisticsByProduct();

  /**
   * Obtiene las ventas totales en un período
   */
  @Query("SELECT COUNT(p) as totalOrders, SUM(p.quantityPurchased) as totalQuantity, " +
      "SUM(p.totalPrice) as totalRevenue, AVG(p.totalPrice) as avgOrderValue " +
      "FROM PurchaseHistory p WHERE p.purchaseStatus = 'COMPLETED' " +
      "AND p.purchaseDate BETWEEN :startDate AND :endDate")
  SalesStatistics getSalesStatistics(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

  /**
   * Obtiene los productos más vendidos
   */
  @Query("SELECT p.productId, SUM(p.quantityPurchased) as totalSold " +
      "FROM PurchaseHistory p WHERE p.purchaseStatus = 'COMPLETED' " +
      "GROUP BY p.productId " +
      "ORDER BY totalSold DESC")
  List<Object> getTopSellingProducts(Pageable pageable);

  /**
   * Busca compras por cantidad mínima
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.quantityPurchased >= :minQuantity " +
      "ORDER BY p.purchaseDate DESC")
  Page<PurchaseHistory> findByMinQuantity(@Param("minQuantity") Integer minQuantity, Pageable pageable);

  /**
   * Busca compras por rango de precio total
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.totalPrice BETWEEN :minPrice AND :maxPrice " +
      "ORDER BY p.purchaseDate DESC")
  Page<PurchaseHistory> findByTotalPriceBetween(@Param("minPrice") BigDecimal minPrice,
                                                @Param("maxPrice") BigDecimal maxPrice,
                                                Pageable pageable);

  /**
   * Obtiene el historial de compras con filtros múltiples
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE " +
      "(:productId IS NULL OR p.productId = :productId) AND " +
      "(:status IS NULL OR p.purchaseStatus = :status) AND " +
      "(:startDate IS NULL OR p.purchaseDate >= :startDate) AND " +
      "(:endDate IS NULL OR p.purchaseDate <= :endDate) AND " +
      "(:minQuantity IS NULL OR p.quantityPurchased >= :minQuantity) AND " +
      "(:maxQuantity IS NULL OR p.quantityPurchased <= :maxQuantity) " +
      "ORDER BY p.purchaseDate DESC")
  Page<PurchaseHistory> findWithFilters(@Param("productId") Long productId,
                                        @Param("status") PurchaseHistory.PurchaseStatus status,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("minQuantity") Integer minQuantity,
                                        @Param("maxQuantity") Integer maxQuantity,
                                        Pageable pageable);

  /**
   * Obtiene ventas diarias para reporting
   */
  @Query("SELECT DATE(p.purchaseDate) as saleDate, COUNT(p) as totalOrders, " +
      "SUM(p.quantityPurchased) as totalQuantity, SUM(p.totalPrice) as totalRevenue " +
      "FROM PurchaseHistory p WHERE p.purchaseStatus = 'COMPLETED' " +
      "AND p.purchaseDate BETWEEN :startDate AND :endDate " +
      "GROUP BY DATE(p.purchaseDate) " +
      "ORDER BY p.purchaseDate DESC")
  List<Object> getDailySalesReport(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

  /**
   * Busca compras que requieren seguimiento (pendientes por mucho tiempo)
   */
  @Query("SELECT p FROM PurchaseHistory p WHERE p.purchaseStatus = 'PENDING' " +
      "AND p.purchaseDate < :thresholdDate " +
      "ORDER BY p.purchaseDate ASC")
  List<PurchaseHistory> findStalesPendingPurchases(@Param("thresholdDate") LocalDateTime thresholdDate);

  /**
   * Obtiene el total de ingresos por producto en un período
   */
  @Query("SELECT p.productId, SUM(p.totalPrice) as totalRevenue " +
      "FROM PurchaseHistory p WHERE p.purchaseStatus = 'COMPLETED' " +
      "AND p.purchaseDate BETWEEN :startDate AND :endDate " +
      "GROUP BY p.productId " +
      "ORDER BY totalRevenue DESC")
  List<Object> getRevenueByProduct(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

  /**
   * Verifica si existe al menos una compra completada para un producto
   */
  boolean existsByProductIdAndPurchaseStatus(Long productId, PurchaseHistory.PurchaseStatus status);

  /**
   * Interface para proyección de estadísticas de ventas
   */
  interface SalesStatistics {
    Long getTotalOrders();
    Long getTotalQuantity();
    BigDecimal getTotalRevenue();
    BigDecimal getAvgOrderValue();
  }
}