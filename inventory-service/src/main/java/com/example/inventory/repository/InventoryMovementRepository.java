package com.example.inventory.repository;

import com.example.inventory.model.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para la entidad InventoryMovement
 */
@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

  /**
   * Busca movimientos por ID de producto
   */
  Page<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

  /**
   * Busca movimientos por tipo
   */
  Page<InventoryMovement> findByMovementTypeOrderByCreatedAtDesc(
      InventoryMovement.MovementType movementType, Pageable pageable);

  /**
   * Busca movimientos en un rango de fechas
   */
  @Query("SELECT im FROM InventoryMovement im WHERE im.createdAt BETWEEN :startDate AND :endDate " +
      "ORDER BY im.createdAt DESC")
  Page<InventoryMovement> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

  /**
   * Busca movimientos por producto y tipo
   */
  List<InventoryMovement> findByProductIdAndMovementTypeOrderByCreatedAtDesc(
      Long productId, InventoryMovement.MovementType movementType);

  /**
   * Busca movimientos por ID de referencia
   */
  List<InventoryMovement> findByReferenceIdOrderByCreatedAtDesc(String referenceId);

  /**
   * Obtiene los movimientos más recientes
   */
  @Query("SELECT im FROM InventoryMovement im WHERE im.createdAt >= :since " +
      "ORDER BY im.createdAt DESC")
  List<InventoryMovement> findRecentMovements(@Param("since") LocalDateTime since);

  /**
   * Cuenta movimientos por tipo
   */
  long countByMovementType(InventoryMovement.MovementType movementType);

  /**
   * Cuenta movimientos por producto
   */
  long countByProductId(Long productId);

  /**
   * Obtiene el último movimiento de un producto
   */
  @Query("SELECT im FROM InventoryMovement im WHERE im.productId = :productId " +
      "ORDER BY im.createdAt DESC LIMIT 1")
  InventoryMovement findLastMovementByProductId(@Param("productId") Long productId);

  /**
   * Busca movimientos con filtros múltiples
   */
  @Query("SELECT im FROM InventoryMovement im WHERE " +
      "(:productId IS NULL OR im.productId = :productId) AND " +
      "(:movementType IS NULL OR im.movementType = :movementType) AND " +
      "(:startDate IS NULL OR im.createdAt >= :startDate) AND " +
      "(:endDate IS NULL OR im.createdAt <= :endDate) AND " +
      "(:referenceId IS NULL OR im.referenceId = :referenceId) " +
      "ORDER BY im.createdAt DESC")
  Page<InventoryMovement> findWithFilters(@Param("productId") Long productId,
                                          @Param("movementType") InventoryMovement.MovementType movementType,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("referenceId") String referenceId,
                                          Pageable pageable);

  /**
   * Obtiene estadísticas de movimientos por tipo
   */
  @Query("SELECT im.movementType, COUNT(im), SUM(ABS(im.quantityChange)) " +
      "FROM InventoryMovement im " +
      "WHERE im.createdAt BETWEEN :startDate AND :endDate " +
      "GROUP BY im.movementType " +
      "ORDER BY COUNT(im) DESC")
  List<Object[]> getMovementStatistics(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

  /**
   * Obtiene los productos con más movimientos
   */
  @Query("SELECT im.productId, COUNT(im) as movementCount " +
      "FROM InventoryMovement im " +
      "GROUP BY im.productId " +
      "ORDER BY movementCount DESC")
  List<Object[]> findProductsWithMostMovements(Pageable pageable);

  /**
   * Verifica si existe algún movimiento para un producto
   */
  boolean existsByProductId(Long productId);

  /**
   * Busca movimientos de entrada de stock
   */
  @Query("SELECT im FROM InventoryMovement im WHERE im.quantityChange > 0 " +
      "ORDER BY im.createdAt DESC")
  Page<InventoryMovement> findStockInMovements(Pageable pageable);

  /**
   * Busca movimientos de salida de stock
   */
  @Query("SELECT im FROM InventoryMovement im WHERE im.quantityChange < 0 " +
      "ORDER BY im.createdAt DESC")
  Page<InventoryMovement> findStockOutMovements(Pageable pageable);

  /**
   * Obtiene el balance de movimientos para un producto
   */
  @Query("SELECT COALESCE(SUM(im.quantityChange), 0) FROM InventoryMovement im " +
      "WHERE im.productId = :productId")
  Integer getMovementBalanceByProductId(@Param("productId") Long productId);

  /**
   * Busca movimientos anómalos (cambios grandes de cantidad)
   */
  @Query("SELECT im FROM InventoryMovement im WHERE ABS(im.quantityChange) > :threshold " +
      "ORDER BY im.createdAt DESC")
  List<InventoryMovement> findAnomalousMovements(@Param("threshold") Integer threshold);
}